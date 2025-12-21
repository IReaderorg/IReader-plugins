package io.github.ireaderorg.plugins.chapternotes

import ireader.plugin.api.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Chapter Notes Plugin for IReader
 * Take notes and highlights while reading.
 */
class ChapterNotesPlugin : FeaturePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.chapter-notes",
        name = "Chapter Notes",
        version = "1.0.0",
        versionCode = 1,
        description = "Take notes and highlights while reading",
        author = PluginAuthor("IReader Team"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    private var context: PluginContext? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val notes = mutableMapOf<String, MutableList<ChapterNote>>()
    private val tags = mutableSetOf<String>()
    
    override fun initialize(context: PluginContext) {
        this.context = context
        loadState()
    }
    
    override fun cleanup() {
        saveState()
        context = null
    }
    
    override fun getMenuItems(): List<PluginMenuItem> = listOf(
        PluginMenuItem(id = "add_note", label = "Add Note", icon = "note", order = 0),
        PluginMenuItem(id = "view_notes", label = "View Notes", icon = "list", order = 1),
        PluginMenuItem(id = "highlight", label = "Highlight", icon = "highlight", order = 2),
        PluginMenuItem(id = "export", label = "Export Notes", icon = "export", order = 3)
    )
    
    override fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(route = "notes/chapter/{chapterId}", title = "Chapter Notes", content = {}),
        PluginScreen(route = "notes/book/{bookId}", title = "All Notes", content = {}),
        PluginScreen(route = "notes/search", title = "Search Notes", content = {})
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        context.selectedText?.let { text ->
            if (text.isNotBlank() && text.length > 10) {
                return PluginAction.Navigate("notes/highlight?text=$text")
            }
        }
        return null
    }
    
    fun addNote(bookId: Long, chapterId: Long, content: String, tag: String = "general"): ChapterNote {
        val key = "$bookId:$chapterId"
        val note = ChapterNote(generateId(), content, null, NoteType.NOTE, tag, System.currentTimeMillis())
        notes.getOrPut(key) { mutableListOf() }.add(note)
        tags.add(tag)
        saveState()
        return note
    }
    
    fun addHighlight(bookId: Long, chapterId: Long, text: String, comment: String? = null): ChapterNote {
        val key = "$bookId:$chapterId"
        val highlight = ChapterNote(generateId(), text, comment, NoteType.HIGHLIGHT, "highlight", System.currentTimeMillis())
        notes.getOrPut(key) { mutableListOf() }.add(highlight)
        saveState()
        return highlight
    }
    
    fun getChapterNotes(bookId: Long, chapterId: Long) = notes["$bookId:$chapterId"]?.toList() ?: emptyList()
    fun getBookNotes(bookId: Long) = notes.filterKeys { it.startsWith("$bookId:") }.flatMap { it.value }.sortedByDescending { it.createdAt }
    fun searchNotes(bookId: Long, query: String) = getBookNotes(bookId).filter { it.content.contains(query, true) || it.tag.contains(query, true) }
    fun deleteNote(bookId: Long, chapterId: Long, noteId: String) { notes["$bookId:$chapterId"]?.removeAll { it.id == noteId }; saveState() }
    fun getTags(): Set<String> = tags.toSet()
    
    fun exportNotes(bookId: Long): String {
        val bookNotes = getBookNotes(bookId)
        return buildString {
            appendLine("# Reading Notes\nTotal: ${bookNotes.size}\n")
            bookNotes.groupBy { it.type }.forEach { (type, typeNotes) ->
                appendLine("## ${type.name}")
                typeNotes.forEach { note -> appendLine("- ${note.content}"); note.comment?.let { appendLine("  *$it*") } }
                appendLine()
            }
        }
    }
    
    private fun generateId() = "${System.currentTimeMillis()}${(0..999).random()}"
    
    private fun loadState() {
        context?.let { ctx ->
            try {
                val notesStr = ctx.preferences.getString("notes", "")
                if (notesStr.isNotEmpty()) {
                    val decoded = json.decodeFromString<Map<String, List<ChapterNote>>>(notesStr)
                    notes.clear()
                    decoded.forEach { (k, v) -> notes[k] = v.toMutableList() }
                }
                val tagsStr = ctx.preferences.getString("tags", "")
                if (tagsStr.isNotEmpty()) { tags.clear(); tags.addAll(json.decodeFromString<Set<String>>(tagsStr)) }
            } catch (e: Exception) { }
        }
    }
    
    private fun saveState() {
        context?.let { ctx ->
            try {
                ctx.preferences.putString("notes", json.encodeToString(notes.mapValues { it.value.toList() }))
                ctx.preferences.putString("tags", json.encodeToString(tags.toSet()))
            } catch (e: Exception) { }
        }
    }
}

@Serializable data class ChapterNote(val id: String, val content: String, val comment: String? = null, val type: NoteType, val tag: String, val createdAt: Long)
@Serializable enum class NoteType { NOTE, HIGHLIGHT, BOOKMARK }
