package io.github.ireaderorg.plugins.chapternotes

import ireader.plugin.api.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Chapter Notes Plugin for IReader
 * Allows users to take notes while reading chapters.
 * Uses declarative UI that the app renders.
 */
class ChapterNotesPlugin : FeaturePlugin, PluginUIProvider {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.chapter-notes",
        name = "Chapter Notes",
        version = "1.0.0",
        versionCode = 1,
        description = "Take notes while reading chapters",
        author = PluginAuthor("IReader Team"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    private var context: PluginContext? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    // In-memory state for current session
    private var currentNotes = mutableListOf<NoteData>()
    private var currentCharacters = mutableListOf<CharacterData>()
    private var currentPlotPoints = mutableListOf<PlotPointData>()
    private var currentTab = 0
    private var pendingNoteContent = ""
    private var pendingCharacterName = ""
    private var pendingCharacterDesc = ""
    private var pendingPlotTitle = ""
    private var pendingPlotDesc = ""
    private var pendingPlotType = "Event"
    
    override fun initialize(context: PluginContext) {
        this.context = context
    }
    
    override fun cleanup() {
        context = null
    }

    override fun getMenuItems(): List<PluginMenuItem> = listOf(
        PluginMenuItem(id = "add_note", label = "Add Note", icon = "note_add", order = 0),
        PluginMenuItem(id = "view_notes", label = "View Notes", icon = "notes", order = 1),
        PluginMenuItem(id = "add_character", label = "Track Character", icon = "person_add", order = 2),
        PluginMenuItem(id = "add_plot_point", label = "Add Plot Point", icon = "timeline", order = 3)
    )
    
    override fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(
            route = "plugin/chapter-notes/main",
            title = "Chapter Notes",
            content = {} // Content provided via PluginUIProvider
        )
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        if (context.selectedText != null) {
            return PluginAction.ShowMenu(listOf("add_note"))
        }
        return null
    }
    
    // ==================== PluginUIProvider Implementation ====================
    
    override fun getScreen(screenId: String, context: PluginScreenContext): PluginUIScreen? {
        val bookId = context.bookId ?: return null
        val chapterId = context.chapterId ?: return null
        
        // Load data
        loadData(bookId, chapterId)
        
        // Pre-fill with selected text if available
        if (context.selectedText != null && pendingNoteContent.isEmpty()) {
            pendingNoteContent = context.selectedText ?: ""
        }
        
        return buildMainScreen(context)
    }
    
    override suspend fun handleEvent(
        screenId: String,
        event: PluginUIEvent,
        context: PluginScreenContext
    ): PluginUIScreen? {
        val bookId = context.bookId ?: return null
        val chapterId = context.chapterId ?: return null
        
        when (event.eventType) {
            UIEventType.TAB_SELECTED -> {
                currentTab = event.data["index"]?.toIntOrNull() ?: 0
            }
            UIEventType.TEXT_CHANGED -> {
                when (event.componentId) {
                    "note_content" -> pendingNoteContent = event.data["value"] ?: ""
                    "character_name" -> pendingCharacterName = event.data["value"] ?: ""
                    "character_desc" -> pendingCharacterDesc = event.data["value"] ?: ""
                    "plot_title" -> pendingPlotTitle = event.data["value"] ?: ""
                    "plot_desc" -> pendingPlotDesc = event.data["value"] ?: ""
                }
            }
            UIEventType.CHIP_SELECTED -> {
                if (event.componentId == "plot_type") {
                    pendingPlotType = event.data["value"] ?: "Event"
                }
            }
            UIEventType.CLICK -> {
                when (event.componentId) {
                    "save_note" -> {
                        if (pendingNoteContent.isNotBlank()) {
                            currentNotes.add(NoteData(
                                id = System.currentTimeMillis().toString(),
                                content = pendingNoteContent,
                                highlight = context.selectedText,
                                createdAt = System.currentTimeMillis()
                            ))
                            saveNotes(bookId, chapterId)
                            pendingNoteContent = ""
                        }
                    }
                    "save_character" -> {
                        if (pendingCharacterName.isNotBlank()) {
                            currentCharacters.add(CharacterData(
                                id = System.currentTimeMillis().toString(),
                                name = pendingCharacterName,
                                description = pendingCharacterDesc,
                                firstChapter = chapterId
                            ))
                            saveCharacters(bookId)
                            pendingCharacterName = ""
                            pendingCharacterDesc = ""
                        }
                    }
                    "save_plot" -> {
                        if (pendingPlotTitle.isNotBlank()) {
                            currentPlotPoints.add(PlotPointData(
                                id = System.currentTimeMillis().toString(),
                                title = pendingPlotTitle,
                                description = pendingPlotDesc,
                                type = pendingPlotType,
                                createdAt = System.currentTimeMillis()
                            ))
                            savePlotPoints(bookId, chapterId)
                            pendingPlotTitle = ""
                            pendingPlotDesc = ""
                        }
                    }
                }
                // Handle delete actions
                if (event.componentId.startsWith("delete_note_")) {
                    val noteId = event.componentId.removePrefix("delete_note_")
                    currentNotes.removeAll { it.id == noteId }
                    saveNotes(bookId, chapterId)
                }
                if (event.componentId.startsWith("delete_char_")) {
                    val charId = event.componentId.removePrefix("delete_char_")
                    currentCharacters.removeAll { it.id == charId }
                    saveCharacters(bookId)
                }
                if (event.componentId.startsWith("delete_plot_")) {
                    val plotId = event.componentId.removePrefix("delete_plot_")
                    currentPlotPoints.removeAll { it.id == plotId }
                    savePlotPoints(bookId, chapterId)
                }
            }
            else -> {}
        }
        
        return buildMainScreen(context)
    }
    
