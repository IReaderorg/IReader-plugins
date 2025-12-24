package io.github.ireaderorg.plugins.readingstats

import ireader.plugin.api.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Reading Statistics Plugin for IReader
 * Provides comprehensive reading analytics and goal tracking.
 */
class ReadingStatsPlugin : FeaturePlugin, PluginUIProvider {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.reading-stats",
        name = "Reading Statistics",
        version = "1.0.0",
        versionCode = 1,
        description = "Comprehensive reading statistics and analytics",
        author = PluginAuthor("IReader Team"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    private var context: PluginContext? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    // Data storage
    private val readingSessions = mutableListOf<ReadingSession>()
    private val dailyStats = mutableMapOf<String, DailyStats>()
    private var goals = ReadingGoals()
    private var currentSession: ReadingSession? = null
    
    // UI State
    private var currentTab = 0
    private var pendingDailyGoal = ""
    private var pendingWeeklyGoal = ""
    private var pendingMonthlyGoal = ""
    
    override fun initialize(context: PluginContext) {
        this.context = context
        loadData()
    }
    
    override fun cleanup() {
        endCurrentSession()
        saveData()
        context = null
    }

    override fun getMenuItems(): List<PluginMenuItem> = listOf(
        PluginMenuItem(id = "view_stats", label = "Reading Statistics", icon = "analytics", order = 0, route = "plugin/reading-stats/main"),
        PluginMenuItem(id = "set_goals", label = "Set Reading Goals", icon = "flag", order = 1, route = "plugin/reading-stats/main"),
        PluginMenuItem(id = "view_streak", label = "Reading Streak", icon = "local_fire_department", order = 2, route = "plugin/reading-stats/main")
    )
    
    override fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(route = "plugin/reading-stats/main", title = "Reading Statistics", content = {})
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        // Track reading progress when reader context changes
        trackReading(context)
        return null
    }
    
    // ==================== PluginUIProvider Implementation ====================
    
