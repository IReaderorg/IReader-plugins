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
class ReadingGoalsPlugin : FeaturePlugin {
    
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
        PluginScreen(route = "goals/overview", title = "Reading Goals", content = {}),
        PluginScreen(route = "goals/settings", title = "Goal Settings", content = {}),
        PluginScreen(route = "goals/achievements", title = "Achievements", content = {})
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? = null
    
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