    private fun buildMainScreen(context: PluginScreenContext): PluginUIScreen {
        val tabs = listOf(
            Tab(
                id = "notes",
                title = "Notes",
                icon = "notes",
                content = buildNotesTab()
            ),
            Tab(
                id = "characters",
                title = "Characters",
                icon = "people",
                content = buildCharactersTab()
            ),
            Tab(
                id = "plot",
                title = "Plot",
                icon = "timeline",
                content = buildPlotTab()
            )
        )
        
        return PluginUIScreen(
            id = "main",
            title = context.chapterTitle ?: "Chapter Notes",
            components = listOf(PluginUIComponent.Tabs(tabs))
        )
    }
    
    private fun buildNotesTab(): List<PluginUIComponent> {
        val components = mutableListOf<PluginUIComponent>()
        
        // Add note form
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.TextField(
                id = "note_content",
                label = "Your note",
                value = pendingNoteContent,
                multiline = true,
                maxLines = 4
            ),
            PluginUIComponent.Spacer(8),
            PluginUIComponent.Button(
                id = "save_note",
                label = "Save Note",
                style = ButtonStyle.PRIMARY,
                icon = "save"
            )
        )))
        
        components.add(PluginUIComponent.Spacer(16))
        
        // Notes list
        if (currentNotes.isEmpty()) {
            components.add(PluginUIComponent.Empty(
                icon = "notes",
                message = "No notes yet",
                description = "Add your first note above"
            ))
        } else {
            currentNotes.forEach { note ->
                components.add(PluginUIComponent.Card(listOf(
                    if (note.highlight != null) {
                        PluginUIComponent.Text("\"${note.highlight}\"", TextStyle.BODY_SMALL)
                    } else {
                        PluginUIComponent.Spacer(0)
                    },
                    PluginUIComponent.Text(note.content, TextStyle.BODY),
                    PluginUIComponent.Row(listOf(
                        PluginUIComponent.Button(
                            id = "delete_note_${note.id}",
                            label = "Delete",
                            style = ButtonStyle.TEXT,
                            icon = "delete"
                        )
                    ))
                )))
            }
        }
        
        return components
    }
    
    private fun buildCharactersTab(): List<PluginUIComponent> {
        val components = mutableListOf<PluginUIComponent>()
        
        // Add character form
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.TextField(
                id = "character_name",
                label = "Character name",
                value = pendingCharacterName
            ),
            PluginUIComponent.Spacer(8),
            PluginUIComponent.TextField(
                id = "character_desc",
                label = "Description (optional)",
                value = pendingCharacterDesc,
                multiline = true,
                maxLines = 3
            ),
            PluginUIComponent.Spacer(8),
            PluginUIComponent.Button(
                id = "save_character",
                label = "Track Character",
                style = ButtonStyle.PRIMARY,
                icon = "person_add"
            )
        )))
        
        components.add(PluginUIComponent.Spacer(16))
        
        // Characters list
        if (currentCharacters.isEmpty()) {
            components.add(PluginUIComponent.Empty(
                icon = "people",
                message = "No characters tracked",
                description = "Track characters as you read"
            ))
        } else {
            val items = currentCharacters.map { char ->
                ListItem(
                    id = char.id,
                    title = char.name,
                    subtitle = char.description.ifBlank { null },
                    icon = "person",
                    trailing = "delete_char_${char.id}"
                )
            }
            components.add(PluginUIComponent.ItemList(id = "characters_list", items = items))
        }
        
        return components
    }
    
    private fun buildPlotTab(): List<PluginUIComponent> {
        val components = mutableListOf<PluginUIComponent>()
        
        val plotTypes = listOf("Event", "Revelation", "Conflict", "Resolution", "Twist", "Foreshadowing")
        
        // Add plot point form
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.TextField(
                id = "plot_title",
                label = "Plot point title",
                value = pendingPlotTitle
            ),
            PluginUIComponent.Spacer(8),
            PluginUIComponent.TextField(
                id = "plot_desc",
                label = "Description (optional)",
                value = pendingPlotDesc,
                multiline = true,
                maxLines = 3
            ),
            PluginUIComponent.Spacer(8),
            PluginUIComponent.Text("Type", TextStyle.LABEL),
            PluginUIComponent.ChipGroup(
                id = "plot_type",
                chips = plotTypes.map { type ->
                    PluginUIComponent.Chip(
                        id = type,
                        label = type,
                        selected = pendingPlotType == type
                    )
                },
                singleSelection = true
            ),
            PluginUIComponent.Spacer(8),
            PluginUIComponent.Button(
                id = "save_plot",
                label = "Add Plot Point",
                style = ButtonStyle.PRIMARY,
                icon = "add"
            )
        )))
        
        components.add(PluginUIComponent.Spacer(16))
        
        // Plot points list
        if (currentPlotPoints.isEmpty()) {
            components.add(PluginUIComponent.Empty(
                icon = "timeline",
                message = "No plot points",
                description = "Track important events as you read"
            ))
        } else {
            currentPlotPoints.forEach { point ->
                components.add(PluginUIComponent.Card(listOf(
                    PluginUIComponent.Row(listOf(
                        PluginUIComponent.Chip(id = point.type, label = point.type, selected = true),
                        PluginUIComponent.Button(
                            id = "delete_plot_${point.id}",
                            label = "",
                            style = ButtonStyle.TEXT,
                            icon = "delete"
                        )
                    )),
                    PluginUIComponent.Text(point.title, TextStyle.TITLE_SMALL),
                    if (point.description.isNotBlank()) {
                        PluginUIComponent.Text(point.description, TextStyle.BODY_SMALL)
                    } else {
                        PluginUIComponent.Spacer(0)
                    }
                )))
            }
        }
        
        return components
    }
    
    // ==================== Data Storage ====================
    
    private fun loadData(bookId: Long, chapterId: Long) {
        currentNotes = loadNotes(bookId, chapterId).toMutableList()
        currentCharacters = loadCharacters(bookId).toMutableList()
        currentPlotPoints = loadPlotPoints(bookId, chapterId).toMutableList()
    }
    
    private fun loadNotes(bookId: Long, chapterId: Long): List<NoteData> {
        val key = "notes_${bookId}_$chapterId"
        val data = context?.preferences?.getString(key, "") ?: ""
        return if (data.isNotBlank()) {
            try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }
        } else emptyList()
    }
    
    private fun saveNotes(bookId: Long, chapterId: Long) {
        val key = "notes_${bookId}_$chapterId"
        context?.preferences?.putString(key, json.encodeToString(currentNotes.toList()))
    }
    
    private fun loadCharacters(bookId: Long): List<CharacterData> {
        val key = "characters_$bookId"
        val data = context?.preferences?.getString(key, "") ?: ""
        return if (data.isNotBlank()) {
            try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }
        } else emptyList()
    }
    
    private fun saveCharacters(bookId: Long) {
        val key = "characters_$bookId"
        context?.preferences?.putString(key, json.encodeToString(currentCharacters.toList()))
    }
    
    private fun loadPlotPoints(bookId: Long, chapterId: Long): List<PlotPointData> {
        val key = "plotpoints_${bookId}_$chapterId"
        val data = context?.preferences?.getString(key, "") ?: ""
        return if (data.isNotBlank()) {
            try { json.decodeFromString(data) } catch (_: Exception) { emptyList() }
        } else emptyList()
    }
    
    private fun savePlotPoints(bookId: Long, chapterId: Long) {
        val key = "plotpoints_${bookId}_$chapterId"
        context?.preferences?.putString(key, json.encodeToString(currentPlotPoints.toList()))
    }
}

// Data classes
@Serializable
data class NoteData(
    val id: String,
    val content: String,
    val highlight: String? = null,
    val createdAt: Long
)

@Serializable
data class CharacterData(
    val id: String,
    val name: String,
    val description: String,
    val firstChapter: Long? = null
)

@Serializable
data class PlotPointData(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val createdAt: Long
)