    override fun getScreen(screenId: String, context: PluginScreenContext): PluginUIScreen? {
        // Initialize pending goals from current goals
        if (pendingDailyGoal.isEmpty()) pendingDailyGoal = goals.dailyMinutes.toString()
        if (pendingWeeklyGoal.isEmpty()) pendingWeeklyGoal = goals.weeklyMinutes.toString()
        if (pendingMonthlyGoal.isEmpty()) pendingMonthlyGoal = goals.monthlyBooks.toString()
        
        return buildMainScreen()
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
                    "daily_goal" -> pendingDailyGoal = event.data["value"] ?: ""
                    "weekly_goal" -> pendingWeeklyGoal = event.data["value"] ?: ""
                    "monthly_goal" -> pendingMonthlyGoal = event.data["value"] ?: ""
                }
            }
            UIEventType.CLICK -> {
                when (event.componentId) {
                    "save_goals" -> {
                        val daily = pendingDailyGoal.toIntOrNull() ?: goals.dailyMinutes
                        val weekly = pendingWeeklyGoal.toIntOrNull() ?: goals.weeklyMinutes
                        val monthly = pendingMonthlyGoal.toIntOrNull() ?: goals.monthlyBooks
                        goals = ReadingGoals(daily, weekly, monthly)
                        saveData()
                    }
                }
            }
            else -> {}
        }
        return buildMainScreen()
    }
    
    private fun buildMainScreen(): PluginUIScreen {
        val tabs = listOf(
            Tab(id = "overview", title = "Overview", icon = "analytics", content = buildOverviewTab()),
            Tab(id = "streak", title = "Streak", icon = "local_fire_department", content = buildStreakTab()),
            Tab(id = "goals", title = "Goals", icon = "flag", content = buildGoalsTab())
        )
        
        return PluginUIScreen(
            id = "main",
            title = "Reading Statistics",
            components = listOf(PluginUIComponent.Tabs(tabs))
        )
    }
    
    private fun buildOverviewTab(): List<PluginUIComponent> {
        val components = mutableListOf<PluginUIComponent>()
        val allTime = getAllTimeStats()
        val today = getTodayStats()
        val week = getWeekStats()
        
        // Today's stats
        components.add(PluginUIComponent.Text("Today", TextStyle.TITLE_SMALL))
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.Row(listOf(
                PluginUIComponent.Column(listOf(
                    PluginUIComponent.Text("${today.totalReadingTimeMs / 60_000} min", TextStyle.TITLE_MEDIUM),
                    PluginUIComponent.Text("Reading Time", TextStyle.BODY_SMALL)
                ), spacing = 4),
                PluginUIComponent.Column(listOf(
                    PluginUIComponent.Text("${today.sessionsCount}", TextStyle.TITLE_MEDIUM),
                    PluginUIComponent.Text("Sessions", TextStyle.BODY_SMALL)
                ), spacing = 4)
            ), spacing = 32)
        )))
        
        components.add(PluginUIComponent.Spacer(16))
        
        // This week
        components.add(PluginUIComponent.Text("This Week", TextStyle.TITLE_SMALL))
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.Row(listOf(
                PluginUIComponent.Column(listOf(
                    PluginUIComponent.Text("${week.totalReadingTimeMs / 60_000} min", TextStyle.TITLE_MEDIUM),
                    PluginUIComponent.Text("Total Time", TextStyle.BODY_SMALL)
                ), spacing = 4),
                PluginUIComponent.Column(listOf(
                    PluginUIComponent.Text("${week.daysActive}/7", TextStyle.TITLE_MEDIUM),
                    PluginUIComponent.Text("Days Active", TextStyle.BODY_SMALL)
                ), spacing = 4)
            ), spacing = 32)
        )))
        
        components.add(PluginUIComponent.Spacer(16))
        
        // All time
        components.add(PluginUIComponent.Text("All Time", TextStyle.TITLE_SMALL))
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.Row(listOf(
                PluginUIComponent.Column(listOf(
                    PluginUIComponent.Text("${allTime.totalReadingTimeMs / 3_600_000}h", TextStyle.TITLE_MEDIUM),
                    PluginUIComponent.Text("Total Hours", TextStyle.BODY_SMALL)
                ), spacing = 4),
                PluginUIComponent.Column(listOf(
                    PluginUIComponent.Text("${allTime.totalBooksRead}", TextStyle.TITLE_MEDIUM),
                    PluginUIComponent.Text("Books", TextStyle.BODY_SMALL)
                ), spacing = 4),
                PluginUIComponent.Column(listOf(
                    PluginUIComponent.Text("${allTime.totalSessions}", TextStyle.TITLE_MEDIUM),
                    PluginUIComponent.Text("Sessions", TextStyle.BODY_SMALL)
                ), spacing = 4)
            ), spacing = 24)
        )))
        
        return components
    }
    
    private fun buildStreakTab(): List<PluginUIComponent> {
        val components = mutableListOf<PluginUIComponent>()
        val streak = getCurrentStreak()
        
        // Current streak
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.Column(listOf(
                PluginUIComponent.Text("🔥 ${streak.currentStreak}", TextStyle.TITLE_LARGE),
                PluginUIComponent.Text("Day Streak", TextStyle.BODY)
            ), spacing = 8)
        )))
        
        components.add(PluginUIComponent.Spacer(16))
        
        // Longest streak
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.Row(listOf(
                PluginUIComponent.Column(listOf(
                    PluginUIComponent.Text("${streak.longestStreak}", TextStyle.TITLE_MEDIUM),
                    PluginUIComponent.Text("Longest Streak", TextStyle.BODY_SMALL)
                ), spacing = 4)
            ))
        )))
        
        components.add(PluginUIComponent.Spacer(16))
        
        // Goal progress
        val progress = getGoalProgress()
        components.add(PluginUIComponent.Text("Today's Progress", TextStyle.TITLE_SMALL))
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.Text(
                "${progress.dailyMinutesRead}/${goals.dailyMinutes} minutes (${progress.dailyProgressPercent.toInt()}%)",
                TextStyle.BODY
            )
        )))
        
        return components
    }
    
    private fun buildGoalsTab(): List<PluginUIComponent> {
        val components = mutableListOf<PluginUIComponent>()
        val progress = getGoalProgress()
        
        components.add(PluginUIComponent.Text("Set Your Reading Goals", TextStyle.TITLE_SMALL))
        components.add(PluginUIComponent.Spacer(16))
        
        // Daily goal
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.Text("Daily Goal (minutes)", TextStyle.LABEL),
            PluginUIComponent.TextField(
                id = "daily_goal",
                label = "Minutes per day",
                value = pendingDailyGoal
            ),
            PluginUIComponent.Text(
                "Progress: ${progress.dailyMinutesRead}/${goals.dailyMinutes} min (${progress.dailyProgressPercent.toInt()}%)",
                TextStyle.BODY_SMALL
            )
        )))
        
        components.add(PluginUIComponent.Spacer(8))
        
        // Weekly goal
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.Text("Weekly Goal (minutes)", TextStyle.LABEL),
            PluginUIComponent.TextField(
                id = "weekly_goal",
                label = "Minutes per week",
                value = pendingWeeklyGoal
            ),
            PluginUIComponent.Text(
                "Progress: ${progress.weeklyMinutesRead}/${goals.weeklyMinutes} min (${progress.weeklyProgressPercent.toInt()}%)",
                TextStyle.BODY_SMALL
            )
        )))
        
        components.add(PluginUIComponent.Spacer(8))
        
        // Monthly goal
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.Text("Monthly Goal (books)", TextStyle.LABEL),
            PluginUIComponent.TextField(
                id = "monthly_goal",
                label = "Books per month",
                value = pendingMonthlyGoal
            ),
            PluginUIComponent.Text(
                "Progress: ${progress.monthlyBooksRead}/${goals.monthlyBooks} books (${progress.monthlyProgressPercent.toInt()}%)",
                TextStyle.BODY_SMALL
            )
        )))
        
        components.add(PluginUIComponent.Spacer(16))
        
        components.add(PluginUIComponent.Button(
            id = "save_goals",
            label = "Save Goals",
            style = ButtonStyle.PRIMARY,
            icon = "save"
        ))
        
        return components
    }
    
    // Session tracking
    
    fun startReadingSession(bookId: Long, chapterId: Long) {
        endCurrentSession()
        currentSession = ReadingSession(
            id = generateId(),
            bookId = bookId,
            chapterId = chapterId,
            startTime = System.currentTimeMillis(),
            pagesRead = 0,
            wordsRead = 0
        )
    }
    
    fun updateCurrentSession(pagesRead: Int, wordsRead: Int) {
        currentSession?.let { session ->
            currentSession = session.copy(
                pagesRead = session.pagesRead + pagesRead,
                wordsRead = session.wordsRead + wordsRead
            )
        }
    }
    
    fun endCurrentSession() {
        currentSession?.let { session ->
            val endTime = System.currentTimeMillis()
            val duration = endTime - session.startTime
            
            // Only save sessions longer than 30 seconds
            if (duration > 30_000) {
                val completedSession = session.copy(
                    endTime = endTime,
                    durationMs = duration
                )
                readingSessions.add(completedSession)
                updateDailyStats(completedSession)
                saveData()
            }
        }
        currentSession = null
    }
    
    private fun trackReading(context: ReaderContext) {
        // Auto-start session if not started
        if (currentSession == null) {
            startReadingSession(context.bookId, context.chapterId)
        } else if (currentSession?.bookId != context.bookId || 
                   currentSession?.chapterId != context.chapterId) {
            // Book or chapter changed, end current and start new
            endCurrentSession()
            startReadingSession(context.bookId, context.chapterId)
        }
    }
    
    // Statistics queries
    
    fun getTodayStats(): DailyStats {
        val today = getDateKey(System.currentTimeMillis())
        return dailyStats[today] ?: DailyStats(date = today)
    }
    
    fun getWeekStats(): WeeklyStats {
        val now = System.currentTimeMillis()
        val weekAgo = now - 7 * DAY_MS
        
        val weekSessions = readingSessions.filter { it.startTime >= weekAgo }
        val totalTime = weekSessions.sumOf { it.durationMs }
        val totalPages = weekSessions.sumOf { it.pagesRead }
        val totalWords = weekSessions.sumOf { it.wordsRead }
        val daysActive = weekSessions.map { getDateKey(it.startTime) }.toSet().size
        
        return WeeklyStats(
            totalReadingTimeMs = totalTime,
            totalPagesRead = totalPages,
            totalWordsRead = totalWords,
            daysActive = daysActive,
            averageTimePerDayMs = if (daysActive > 0) totalTime / daysActive else 0,
            sessionsCount = weekSessions.size
        )
    }
    
    fun getMonthStats(): MonthlyStats {
        val now = System.currentTimeMillis()
        val monthAgo = now - 30 * DAY_MS
        
        val monthSessions = readingSessions.filter { it.startTime >= monthAgo }
        val totalTime = monthSessions.sumOf { it.durationMs }
        val totalPages = monthSessions.sumOf { it.pagesRead }
        val totalWords = monthSessions.sumOf { it.wordsRead }
        val daysActive = monthSessions.map { getDateKey(it.startTime) }.toSet().size
        val booksRead = monthSessions.map { it.bookId }.toSet().size
        
        return MonthlyStats(
            totalReadingTimeMs = totalTime,
            totalPagesRead = totalPages,
            totalWordsRead = totalWords,
            daysActive = daysActive,
            booksRead = booksRead,
            averageTimePerDayMs = if (daysActive > 0) totalTime / daysActive else 0,
            sessionsCount = monthSessions.size
        )
    }
    
    fun getBookStats(bookId: Long): BookStats {
        val bookSessions = readingSessions.filter { it.bookId == bookId }
        val totalTime = bookSessions.sumOf { it.durationMs }
        val totalPages = bookSessions.sumOf { it.pagesRead }
        val totalWords = bookSessions.sumOf { it.wordsRead }
        val firstRead = bookSessions.minOfOrNull { it.startTime }
        val lastRead = bookSessions.maxOfOrNull { it.endTime ?: it.startTime }
        
        return BookStats(
            bookId = bookId,
            totalReadingTimeMs = totalTime,
            totalPagesRead = totalPages,
            totalWordsRead = totalWords,
            sessionsCount = bookSessions.size,
            firstReadAt = firstRead,
            lastReadAt = lastRead,
            averageSessionLengthMs = if (bookSessions.isNotEmpty()) totalTime / bookSessions.size else 0
        )
    }
    
    fun getAllTimeStats(): AllTimeStats {
        val totalTime = readingSessions.sumOf { it.durationMs }
        val totalPages = readingSessions.sumOf { it.pagesRead }
        val totalWords = readingSessions.sumOf { it.wordsRead }
        val booksRead = readingSessions.map { it.bookId }.toSet().size
        val daysActive = readingSessions.map { getDateKey(it.startTime) }.toSet().size
        
        return AllTimeStats(
            totalReadingTimeMs = totalTime,
            totalPagesRead = totalPages,
            totalWordsRead = totalWords,
            totalBooksRead = booksRead,
            totalDaysActive = daysActive,
            totalSessions = readingSessions.size,
            averageSessionLengthMs = if (readingSessions.isNotEmpty()) totalTime / readingSessions.size else 0
        )
    }
    
    // Reading streak
    
    fun getCurrentStreak(): StreakInfo {
        val today = getDateKey(System.currentTimeMillis())
        val sortedDates = dailyStats.keys.sorted().reversed()
        
        if (sortedDates.isEmpty()) {
            return StreakInfo(currentStreak = 0, longestStreak = 0, lastReadDate = null)
        }
        
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0
        var previousDate: String? = null
        
        for (date in sortedDates) {
            if (previousDate == null) {
                // First date
                if (date == today || date == getDateKey(System.currentTimeMillis() - DAY_MS)) {
                    currentStreak = 1
                }
                tempStreak = 1
            } else {
                val daysBetween = daysBetween(date, previousDate)
                if (daysBetween == 1L) {
                    tempStreak++
                    if (currentStreak > 0) currentStreak++
                } else {
                    longestStreak = maxOf(longestStreak, tempStreak)
                    tempStreak = 1
                    if (currentStreak > 0) {
                        longestStreak = maxOf(longestStreak, currentStreak)
                        currentStreak = 0
                    }
                }
            }
            previousDate = date
        }
        
        longestStreak = maxOf(longestStreak, tempStreak, currentStreak)
        
        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastReadDate = sortedDates.firstOrNull()
        )
    }
    
    // Goals
    
    fun setDailyGoal(minutes: Int) {
        goals = goals.copy(dailyMinutes = minutes)
        saveData()
    }
    
    fun setWeeklyGoal(minutes: Int) {
        goals = goals.copy(weeklyMinutes = minutes)
        saveData()
    }
    
    fun setMonthlyGoal(books: Int) {
        goals = goals.copy(monthlyBooks = books)
        saveData()
    }
    
    fun getGoals(): ReadingGoals = goals
    
    fun getGoalProgress(): GoalProgress {
        val todayStats = getTodayStats()
        val weekStats = getWeekStats()
        val monthStats = getMonthStats()
        
        val dailyProgress = if (goals.dailyMinutes > 0) {
            (todayStats.totalReadingTimeMs / 60_000.0 / goals.dailyMinutes * 100).coerceAtMost(100.0)
        } else 0.0
        
        val weeklyProgress = if (goals.weeklyMinutes > 0) {
            (weekStats.totalReadingTimeMs / 60_000.0 / goals.weeklyMinutes * 100).coerceAtMost(100.0)
        } else 0.0
        
        val monthlyProgress = if (goals.monthlyBooks > 0) {
            (monthStats.booksRead.toDouble() / goals.monthlyBooks * 100).coerceAtMost(100.0)
        } else 0.0
        
        return GoalProgress(
            dailyProgressPercent = dailyProgress,
            weeklyProgressPercent = weeklyProgress,
            monthlyProgressPercent = monthlyProgress,
            dailyMinutesRead = (todayStats.totalReadingTimeMs / 60_000).toInt(),
            weeklyMinutesRead = (weekStats.totalReadingTimeMs / 60_000).toInt(),
            monthlyBooksRead = monthStats.booksRead
        )
    }
    
    // Private helpers
    
    private fun updateDailyStats(session: ReadingSession) {
        val dateKey = getDateKey(session.startTime)
        val existing = dailyStats[dateKey] ?: DailyStats(date = dateKey)
        
        dailyStats[dateKey] = existing.copy(
            totalReadingTimeMs = existing.totalReadingTimeMs + session.durationMs,
            pagesRead = existing.pagesRead + session.pagesRead,
            wordsRead = existing.wordsRead + session.wordsRead,
            sessionsCount = existing.sessionsCount + 1
        )
    }
    
    private fun generateId(): String {
        return "${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
    
    private fun getDateKey(timestamp: Long): String {
        val days = timestamp / DAY_MS
        return days.toString()
    }
    
    private fun daysBetween(date1: String, date2: String): Long {
        return kotlin.math.abs(date1.toLong() - date2.toLong())
    }
    
    private fun loadData() {
        context?.let { ctx ->
            try {
                val data = ctx.preferences.getString("reading_stats_data", "")
                if (data.isNotEmpty()) {
                    val export = json.decodeFromString<StatsExport>(data)
                    readingSessions.clear()
                    readingSessions.addAll(export.sessions)
                    dailyStats.clear()
                    dailyStats.putAll(export.dailyStats)
                    goals = export.goals
                }
            } catch (_: Exception) {
                // Ignore load errors
            }
        }
    }
    
    private fun saveData() {
        context?.let { ctx ->
            try {
                val export = StatsExport(
                    version = 1,
                    sessions = readingSessions.toList(),
                    dailyStats = dailyStats.toMap(),
                    goals = goals
                )
                ctx.preferences.putString("reading_stats_data", json.encodeToString(export))
            } catch (_: Exception) {
                // Ignore save errors
            }
        }
    }
    
    companion object {
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}

// Data classes

@Serializable
data class ReadingSession(
    val id: String,
    val bookId: Long,
    val chapterId: Long,
    val startTime: Long,
    val endTime: Long? = null,
    val durationMs: Long = 0,
    val pagesRead: Int = 0,
    val wordsRead: Int = 0
)

@Serializable
data class DailyStats(
    val date: String,
    val totalReadingTimeMs: Long = 0,
    val pagesRead: Int = 0,
    val wordsRead: Int = 0,
    val sessionsCount: Int = 0
)

@Serializable
data class ReadingGoals(
    val dailyMinutes: Int = 30,
    val weeklyMinutes: Int = 180,
    val monthlyBooks: Int = 2
)

@Serializable
data class StatsExport(
    val version: Int,
    val sessions: List<ReadingSession>,
    val dailyStats: Map<String, DailyStats>,
    val goals: ReadingGoals
)

data class WeeklyStats(
    val totalReadingTimeMs: Long,
    val totalPagesRead: Int,
    val totalWordsRead: Int,
    val daysActive: Int,
    val averageTimePerDayMs: Long,
    val sessionsCount: Int
)

data class MonthlyStats(
    val totalReadingTimeMs: Long,
    val totalPagesRead: Int,
    val totalWordsRead: Int,
    val daysActive: Int,
    val booksRead: Int,
    val averageTimePerDayMs: Long,
    val sessionsCount: Int
)

data class BookStats(
    val bookId: Long,
    val totalReadingTimeMs: Long,
    val totalPagesRead: Int,
    val totalWordsRead: Int,
    val sessionsCount: Int,
    val firstReadAt: Long?,
    val lastReadAt: Long?,
    val averageSessionLengthMs: Long
)

data class AllTimeStats(
    val totalReadingTimeMs: Long,
    val totalPagesRead: Int,
    val totalWordsRead: Int,
    val totalBooksRead: Int,
    val totalDaysActive: Int,
    val totalSessions: Int,
    val averageSessionLengthMs: Long
)

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastReadDate: String?
)

data class GoalProgress(
    val dailyProgressPercent: Double,
    val weeklyProgressPercent: Double,
    val monthlyProgressPercent: Double,
    val dailyMinutesRead: Int,
    val weeklyMinutesRead: Int,
    val monthlyBooksRead: Int
)
