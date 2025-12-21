package io.github.ireaderorg.plugins.readingtimer

import ireader.plugin.api.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Reading Timer Plugin for IReader
 * Track reading time with sessions, daily summaries, and Pomodoro support.
 */
class ReadingTimerPlugin : FeaturePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.reading-timer",
        name = "Reading Timer",
        version = "1.0.0",
        versionCode = 1,
        description = "Track your reading time with Pomodoro support",
        author = PluginAuthor("IReader Team"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    private var context: PluginContext? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    private var settings = TimerSettings()
    private var sessions = mutableListOf<ReadingSession>()
    private var currentSession: ReadingSession? = null
    private var pomodoroState = PomodoroState()
    
    override fun initialize(context: PluginContext) {
        this.context = context
        loadState()
    }
    
    override fun cleanup() {
        endCurrentSession()
        saveState()
        context = null
    }
    
    override fun getMenuItems(): List<PluginMenuItem> = listOf(
        PluginMenuItem(id = "start_timer", label = "Start Timer", icon = "play", order = 0),
        PluginMenuItem(id = "stop_timer", label = "Stop Timer", icon = "stop", order = 1),
        PluginMenuItem(id = "view_stats", label = "View Stats", icon = "chart", order = 2),
        PluginMenuItem(id = "pomodoro", label = "Pomodoro Mode", icon = "timer", order = 3)
    )
    
    override fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(route = "timer/active", title = "Active Timer", content = {}),
        PluginScreen(route = "timer/stats", title = "Reading Stats", content = {}),
        PluginScreen(route = "timer/pomodoro", title = "Pomodoro", content = {})
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        // Auto-start timer when entering reader
        if (settings.autoStartTimer && currentSession == null) {
            startSession(context.bookId, context.chapterId)
        }
        return null
    }
    
    fun startSession(bookId: Long, chapterId: Long): ReadingSession {
        endCurrentSession()
        val session = ReadingSession(
            id = generateId(),
            bookId = bookId,
            chapterId = chapterId,
            startTime = System.currentTimeMillis()
        )
        currentSession = session
        saveState()
        return session
    }
    
    fun endCurrentSession(): ReadingSession? {
        val session = currentSession ?: return null
        val endedSession = session.copy(
            endTime = System.currentTimeMillis(),
            durationMinutes = ((System.currentTimeMillis() - session.startTime) / 60000).toInt()
        )
        sessions.add(endedSession)
        currentSession = null
        saveState()
        return endedSession
    }
    
    fun getCurrentSession(): ReadingSession? = currentSession
    
    fun isTimerRunning(): Boolean = currentSession != null
    
    fun getElapsedMinutes(): Int {
        val session = currentSession ?: return 0
        return ((System.currentTimeMillis() - session.startTime) / 60000).toInt()
    }
    
    fun getTodayReadingTime(): Int {
        val today = getCurrentDay()
        return sessions.filter { it.getDay() == today }.sumOf { it.durationMinutes }
    }
    
    fun getWeekReadingTime(): Int {
        val today = getCurrentDay()
        return sessions.filter { it.getDay() >= today - 6 }.sumOf { it.durationMinutes }
    }
    
    fun getTotalReadingTime(): Int = sessions.sumOf { it.durationMinutes }
    
    fun getSessionCount(): Int = sessions.size
    
    fun getAverageSessionLength(): Int {
        if (sessions.isEmpty()) return 0
        return sessions.sumOf { it.durationMinutes } / sessions.size
    }
    
    // Pomodoro functions
    fun startPomodoro() {
        pomodoroState = pomodoroState.copy(
            isActive = true,
            isBreak = false,
            startTime = System.currentTimeMillis(),
            completedPomodoros = pomodoroState.completedPomodoros
        )
        saveState()
    }
    
    fun stopPomodoro() {
        if (pomodoroState.isActive && !pomodoroState.isBreak) {
            pomodoroState = pomodoroState.copy(
                isActive = false,
                completedPomodoros = pomodoroState.completedPomodoros + 1
            )
        } else {
            pomodoroState = pomodoroState.copy(isActive = false)
        }
        saveState()
    }
    
    fun startBreak() {
        val isLongBreak = pomodoroState.completedPomodoros % 4 == 0
        pomodoroState = pomodoroState.copy(
            isActive = true,
            isBreak = true,
            startTime = System.currentTimeMillis()
        )
        saveState()
    }
    
    fun getPomodoroTimeRemaining(): Int {
        if (!pomodoroState.isActive) return 0
        val duration = if (pomodoroState.isBreak) {
            if (pomodoroState.completedPomodoros % 4 == 0) settings.longBreakMinutes else settings.shortBreakMinutes
        } else {
            settings.pomodoroMinutes
        }
        val elapsed = ((System.currentTimeMillis() - pomodoroState.startTime) / 60000).toInt()
        return (duration - elapsed).coerceAtLeast(0)
    }
    
    fun isPomodoroComplete(): Boolean = getPomodoroTimeRemaining() <= 0
    
    fun getPomodoroState(): PomodoroState = pomodoroState
    
    fun getCompletedPomodoros(): Int = pomodoroState.completedPomodoros
    
    // Settings
    fun setAutoStartTimer(enabled: Boolean) {
        settings = settings.copy(autoStartTimer = enabled)
        saveState()
    }
    
    fun setPomodoroMinutes(minutes: Int) {
        settings = settings.copy(pomodoroMinutes = minutes.coerceIn(5, 60))
        saveState()
    }
    
    fun setShortBreakMinutes(minutes: Int) {
        settings = settings.copy(shortBreakMinutes = minutes.coerceIn(1, 15))
        saveState()
    }
    
    fun setLongBreakMinutes(minutes: Int) {
        settings = settings.copy(longBreakMinutes = minutes.coerceIn(5, 30))
        saveState()
    }
    
    fun getSettings(): TimerSettings = settings
    
    private fun generateId() = "${System.currentTimeMillis()}${(0..999).random()}"
    private fun getCurrentDay() = (System.currentTimeMillis() / 86400000L).toInt()
    
    private fun loadState() {
        context?.let { ctx ->
            try {
                val settingsStr = ctx.preferences.getString("settings", "")
                if (settingsStr.isNotEmpty()) settings = json.decodeFromString(settingsStr)
                val sessionsStr = ctx.preferences.getString("sessions", "")
                if (sessionsStr.isNotEmpty()) { sessions.clear(); sessions.addAll(json.decodeFromString<List<ReadingSession>>(sessionsStr)) }
                val pomodoroStr = ctx.preferences.getString("pomodoro", "")
                if (pomodoroStr.isNotEmpty()) pomodoroState = json.decodeFromString(pomodoroStr)
            } catch (e: Exception) { }
        }
    }
    
    private fun saveState() {
        context?.let { ctx ->
            try {
                ctx.preferences.putString("settings", json.encodeToString(settings))
                ctx.preferences.putString("sessions", json.encodeToString(sessions.takeLast(1000)))
                ctx.preferences.putString("pomodoro", json.encodeToString(pomodoroState))
            } catch (e: Exception) { }
        }
    }
}

@Serializable
data class TimerSettings(
    val autoStartTimer: Boolean = false,
    val pomodoroMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val dailyGoalMinutes: Int = 60
)

@Serializable
data class ReadingSession(
    val id: String,
    val bookId: Long,
    val chapterId: Long,
    val startTime: Long,
    val endTime: Long = 0,
    val durationMinutes: Int = 0
) {
    fun getDay(): Int = (startTime / 86400000L).toInt()
}

@Serializable
data class PomodoroState(
    val isActive: Boolean = false,
    val isBreak: Boolean = false,
    val startTime: Long = 0,
    val completedPomodoros: Int = 0
)
