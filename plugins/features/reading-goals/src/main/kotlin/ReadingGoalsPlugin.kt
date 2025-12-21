package io.github.ireaderorg.plugins.readinggoals

import ireader.plugin.api.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Reading Goals Plugin for IReader
 * Helps users set and track reading goals with streaks and achievements.
 */
class ReadingGoalsPlugin : FeaturePlugin, PluginUIProvider {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.reading-goals",
        name = "Reading Goals",
        version = "1.0.0",
        versionCode = 1,
        description = "Set and track your reading goals with streaks and achievements",
        author = PluginAuthor("IReader Team"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    private var context: PluginContext? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    private var goals = ReadingGoals()
    private var progress = ReadingProgress()
    private val achievements = mutableListOf<Achievement>()
    
    // UI State
    private var currentTab = 0
    private var pendingDailyGoal = ""
    
    override fun initialize(context: PluginContext) {
        this.context = context
        loadState()
    }
    
    override fun cleanup() {
        saveState()
        context = null
    }
    
    override fun getMenuItems(): List<PluginMenuItem> = listOf(
        PluginMenuItem(id = "view_goals", label = "View Goals", icon = "target", order = 0),
        PluginMenuItem(id = "log_reading", label = "Log Reading", icon = "book", order = 1),
        PluginMenuItem(id = "view_streak", label = "View Streak", icon = "fire", order = 2),
        PluginMenuItem(id = "achievements", label = "Achievements", icon = "trophy", order = 3)
    )
    
    override fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(route = "plugin/reading-goals/main", title = "Reading Goals", content = {})
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? = null
    
    // ==================== PluginUIProvider Implementation ====================
    
    override fun getScreen(screenId: String, context: PluginScreenContext): PluginUIScreen? {
        if (pendingDailyGoal.isEmpty()) pendingDailyGoal = goals.dailyChapters.toString()
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
                if (event.componentId == "daily_goal") {
                    pendingDailyGoal = event.data["value"] ?: ""
                }
            }
            UIEventType.CLICK -> {
                when (event.componentId) {
                    "save_goal" -> {
                        val daily = pendingDailyGoal.toIntOrNull() ?: goals.dailyChapters
                        setDailyGoal(daily)
                    }
                    "log_chapter" -> logReading(1)
                }
            }
            else -> {}
        }
        return buildMainScreen()
    }
    
    private fun buildMainScreen(): PluginUIScreen {
        val tabs = listOf(
            Tab(id = "progress", title = "Progress", icon = "trending_up", content = buildProgressTab()),
            Tab(id = "streak", title = "Streak", icon = "local_fire_department", content = buildStreakTab()),
            Tab(id = "achievements", title = "Achievements", icon = "emoji_events", content = buildAchievementsTab()),
            Tab(id = "settings", title = "Settings", icon = "settings", content = buildSettingsTab())
        )
        
        return PluginUIScreen(
            id = "main",
            title = "Reading Goals",
            components = listOf(PluginUIComponent.Tabs(tabs))
        )
    }
    
    private fun buildProgressTab(): List<PluginUIComponent> {
        val todayProgress = getTodayProgress()
        val progressPercent = if (goals.dailyChapters > 0) {
            (todayProgress.chaptersRead.toFloat() / goals.dailyChapters * 100).coerceAtMost(100f)
        } else 0f
        
        return listOf(
            PluginUIComponent.Card(listOf(
                PluginUIComponent.Text("Today's Progress", TextStyle.TITLE_SMALL),
                PluginUIComponent.Spacer(8),
                PluginUIComponent.Text(
                    "${todayProgress.chaptersRead}/${goals.dailyChapters} chapters",
                    TextStyle.TITLE_LARGE
                ),
                PluginUIComponent.Text("${progressPercent.toInt()}% complete", TextStyle.BODY_SMALL)
            )),
            PluginUIComponent.Spacer(16),
            PluginUIComponent.Button(
                id = "log_chapter",
                label = "Log Chapter Read",
                style = ButtonStyle.PRIMARY,
                icon = "add"
            ),
            PluginUIComponent.Spacer(16),
            if (isDailyGoalMet()) {
                PluginUIComponent.Card(listOf(
                    PluginUIComponent.Text("🎉 Daily goal achieved!", TextStyle.TITLE_MEDIUM)
                ))
            } else {
                PluginUIComponent.Text(
                    "${goals.dailyChapters - todayProgress.chaptersRead} more to reach your goal",
                    TextStyle.BODY
                )
            }
        )
    }
    
    private fun buildStreakTab(): List<PluginUIComponent> {
        return listOf(
            PluginUIComponent.Card(listOf(
                PluginUIComponent.Column(listOf(
                    PluginUIComponent.Text("🔥 ${getCurrentStreak()}", TextStyle.TITLE_LARGE),
                    PluginUIComponent.Text("Day Streak", TextStyle.BODY)
                ), spacing = 8)
            )),
            PluginUIComponent.Spacer(16),
            PluginUIComponent.Card(listOf(
                PluginUIComponent.Row(listOf(
                    PluginUIComponent.Column(listOf(
                        PluginUIComponent.Text("${getBestStreak()}", TextStyle.TITLE_MEDIUM),
                        PluginUIComponent.Text("Best Streak", TextStyle.BODY_SMALL)
                    ), spacing = 4)
                ))
            )),
            PluginUIComponent.Spacer(16),
            PluginUIComponent.Text(
                if (isDailyGoalMet()) "Keep it up! You're on track." else "Complete today's goal to maintain your streak!",
                TextStyle.BODY
            )
        )
    }
    
    private fun buildAchievementsTab(): List<PluginUIComponent> {
        if (achievements.isEmpty()) {
            return listOf(PluginUIComponent.Empty(
                icon = "emoji_events",
                message = "No achievements yet",
                description = "Keep reading to unlock achievements!"
            ))
        }
        
        val items = achievements.map { achievement ->
            ListItem(
                id = achievement.id,
                title = achievement.name,
                subtitle = achievement.description,
                icon = "emoji_events"
            )
        }
        
        return listOf(
            PluginUIComponent.Text("${achievements.size} achievements unlocked", TextStyle.BODY_SMALL),
            PluginUIComponent.Spacer(8),
            PluginUIComponent.ItemList(id = "achievements_list", items = items)
        )
    }
    
    private fun buildSettingsTab(): List<PluginUIComponent> {
        return listOf(
            PluginUIComponent.Text("Goal Settings", TextStyle.TITLE_SMALL),
            PluginUIComponent.Spacer(16),
            PluginUIComponent.Card(listOf(
                PluginUIComponent.Text("Daily Chapter Goal", TextStyle.LABEL),
                PluginUIComponent.TextField(
                    id = "daily_goal",
                    label = "Chapters per day",
                    value = pendingDailyGoal
                ),
                PluginUIComponent.Spacer(8),
                PluginUIComponent.Button(
                    id = "save_goal",
                    label = "Save Goal",
                    style = ButtonStyle.PRIMARY,
                    icon = "save"
                )
            ))
        )
    }
    
    fun getGoals(): ReadingGoals = goals
    
    fun setDailyGoal(chapters: Int) {
        goals = goals.copy(dailyChapters = chapters)
        saveState()
    }
    
    fun logReading(chapters: Int = 1) {
        val today = getCurrentDay()
        val todayProgress = progress.dailyProgress[today] ?: DailyProgress()
        val newProgress = todayProgress.copy(
            chaptersRead = todayProgress.chaptersRead + chapters,
            lastUpdated = System.currentTimeMillis()
        )
        progress.dailyProgress[today] = newProgress
        updateStreak()
        checkAchievements(newProgress)
        saveState()
    }
    
    fun getTodayProgress(): DailyProgress = progress.dailyProgress[getCurrentDay()] ?: DailyProgress()
    fun getCurrentStreak(): Int = progress.currentStreak
    fun getBestStreak(): Int = progress.bestStreak
    fun getAchievements(): List<Achievement> = achievements.toList()
    fun isDailyGoalMet(): Boolean = getTodayProgress().chaptersRead >= goals.dailyChapters
    
    private fun updateStreak() {
        val today = getCurrentDay()
        val todayMet = (progress.dailyProgress[today]?.chaptersRead ?: 0) >= goals.dailyChapters
        val yesterdayMet = (progress.dailyProgress[today - 1]?.chaptersRead ?: 0) >= goals.dailyChapters
        
        if (todayMet && (yesterdayMet || progress.currentStreak == 0)) {
            progress.currentStreak++
            if (progress.currentStreak > progress.bestStreak) progress.bestStreak = progress.currentStreak
        }
    }
    
    private fun checkAchievements(todayProgress: DailyProgress) {
        val total = progress.dailyProgress.values.sumOf { it.chaptersRead }
        if (total >= 1 && !hasAchievement("first")) unlockAchievement("first", "First Steps", "Read first chapter")
        if (todayProgress.chaptersRead >= goals.dailyChapters && !hasAchievement("daily")) unlockAchievement("daily", "Goal Getter", "Complete daily goal")
        if (progress.currentStreak >= 7 && !hasAchievement("week")) unlockAchievement("week", "Week Warrior", "7 day streak")
        if (total >= 100 && !hasAchievement("century")) unlockAchievement("century", "Century Reader", "Read 100 chapters")
    }
    
    private fun hasAchievement(id: String) = achievements.any { it.id == id }
    private fun unlockAchievement(id: String, name: String, desc: String) { achievements.add(Achievement(id, name, desc, System.currentTimeMillis())) }
    private fun getCurrentDay() = (System.currentTimeMillis() / 86400000L).toInt()
    
    private fun loadState() {
        context?.let { ctx ->
            try {
                val goalsStr = ctx.preferences.getString("goals", "")
                if (goalsStr.isNotEmpty()) goals = json.decodeFromString(goalsStr)
                val progressStr = ctx.preferences.getString("progress", "")
                if (progressStr.isNotEmpty()) progress = json.decodeFromString(progressStr)
                val achievementsStr = ctx.preferences.getString("achievements", "")
                if (achievementsStr.isNotEmpty()) { achievements.clear(); achievements.addAll(json.decodeFromString<List<Achievement>>(achievementsStr)) }
            } catch (e: Exception) { }
        }
    }
    
    private fun saveState() {
        context?.let { ctx ->
            try {
                ctx.preferences.putString("goals", json.encodeToString(goals))
                ctx.preferences.putString("progress", json.encodeToString(progress))
                ctx.preferences.putString("achievements", json.encodeToString(achievements.toList()))
            } catch (e: Exception) { }
        }
    }
}

@Serializable data class ReadingGoals(val dailyChapters: Int = 3, val weeklyChapters: Int = 21, val monthlyChapters: Int = 90)
@Serializable data class ReadingProgress(val dailyProgress: MutableMap<Int, DailyProgress> = mutableMapOf(), var currentStreak: Int = 0, var bestStreak: Int = 0)
@Serializable data class DailyProgress(val chaptersRead: Int = 0, val lastUpdated: Long = 0)
@Serializable data class Achievement(val id: String, val name: String, val description: String, val unlockedAt: Long)
