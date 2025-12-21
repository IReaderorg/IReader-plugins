package io.github.ireaderorg.plugins.quotehighlighter

import ireader.plugin.api.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Quote Highlighter Plugin for IReader
 * Allows users to highlight and save memorable quotes while reading.
 */
class QuoteHighlighterPlugin : FeaturePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.quote-highlighter",
        name = "Quote Highlighter",
        version = "1.0.0",
        versionCode = 1,
        description = "Highlight and save memorable quotes while reading",
        author = PluginAuthor("IReader Team"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    private var context: PluginContext? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    // Data storage
    private val highlights = mutableListOf<Highlight>()
    private val collections = mutableListOf<QuoteCollection>()
    
    override fun initialize(context: PluginContext) {
        this.context = context
        loadData()
    }
    
    override fun cleanup() {
        saveData()
        highlights.clear()
        collections.clear()
        context = null
    }

    override fun getMenuItems(): List<PluginMenuItem> = listOf(
        PluginMenuItem(id = "highlight_yellow", label = "Highlight (Yellow)", icon = "highlight", order = 0),
        PluginMenuItem(id = "highlight_green", label = "Highlight (Green)", icon = "highlight", order = 1),
        PluginMenuItem(id = "highlight_blue", label = "Highlight (Blue)", icon = "highlight", order = 2),
        PluginMenuItem(id = "highlight_pink", label = "Highlight (Pink)", icon = "highlight", order = 3),
        PluginMenuItem(id = "save_quote", label = "Save as Quote", icon = "format_quote", order = 4),
        PluginMenuItem(id = "view_highlights", label = "View All Highlights", icon = "collections_bookmark", order = 5)
    )
    
    override fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(route = "highlights/list", title = "My Highlights", content = {}),
        PluginScreen(route = "highlights/book/{bookId}", title = "Book Highlights", content = {}),
        PluginScreen(route = "highlights/collections", title = "Quote Collections", content = {}),
        PluginScreen(route = "highlights/collection/{collectionId}", title = "Collection", content = {}),
        PluginScreen(route = "highlights/share/{highlightId}", title = "Share Quote", content = {})
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        context.selectedText?.let { text ->
            if (text.isNotBlank() && text.length >= 10) {
                // Show highlight options for selected text
                return PluginAction.ShowMenu(listOf(
                    "highlight_yellow", "highlight_green", "highlight_blue", "highlight_pink", "save_quote"
                ))
            }
        }
        return null
    }
    
    // Highlight operations
    
    fun createHighlight(
        bookId: Long,
        chapterId: Long,
        text: String,
        startPosition: Int,
        endPosition: Int,
        color: HighlightColor,
        note: String? = null
    ): Highlight {
        val highlight = Highlight(
            id = generateId(),
            bookId = bookId,
            chapterId = chapterId,
            text = text,
            startPosition = startPosition,
            endPosition = endPosition,
            color = color,
            note = note,
            createdAt = System.currentTimeMillis()
        )
        highlights.add(highlight)
        saveData()
        return highlight
    }
    
    fun updateHighlight(
        highlightId: String,
        color: HighlightColor? = null,
        note: String? = null
    ): Boolean {
        val index = highlights.indexOfFirst { it.id == highlightId }
        if (index == -1) return false
        
        val existing = highlights[index]
        highlights[index] = existing.copy(
            color = color ?: existing.color,
            note = note ?: existing.note
        )
        saveData()
        return true
    }
    
    fun deleteHighlight(highlightId: String): Boolean {
        val removed = highlights.removeAll { it.id == highlightId }
        if (removed) {
            // Also remove from collections
            collections.forEach { collection ->
                collection.highlightIds.remove(highlightId)
            }
            saveData()
        }
        return removed
    }
    
    fun getHighlight(highlightId: String): Highlight? {
        return highlights.find { it.id == highlightId }
    }
    
    // Query operations
    
    fun getHighlightsForBook(bookId: Long): List<Highlight> {
        return highlights.filter { it.bookId == bookId }.sortedBy { it.startPosition }
    }
    
    fun getHighlightsForChapter(chapterId: Long): List<Highlight> {
        return highlights.filter { it.chapterId == chapterId }.sortedBy { it.startPosition }
    }
    
    fun getHighlightsByColor(color: HighlightColor): List<Highlight> {
        return highlights.filter { it.color == color }.sortedByDescending { it.createdAt }
    }
    
    fun getAllHighlights(): List<Highlight> {
        return highlights.sortedByDescending { it.createdAt }
    }
    
    fun searchHighlights(query: String): List<Highlight> {
        val lowerQuery = query.lowercase()
        return highlights.filter { highlight ->
            highlight.text.lowercase().contains(lowerQuery) ||
            highlight.note?.lowercase()?.contains(lowerQuery) == true
        }.sortedByDescending { it.createdAt }
    }
    
    // Collection operations
    
    fun createCollection(name: String, description: String? = null): QuoteCollection {
        val collection = QuoteCollection(
            id = generateId(),
            name = name,
            description = description,
            highlightIds = mutableListOf(),
            createdAt = System.currentTimeMillis()
        )
        collections.add(collection)
        saveData()
        return collection
    }
    
    fun addToCollection(collectionId: String, highlightId: String): Boolean {
        val collection = collections.find { it.id == collectionId } ?: return false
        if (highlightId !in collection.highlightIds) {
            collection.highlightIds.add(highlightId)
            saveData()
        }
        return true
    }
    
    fun removeFromCollection(collectionId: String, highlightId: String): Boolean {
        val collection = collections.find { it.id == collectionId } ?: return false
        val removed = collection.highlightIds.remove(highlightId)
        if (removed) saveData()
        return removed
    }
    
    fun deleteCollection(collectionId: String): Boolean {
        val removed = collections.removeAll { it.id == collectionId }
        if (removed) saveData()
        return removed
    }
    
    fun getCollection(collectionId: String): QuoteCollection? {
        return collections.find { it.id == collectionId }
    }
    
    fun getCollectionHighlights(collectionId: String): List<Highlight> {
        val collection = collections.find { it.id == collectionId } ?: return emptyList()
        return collection.highlightIds.mapNotNull { id -> highlights.find { it.id == id } }
    }
    
    fun getAllCollections(): List<QuoteCollection> {
        return collections.sortedByDescending { it.createdAt }
    }
    
    // Sharing
    
    fun generateShareText(highlightId: String, includeBookInfo: Boolean = true): String? {
        val highlight = highlights.find { it.id == highlightId } ?: return null
        
        return buildString {
            append("\"${highlight.text}\"")
            if (includeBookInfo) {
                append("\n\n— From my reading on IReader")
            }
            if (!highlight.note.isNullOrBlank()) {
                append("\n\nMy thoughts: ${highlight.note}")
            }
        }
    }
    
    fun generateCollectionShareText(collectionId: String): String? {
        val collection = collections.find { it.id == collectionId } ?: return null
        val collectionHighlights = getCollectionHighlights(collectionId)
        
        if (collectionHighlights.isEmpty()) return null
        
        return buildString {
            append("📚 ${collection.name}\n")
            if (!collection.description.isNullOrBlank()) {
                append("${collection.description}\n")
            }
            append("\n")
            collectionHighlights.take(5).forEachIndexed { index, highlight ->
                append("${index + 1}. \"${highlight.text.take(100)}${if (highlight.text.length > 100) "..." else ""}\"\n\n")
            }
            if (collectionHighlights.size > 5) {
                append("...and ${collectionHighlights.size - 5} more quotes")
            }
            append("\n\n— Collected with IReader")
        }
    }
    
    // Statistics
    
    fun getStatistics(): HighlightStatistics {
        val byColor = HighlightColor.values().associateWith { color ->
            highlights.count { it.color == color }
        }
        val byBook = highlights.groupBy { it.bookId }.mapValues { it.value.size }
        val withNotes = highlights.count { !it.note.isNullOrBlank() }
        
        return HighlightStatistics(
            totalHighlights = highlights.size,
            totalCollections = collections.size,
            highlightsWithNotes = withNotes,
            highlightsByColor = byColor,
            highlightsByBook = byBook
        )
    }
    
    // Export/Import
    
    fun exportData(): String {
        val exportData = HighlightExport(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            highlights = highlights.toList(),
            collections = collections.toList()
        )
        return json.encodeToString(exportData)
    }
    
    fun importData(jsonData: String): ImportResult {
        return try {
            val importData = json.decodeFromString<HighlightExport>(jsonData)
            var imported = 0
            var skipped = 0
            
            importData.highlights.forEach { newHighlight ->
                val exists = highlights.any { 
                    it.bookId == newHighlight.bookId && 
                    it.chapterId == newHighlight.chapterId &&
                    it.startPosition == newHighlight.startPosition &&
                    it.endPosition == newHighlight.endPosition
                }
                if (!exists) {
                    highlights.add(newHighlight.copy(id = generateId()))
                    imported++
                } else {
                    skipped++
                }
            }
            
            importData.collections.forEach { newCollection ->
                if (collections.none { it.name == newCollection.name }) {
                    collections.add(newCollection.copy(id = generateId()))
                }
            }
            
            saveData()
            ImportResult(success = true, imported = imported, skipped = skipped)
        } catch (e: Exception) {
            ImportResult(success = false, error = e.message)
        }
    }
    
    // Private helpers
    
    private fun generateId(): String {
        return "${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
    
    private fun loadData() {
        context?.let { ctx ->
            try {
                val data = ctx.preferences.getString("highlights_data", null)
                if (data != null) {
                    val export = json.decodeFromString<HighlightExport>(data)
                    highlights.clear()
                    highlights.addAll(export.highlights)
                    collections.clear()
                    collections.addAll(export.collections)
                }
            } catch (_: Exception) {
                // Ignore load errors
            }
        }
    }
    
    private fun saveData() {
        context?.let { ctx ->
            try {
                val export = HighlightExport(
                    version = 1,
                    exportedAt = System.currentTimeMillis(),
                    highlights = highlights.toList(),
                    collections = collections.toList()
                )
                ctx.preferences.putString("highlights_data", json.encodeToString(export))
            } catch (_: Exception) {
                // Ignore save errors
            }
        }
    }
}

// Data classes

enum class HighlightColor {
    YELLOW, GREEN, BLUE, PINK, ORANGE, PURPLE
}

@Serializable
data class Highlight(
    val id: String,
    val bookId: Long,
    val chapterId: Long,
    val text: String,
    val startPosition: Int,
    val endPosition: Int,
    val color: HighlightColor,
    val note: String? = null,
    val createdAt: Long
)

@Serializable
data class QuoteCollection(
    val id: String,
    val name: String,
    val description: String? = null,
    val highlightIds: MutableList<String>,
    val createdAt: Long
)

@Serializable
data class HighlightExport(
    val version: Int,
    val exportedAt: Long,
    val highlights: List<Highlight>,
    val collections: List<QuoteCollection>
)

data class ImportResult(
    val success: Boolean,
    val imported: Int = 0,
    val skipped: Int = 0,
    val error: String? = null
)

data class HighlightStatistics(
    val totalHighlights: Int,
    val totalCollections: Int,
    val highlightsWithNotes: Int,
    val highlightsByColor: Map<HighlightColor, Int>,
    val highlightsByBook: Map<Long, Int>
)
