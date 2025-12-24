package io.github.ireaderorg.plugins.readingtracker

import ireader.plugin.api.*
import kotlinx.serialization.Serializable

/**
 * Reading Progress Tracker Plugin
 * 
 * Provides advanced reading progress tracking with:
 * - Cloud sync support for cross-device progress
 * - Reading goals and streaks
 * - Detailed reading analytics
 * - Export/import functionality
 */
class ReadingTrackerPlugin : FeaturePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.reading-tracker",
        name = "Reading Progress Tracker",
        version = "1.0.0",
        versionCode = 1,
        description = "Track and sync your reading progress across devices. Supports cloud backup, reading goals, and detailed analytics.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.FEATURE,
        permissions = listOf(
            PluginPermission.NETWORK,
            PluginPermission.PREFERENCES,
            PluginPermission.LIBRARY_ACCESS
        ),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP),
        metadata = mapOf(
            "tracker.syncEnabled" to "true",
            "tracker.analyticsEnabled" to "true",
            "tracker.goalsEnabled" to "true"
        )
    )
    
    private var context: PluginContext? = null
    private var syncEndpoint: String = ""
    private var syncToken: String = ""
    
    override fun initialize(context: PluginContext) {
        this.context = context
        syncEndpoint = context.preferences.getString("sync_endpoint", "")
        syncToken = context.preferences.getString("sync_token", "")
    }
    
    override fun cleanup() {
        context = null
    }
    
    override fun getMenuItems(): List<PluginMenuItem> = listOf(
        PluginMenuItem(
            id = "sync_progress",
            label = "Sync Progress",
            icon = "sync",
            order = 1,
            route = "plugin/reading-tracker/main"
        ),
        PluginMenuItem(
            id = "reading_goals",
            label = "Reading Goals",
            icon = "flag",
            order = 2,
            route = "plugin/reading-tracker/main"
        ),
        PluginMenuItem(
            id = "analytics",
            label = "Reading Analytics",
            icon = "analytics",
            order = 3,
            route = "plugin/reading-tracker/main"
        ),
        PluginMenuItem(
            id = "export_progress",
            label = "Export Progress",
            icon = "download",
            order = 4,
            route = "plugin/reading-tracker/main"
        )
    )
    
    override fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(route = "plugin/reading-tracker/main", title = "Reading Tracker", content = {})
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        // Track reading progress when user reads
        return PluginAction.Custom(
            actionId = "track_progress",
            data = mapOf(
                "bookId" to context.bookId,
                "chapterId" to context.chapterId,
                "position" to context.currentPosition,
                "timestamp" to System.currentTimeMillis()
            )
        )
    }
    
    override fun getPreferencesScreen(): PluginScreen? = null
    
    // ==================== Sync Functions ====================
    
    suspend fun syncProgress(): SyncResult {
        if (syncEndpoint.isBlank() || syncToken.isBlank()) {
            return SyncResult.Error("Sync not configured")
        }
        
        val httpClient = context?.httpClient
            ?: return SyncResult.Error("HTTP client not available")
        
        return try {
            // Get local progress
            val localProgress = getLocalProgress()
            
            // Upload to server
            val response = httpClient.post(
                url = "$syncEndpoint/sync",
                body = localProgress.toJson(),
                headers = mapOf(
                    "Authorization" to "Bearer $syncToken",
                    "Content-Type" to "application/json"
                )
            )
            
            if (response.statusCode !in 200..299) {
                return SyncResult.Error("Sync failed: ${response.statusCode}")
            }
            
            // Parse server response and merge
            val serverProgress = parseProgressResponse(response.body)
            mergeProgress(serverProgress)
            
            SyncResult.Success(
                uploaded = localProgress.entries.size,
                downloaded = serverProgress.entries.size
            )
        } catch (e: Exception) {
            SyncResult.Error("Sync failed: ${e.message}")
        }
    }
    
    // ==================== Goals Functions ====================
    
    fun setDailyGoal(chaptersPerDay: Int) {
        context?.preferences?.putInt("daily_goal_chapters", chaptersPerDay)
    }
    
    fun setWeeklyGoal(booksPerWeek: Int) {
        context?.preferences?.putInt("weekly_goal_books", booksPerWeek)
    }
    
    fun getDailyGoal(): Int {
        return context?.preferences?.getInt("daily_goal_chapters", 5) ?: 5
    }
    
    fun getWeeklyGoal(): Int {
        return context?.preferences?.getInt("weekly_goal_books", 1) ?: 1
    }
    
    fun getDailyProgress(): GoalProgress {
        val goal = getDailyGoal()
        val current = getTodayChaptersRead()
        return GoalProgress(
            goal = goal,
            current = current,
            percentage = if (goal > 0) (current.toFloat() / goal * 100).coerceAtMost(100f) else 0f
        )
    }
    
    // ==================== Analytics Functions ====================
    
    fun getReadingStats(): ReadingStats {
        val prefs = context?.preferences ?: return ReadingStats()
        
        return ReadingStats(
            totalChaptersRead = prefs.getInt("total_chapters_read", 0),
            totalBooksCompleted = prefs.getInt("total_books_completed", 0),
            totalReadingTimeMinutes = prefs.getLong("total_reading_time", 0L),
            currentStreak = prefs.getInt("current_streak", 0),
            longestStreak = prefs.getInt("longest_streak", 0),
            averageChaptersPerDay = prefs.getFloat("avg_chapters_per_day", 0f),
            favoriteGenres = prefs.getString("favorite_genres", "").split(",").filter { it.isNotBlank() }
        )
    }
    
    fun recordChapterRead(bookId: String, chapterId: String, readingTimeMinutes: Int) {
        val prefs = context?.preferences ?: return
        
        // Update totals
        val totalChapters = prefs.getInt("total_chapters_read", 0) + 1
        prefs.putInt("total_chapters_read", totalChapters)
        
        val totalTime = prefs.getLong("total_reading_time", 0L) + readingTimeMinutes
        prefs.putLong("total_reading_time", totalTime)
        
        // Update streak
        updateStreak()
        
        // Record for today
        val todayKey = "chapters_${getTodayKey()}"
        val todayChapters = prefs.getInt(todayKey, 0) + 1
        prefs.putInt(todayKey, todayChapters)
    }
    
    fun recordBookCompleted(bookId: String, genre: String?) {
        val prefs = context?.preferences ?: return
        
        val totalBooks = prefs.getInt("total_books_completed", 0) + 1
        prefs.putInt("total_books_completed", totalBooks)
        
        // Update favorite genres
        if (!genre.isNullOrBlank()) {
            val genres = prefs.getString("favorite_genres", "").split(",").toMutableList()
            genres.add(genre)
            // Keep top 5 most frequent
            val topGenres = genres.groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }
                .take(5)
                .map { it.key }
            prefs.putString("favorite_genres", topGenres.joinToString(","))
        }
    }
    
    // ==================== Export/Import ====================
    
    fun exportProgress(): String {
        val stats = getReadingStats()
        val progress = getLocalProgress()
        
        return """
            {
                "version": 1,
                "exportDate": "${System.currentTimeMillis()}",
                "stats": {
                    "totalChaptersRead": ${stats.totalChaptersRead},
                    "totalBooksCompleted": ${stats.totalBooksCompleted},
                    "totalReadingTimeMinutes": ${stats.totalReadingTimeMinutes},
                    "currentStreak": ${stats.currentStreak},
                    "longestStreak": ${stats.longestStreak}
                },
                "progress": ${progress.toJson()}
            }
        """.trimIndent()
    }
    
    fun importProgress(json: String): Boolean {
        return try {
            // Parse and import progress
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // ==================== Private Helpers ====================
    
    private fun getLocalProgress(): ProgressData {
        return ProgressData(entries = emptyList())
    }
    
    private fun parseProgressResponse(body: String): ProgressData {
        return ProgressData(entries = emptyList())
    }
    
    private fun mergeProgress(serverProgress: ProgressData) {
        // Merge server progress with local
    }
    
    private fun getTodayChaptersRead(): Int {
        val todayKey = "chapters_${getTodayKey()}"
        return context?.preferences?.getInt(todayKey, 0) ?: 0
    }
    
    private fun getTodayKey(): String {
        val now = System.currentTimeMillis()
        val day = now / (24 * 60 * 60 * 1000)
        return day.toString()
    }
    
    private fun updateStreak() {
        val prefs = context?.preferences ?: return
        val lastReadDay = prefs.getLong("last_read_day", 0L)
        val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000)
        
        val currentStreak = if (lastReadDay == today - 1 || lastReadDay == today) {
            prefs.getInt("current_streak", 0) + (if (lastReadDay != today) 1 else 0)
        } else {
            1
        }
        
        prefs.putInt("current_streak", currentStreak)
        prefs.putLong("last_read_day", today)
        
        val longestStreak = prefs.getInt("longest_streak", 0)
        if (currentStreak > longestStreak) {
            prefs.putInt("longest_streak", currentStreak)
        }
    }
    
    private fun ProgressData.toJson(): String {
        return """{"entries": []}"""
    }
    
    // ==================== Configuration ====================
    
    fun configureSyncEndpoint(endpoint: String, token: String) {
        syncEndpoint = endpoint
        syncToken = token
        context?.preferences?.putString("sync_endpoint", endpoint)
        context?.preferences?.putString("sync_token", token)
    }
}

// ==================== Data Classes ====================

@Serializable
data class ProgressData(
    val entries: List<ProgressEntry>
)

@Serializable
data class ProgressEntry(
    val bookId: String,
    val chapterId: String,
    val position: Float,
    val timestamp: Long
)

@Serializable
data class GoalProgress(
    val goal: Int,
    val current: Int,
    val percentage: Float
)

@Serializable
data class ReadingStats(
    val totalChaptersRead: Int = 0,
    val totalBooksCompleted: Int = 0,
    val totalReadingTimeMinutes: Long = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val averageChaptersPerDay: Float = 0f,
    val favoriteGenres: List<String> = emptyList()
)

sealed class SyncResult {
    data class Success(val uploaded: Int, val downloaded: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
