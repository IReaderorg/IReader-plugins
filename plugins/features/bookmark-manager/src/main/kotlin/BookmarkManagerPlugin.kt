package io.github.ireaderorg.plugins.bookmarkmanager

import ireader.plugin.api.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Bookmark Manager Plugin for IReader
 * Provides advanced bookmark management with tags, notes, and smart organization.
 */
class BookmarkManagerPlugin : FeaturePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.bookmark-manager",
        name = "Bookmark Manager",
        version = "1.0.0",
        versionCode = 1,
        description = "Advanced bookmark management with tags, notes, and smart organization",
        author = PluginAuthor("IReader Team"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    private var context: PluginContext? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val bookmarks = mutableListOf<Bookmark>()
    private val tags = mutableSetOf<String>()
    
    override fun initialize(context: PluginContext) {
        this.context = context
        loadBookmarks()
    }
    
    override fun cleanup() {
        saveBookmarks()
        bookmarks.clear()
        tags.clear()
        context = null
    }

    override fun getMenuItems(): List<PluginMenuItem> = listOf(
        PluginMenuItem(id = "add_bookmark", label = "Add Bookmark", icon = "bookmark_add", order = 0),
        PluginMenuItem(id = "view_bookmarks", label = "View Bookmarks", icon = "bookmarks", order = 1),
        PluginMenuItem(id = "add_note", label = "Add Note", icon = "note_add", order = 2)
    )
    
    override fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(route = "bookmarks/list", title = "My Bookmarks", content = {}),
        PluginScreen(route = "bookmarks/add/{bookId}/{chapterId}/{position}", title = "Add Bookmark", content = {}),
        PluginScreen(route = "bookmarks/edit/{bookmarkId}", title = "Edit Bookmark", content = {}),
        PluginScreen(route = "bookmarks/tags", title = "Manage Tags", content = {})
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        // When user selects text, offer to create a bookmark with note
        context.selectedText?.let { text ->
            if (text.isNotBlank()) {
                return PluginAction.Navigate(
                    "bookmarks/add/${context.bookId}/${context.chapterId}/${context.position}"
                )
            }
        }
        return null
    }
    
    // Bookmark CRUD operations
    
    fun addBookmark(
        bookId: Long,
        chapterId: Long,
        position: Int,
        title: String,
        note: String? = null,
        selectedText: String? = null,
        bookmarkTags: List<String> = emptyList()
    ): Bookmark {
        val bookmark = Bookmark(
            id = generateId(),
            bookId = bookId,
            chapterId = chapterId,
            position = position,
            title = title,
            note = note,
            selectedText = selectedText,
            tags = bookmarkTags,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        bookmarks.add(bookmark)
        bookmarkTags.forEach { tags.add(it) }
        saveBookmarks()
        return bookmark
    }
    
    fun updateBookmark(
        bookmarkId: String,
        title: String? = null,
        note: String? = null,
        bookmarkTags: List<String>? = null
    ): Boolean {
        val index = bookmarks.indexOfFirst { it.id == bookmarkId }
        if (index == -1) return false
        
        val existing = bookmarks[index]
        bookmarks[index] = existing.copy(
            title = title ?: existing.title,
            note = note ?: existing.note,
            tags = bookmarkTags ?: existing.tags,
            updatedAt = System.currentTimeMillis()
        )
        bookmarkTags?.forEach { tags.add(it) }
        saveBookmarks()
        return true
    }
    
    fun deleteBookmark(bookmarkId: String): Boolean {
        val removed = bookmarks.removeAll { it.id == bookmarkId }
        if (removed) saveBookmarks()
        return removed
    }
    
    fun getBookmark(bookmarkId: String): Bookmark? {
        return bookmarks.find { it.id == bookmarkId }
    }
    
    // Query operations
    
    fun getBookmarksForBook(bookId: Long): List<Bookmark> {
        return bookmarks.filter { it.bookId == bookId }.sortedByDescending { it.createdAt }
    }
    
    fun getBookmarksForChapter(chapterId: Long): List<Bookmark> {
        return bookmarks.filter { it.chapterId == chapterId }.sortedBy { it.position }
    }
    
    fun getBookmarksByTag(tag: String): List<Bookmark> {
        return bookmarks.filter { tag in it.tags }.sortedByDescending { it.createdAt }
    }
    
    fun searchBookmarks(query: String): List<Bookmark> {
        val lowerQuery = query.lowercase()
        return bookmarks.filter { bookmark ->
            bookmark.title.lowercase().contains(lowerQuery) ||
            bookmark.note?.lowercase()?.contains(lowerQuery) == true ||
            bookmark.selectedText?.lowercase()?.contains(lowerQuery) == true ||
            bookmark.tags.any { it.lowercase().contains(lowerQuery) }
        }.sortedByDescending { it.updatedAt }
    }
    
    fun getAllBookmarks(): List<Bookmark> {
        return bookmarks.sortedByDescending { it.createdAt }
    }
    
    fun getRecentBookmarks(limit: Int = 10): List<Bookmark> {
        return bookmarks.sortedByDescending { it.createdAt }.take(limit)
    }
    
    // Tag operations
    
    fun getAllTags(): Set<String> = tags.toSet()
    
    fun addTag(tag: String) {
        tags.add(tag)
        saveBookmarks()
    }
    
    fun removeTag(tag: String) {
        tags.remove(tag)
        // Remove tag from all bookmarks
        bookmarks.forEachIndexed { index, bookmark ->
            if (tag in bookmark.tags) {
                bookmarks[index] = bookmark.copy(tags = bookmark.tags - tag)
            }
        }
        saveBookmarks()
    }
    
    fun renameTag(oldTag: String, newTag: String) {
        if (oldTag !in tags) return
        tags.remove(oldTag)
        tags.add(newTag)
        bookmarks.forEachIndexed { index, bookmark ->
            if (oldTag in bookmark.tags) {
                bookmarks[index] = bookmark.copy(
                    tags = bookmark.tags.map { if (it == oldTag) newTag else it }
                )
            }
        }
        saveBookmarks()
    }
    
    // Export/Import
    
    fun exportBookmarks(): String {
        val exportData = BookmarkExport(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            bookmarks = bookmarks.toList(),
            tags = tags.toList()
        )
        return json.encodeToString(exportData)
    }
    
    fun importBookmarks(jsonData: String, mergeStrategy: MergeStrategy = MergeStrategy.MERGE): ImportResult {
        return try {
            val importData = json.decodeFromString<BookmarkExport>(jsonData)
            var imported = 0
            var skipped = 0
            var updated = 0
            
            when (mergeStrategy) {
                MergeStrategy.REPLACE -> {
                    bookmarks.clear()
                    tags.clear()
                    bookmarks.addAll(importData.bookmarks)
                    tags.addAll(importData.tags)
                    imported = importData.bookmarks.size
                }
                MergeStrategy.MERGE -> {
                    importData.bookmarks.forEach { newBookmark ->
                        val existing = bookmarks.find { 
                            it.bookId == newBookmark.bookId && 
                            it.chapterId == newBookmark.chapterId && 
                            it.position == newBookmark.position 
                        }
                        if (existing == null) {
                            bookmarks.add(newBookmark.copy(id = generateId()))
                            imported++
                        } else if (newBookmark.updatedAt > existing.updatedAt) {
                            val index = bookmarks.indexOf(existing)
                            bookmarks[index] = newBookmark.copy(id = existing.id)
                            updated++
                        } else {
                            skipped++
                        }
                    }
                    tags.addAll(importData.tags)
                }
                MergeStrategy.SKIP_EXISTING -> {
                    importData.bookmarks.forEach { newBookmark ->
                        val exists = bookmarks.any { 
                            it.bookId == newBookmark.bookId && 
                            it.chapterId == newBookmark.chapterId && 
                            it.position == newBookmark.position 
                        }
                        if (!exists) {
                            bookmarks.add(newBookmark.copy(id = generateId()))
                            imported++
                        } else {
                            skipped++
                        }
                    }
                    tags.addAll(importData.tags)
                }
            }
            
            saveBookmarks()
            ImportResult(success = true, imported = imported, skipped = skipped, updated = updated)
        } catch (e: Exception) {
            ImportResult(success = false, error = e.message)
        }
    }
    
    // Statistics
    
    fun getStatistics(): BookmarkStatistics {
        val booksByBook = bookmarks.groupBy { it.bookId }
        val booksByTag = tags.associateWith { tag -> bookmarks.count { tag in it.tags } }
        val withNotes = bookmarks.count { !it.note.isNullOrBlank() }
        
        return BookmarkStatistics(
            totalBookmarks = bookmarks.size,
            totalTags = tags.size,
            bookmarksWithNotes = withNotes,
            bookmarksByBook = booksByBook.mapValues { it.value.size },
            bookmarksByTag = booksByTag
        )
    }
    
    // Private helpers
    
    private fun generateId(): String {
        return "${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
    
    private fun loadBookmarks() {
        context?.let { ctx ->
            try {
                val data = ctx.preferences.getString("bookmarks_data", null)
                if (data != null) {
                    val export = json.decodeFromString<BookmarkExport>(data)
                    bookmarks.clear()
                    bookmarks.addAll(export.bookmarks)
                    tags.clear()
                    tags.addAll(export.tags)
                }
            } catch (_: Exception) {
                // Ignore load errors, start fresh
            }
        }
    }
    
    private fun saveBookmarks() {
        context?.let { ctx ->
            try {
                val export = BookmarkExport(
                    version = 1,
                    exportedAt = System.currentTimeMillis(),
                    bookmarks = bookmarks.toList(),
                    tags = tags.toList()
                )
                ctx.preferences.putString("bookmarks_data", json.encodeToString(export))
            } catch (_: Exception) {
                // Ignore save errors
            }
        }
    }
}

// Data classes

@Serializable
data class Bookmark(
    val id: String,
    val bookId: Long,
    val chapterId: Long,
    val position: Int,
    val title: String,
    val note: String? = null,
    val selectedText: String? = null,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class BookmarkExport(
    val version: Int,
    val exportedAt: Long,
    val bookmarks: List<Bookmark>,
    val tags: List<String>
)

data class ImportResult(
    val success: Boolean,
    val imported: Int = 0,
    val skipped: Int = 0,
    val updated: Int = 0,
    val error: String? = null
)

data class BookmarkStatistics(
    val totalBookmarks: Int,
    val totalTags: Int,
    val bookmarksWithNotes: Int,
    val bookmarksByBook: Map<Long, Int>,
    val bookmarksByTag: Map<String, Int>
)

enum class MergeStrategy {
    REPLACE,      // Replace all existing bookmarks
    MERGE,        // Merge, update if newer
    SKIP_EXISTING // Only add new bookmarks
}
