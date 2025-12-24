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
class ReadingTimerPlugin : FeaturePlugin, PluginUIProvider {
    
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
    
    // UI State
    private var currentTab = 0
    private var pendingPomodoroMinutes = ""
    private var pendingBreakMinutes = ""
    
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
        PluginMenuItem(id = "start_timer", label = "Start Timer", icon = "play", order = 0, route = "plugin/reading-timer/main"),
        PluginMenuItem(id = "stop_timer", label = "Stop Timer", icon = "stop", order = 1, route = "plugin/reading-timer/main"),
        PluginMenuItem(id = "view_stats", label = "View Stats", icon = "chart", order = 2, route = "plugin/reading-timer/main"),
        PluginMenuItem(id = "pomodoro", label = "Pomodoro Mode", icon = "timer", order = 3, route = "plugin/reading-timer/main")
    )
    
    override fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(route = "plugin/reading-timer/main", title = "Reading Timer", content = {})
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        // Auto-start timer when entering reader
        if (settings.autoStartTimer && currentSession == null) {
            startSession(context.bookId, context.chapterId)
        }
        return null
    }
    
    // ==================== PluginUIProvider Implementation ====================
    
    override fun getScreen(screenId: String, context: PluginScreenContext): PluginUIScreen? {
        if (pendingPomodoroMinutes.isEmpty()) pendingPomodoroMinutes = settings.pomodoroMinutes.toString()
        if (pendingBreakMinutes.isEmpty()) pendingBreakMinutes = settings.shortBreakMinutes.toString()
        return buildMainScreen(context)
    }
    
    override suspend fun handleEvent(
        screenId: String,
        event: PluginUIEvent,
        context: PluginScreenContext
    ): PluginUIScreen? {
        when (event.eventType) {
            UIEventType.TAB_SELECTED -> {
                currentTab = event.data["index"]?.toIntOrNull() ?: 0
            }
            UIEventType.TEXT_CHANGED -> {
                when (event.componentId) {
                    "pomodoro_minutes" -> pendingPomodoroMinutes = event.data["value"] ?: ""
                    "break_minutes" -> pendingBreakMinutes = event.data["value"] ?: ""
                }
            }
            UIEventType.SWITCH_TOGGLED -> {
                if (event.componentId == "auto_start") {
                    setAutoStartTimer(event.data["checked"] == "true")
                }
            }
            UIEventType.CLICK -> {
                when (event.componentId) {
                    "start_timer" -> {
                        if (context.bookId != null && context.chapterId != null) {
                            startSession(context.bookId!!, context.chapterId!!)
                        }
                    }
                    "stop_timer" -> endCurrentSession()
                    "start_pomodoro" -> startPomodoro()
                    "stop_pomodoro" -> stopPomodoro()
                    "start_break" -> startBreak()
                    "save_settings" -> {
                        val pomMinutes = pendingPomodoroMinutes.toIntOrNull() ?: settings.pomodoroMinutes
                        val breakMinutes = pendingBreakMinutes.toIntOrNull() ?: settings.shortBreakMinutes
                        setPomodoroMinutes(pomMinutes)
                        setShortBreakMinutes(breakMinutes)
                    }
                }
            }
            else -> {}
        }
        return buildMainScreen(context)
    }
    
    private fun buildMainScreen(context: PluginScreenContext): PluginUIScreen {
        val tabs = listOf(
            Tab(id = "timer", title = "Timer", icon = "timer", content = buildTimerTab(context)),
            Tab(id = "pomodoro", title = "Pomodoro", icon = "hourglass_empty", content = buildPomodoroTab()),
            Tab(id = "stats", title = "Stats", icon = "analytics", content = buildStatsTab()),
            Tab(id = "settings", title = "Settings", icon = "settings", content = buildSettingsTab())
        )
        
        return PluginUIScreen(
            id = "main",
            title = "Reading Timer",
            components = listOf(PluginUIComponent.Tabs(tabs))
        )
    }
    
    private fun buildTimerTab(context: PluginScreenContext): List<PluginUIComponent> {
        val components = mutableListOf<PluginUIComponent>()
        
        if (isTimerRunning()) {
            val elapsed = getElapsedMinutes()
            components.add(PluginUIComponent.Card(listOf(
                PluginUIComponent.Text("⏱️ ${elapsed} min", TextStyle.TITLE_LARGE),
                PluginUIComponent.Text("Reading session active", TextStyle.BODY_SMALL)
            )))
            
            components.add(PluginUIComponent.Spacer(16))
            
            components.add(PluginUIComponent.Button(
                id = "stop_timer",
                label = "Stop Timer",
                style = ButtonStyle.OUTLINED,
                icon = "stop"
            ))
        } else {
            components.add(PluginUIComponent.Card(listOf(
                PluginUIComponent.Text("No active session", TextStyle.TITLE_MEDIUM),
                PluginUIComponent.Text("Start a timer to track your reading", TextStyle.BODY_SMALL)
            )))
            
            components.add(PluginUIComponent.Spacer(16))
            
            components.add(PluginUIComponent.Button(
                id = "start_timer",
                label = "Start Timer",
                style = ButtonStyle.PRIMARY,
                icon = "play_arrow"
            ))
        }
        
        components.add(PluginUIComponent.Spacer(24))
        
        // Today's summary
        components.add(PluginUIComponent.Text("Today", TextStyle.TITLE_SMALL))
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.Text("${getTodayReadingTime()} minutes", TextStyle.TITLE_MEDIUM)
        )))
        
        return components
    }
    
    private fun buildPomodoroTab(): List<PluginUIComponent> {
        val components = mutableListOf<PluginUIComponent>()
        
        val state = getPomodoroState()
        
        if (state.isActive) {
            val remaining = getPomodoroTimeRemaining()
            val label = if (state.isBreak) "Break" else "Focus"
            
            components.add(PluginUIComponent.Card(listOf(
                PluginUIComponent.Text("🍅 $label", TextStyle.TITLE_SMALL),
                PluginUIComponent.Text("${remaining} min remaining", TextStyle.TITLE_LARGE)
            )))
            
            components.add(PluginUIComponent.Spacer(16))
            
            if (isPomodoroComplete()) {
                if (state.isBreak) {
                    components.add(PluginUIComponent.Button(
                        id = "start_pomodoro",
                        label = "Start Focus",
                        style = ButtonStyle.PRIMARY,
                        icon = "play_arrow"
                    ))
                } else {
                    components.add(PluginUIComponent.Button(
                        id = "start_break",
                        label = "Take a Break",
                        style = ButtonStyle.PRIMARY,
                        icon = "coffee"
                    ))
                }
            } else {
                components.add(PluginUIComponent.Button(
                    id = "stop_pomodoro",
                    label = "Stop",
                    style = ButtonStyle.OUTLINED,
                    icon = "stop"
                ))
            }
        } else {
            components.add(PluginUIComponent.Card(listOf(
                PluginUIComponent.Text("🍅 Pomodoro Timer", TextStyle.TITLE_MEDIUM),
                PluginUIComponent.Text("Focus for ${settings.pomodoroMinutes} minutes", TextStyle.BODY_SMALL)
            )))
            
            components.add(PluginUIComponent.Spacer(16))
            
            components.add(PluginUIComponent.Button(
                id = "start_pomodoro",
                label = "Start Pomodoro",
                style = ButtonStyle.PRIMARY,
                icon = "play_arrow"
            ))
        }
        
        components.add(PluginUIComponent.Spacer(24))
        
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.Text("Completed today: ${getCompletedPomodoros()} 🍅", TextStyle.BODY)
        )))
        
        return components
    }
    
    private fun buildStatsTab(): List<PluginUIComponent> {
        return listOf(
            PluginUIComponent.Text("Reading Statistics", TextStyle.TITLE_SMALL),
            PluginUIComponent.Spacer(16),
            PluginUIComponent.Card(listOf(
                PluginUIComponent.Row(listOf(
                    PluginUIComponent.Column(listOf(
                        PluginUIComponent.Text("${getTodayReadingTime()}", TextStyle.TITLE_MEDIUM),
                        PluginUIComponent.Text("Today (min)", TextStyle.BODY_SMALL)
                    ), spacing = 4),
                    PluginUIComponent.Column(listOf(
                        PluginUIComponent.Text("${getWeekReadingTime()}", TextStyle.TITLE_MEDIUM),
                        PluginUIComponent.Text("This Week", TextStyle.BODY_SMALL)
                    ), spacing = 4)
                ), spacing = 32)
            )),
            PluginUIComponent.Spacer(16),
            PluginUIComponent.Card(listOf(
                PluginUIComponent.Row(listOf(
                    PluginUIComponent.Column(listOf(
                        PluginUIComponent.Text("${getTotalReadingTime()}", TextStyle.TITLE_MEDIUM),
                        PluginUIComponent.Text("Total (min)", TextStyle.BODY_SMALL)
                    ), spacing = 4),
                    PluginUIComponent.Column(listOf(
                        PluginUIComponent.Text("${getSessionCount()}", TextStyle.TITLE_MEDIUM),
                        PluginUIComponent.Text("Sessions", TextStyle.BODY_SMALL)
                    ), spacing = 4),
                    PluginUIComponent.Column(listOf(
                        PluginUIComponent.Text("${getAverageSessionLength()}", TextStyle.TITLE_MEDIUM),
                        PluginUIComponent.Text("Avg (min)", TextStyle.BODY_SMALL)
                    ), spacing = 4)
                ), spacing = 24)
            ))
        )
    }
    
    private fun buildSettingsTab(): List<PluginUIComponent> {
        return listOf(
            PluginUIComponent.Text("Timer Settings", TextStyle.TITLE_SMALL),
            PluginUIComponent.Spacer(16),
            PluginUIComponent.Card(listOf(
                PluginUIComponent.Switch(
                    id = "auto_start",
                    label = "Auto-start timer when reading",
                    checked = settings.autoStartTimer
                )
            )),
            PluginUIComponent.Spacer(16),
            PluginUIComponent.Card(listOf(
                PluginUIComponent.Text("Pomodoro Duration (minutes)", TextStyle.LABEL),
                PluginUIComponent.TextField(
                    id = "pomodoro_minutes",
                    label = "Focus time",
                    value = pendingPomodoroMinutes
                ),
                PluginUIComponent.Spacer(8),
                PluginUIComponent.Text("Break Duration (minutes)", TextStyle.LABEL),
                PluginUIComponent.TextField(
                    id = "break_minutes",
                    label = "Break time",
                    value = pendingBreakMinutes
                ),
                PluginUIComponent.Spacer(8),
                PluginUIComponent.Button(
                    id = "save_settings",
                    label = "Save Settings",
                    style = ButtonStyle.PRIMARY,
                    icon = "save"
                )
            ))
        )
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
