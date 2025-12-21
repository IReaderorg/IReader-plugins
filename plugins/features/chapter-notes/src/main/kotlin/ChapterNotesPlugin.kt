package io.github.ireaderorg.plugins.chapternotes

import ireader.plugin.api.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Chapter Notes Plugin for IReader
 * Allows users to take notes while reading chapters.
 */
class ChapterNotesPlugin : FeaturePlugin {
    
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
    
    // Data storage
    private val chapterNotes = mutableMapOf<String, ChapterNote>()
    private val characterNotes = mutableListOf<CharacterNote>()
    private val plotPoints = mutableListOf<PlotPoint>()
    
    override fun initialize(context: PluginContext) {
        this.context = context
        loadData()
    }
    
    override fun cleanup() {
        saveData()
        chapterNotes.clear()
        characterNotes.clear()
        plotPoints.clear()
        context = null
    }

    override fun getMenuItems(): List<PluginMenuItem> = listOf(
        PluginMenuItem(id = "add_note", label = "Add Chapter Note", icon = "note_add", order = 0),
        PluginMenuItem(id = "add_character", label = "Add Character Note", icon = "person_add", order = 1),
        PluginMenuItem(id = "add_plot_point", label = "Add Plot Point", icon = "timeline", order = 2),
        PluginMenuItem(id = "view_notes", label = "View All Notes", icon = "notes", order = 3)
    )
    
    override fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(route = "notes/chapter/{bookId}/{chapterId}", title = "Chapter Notes", content = {}),
        PluginScreen(route = "notes/book/{bookId}", title = "Book Notes", content = {}),
        PluginScreen(route = "notes/characters/{bookId}", title = "Characters", content = {}),
        PluginScreen(route = "notes/plot/{bookId}", title = "Plot Timeline", content = {}),
        PluginScreen(route = "notes/summary/{bookId}", title = "Book Summary", content = {})
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        // Show quick note option when reading
        return PluginAction.ShowMenu(listOf("add_note", "add_character", "add_plot_point"))
    }
    
    // Chapter Note operations
    
    fun getOrCreateChapterNote(bookId: Long, chapterId: Long): ChapterNote {
        val key = "$bookId-$chapterId"
        return chapterNotes.getOrPut(key) {
            ChapterNote(
                bookId = bookId,
                chapterId = chapterId,
                summary = "",
                notes = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }
    
    fun updateChapterNote(
        bookId: Long,
        chapterId: Long,
        summary: String? = null,
        notes: String? = null
    ): ChapterNote {
        val key = "$bookId-$chapterId"
        val existing = chapterNotes[key] ?: ChapterNote(
            bookId = bookId,
            chapterId = chapterId,
            summary = "",
            notes = "",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        val updated = existing.copy(
            summary = summary ?: existing.summary,
            notes = notes ?: existing.notes,
            updatedAt = System.currentTimeMillis()
        )
        chapterNotes[key] = updated
        saveData()
        return updated
    }
    
    fun getChapterNote(bookId: Long, chapterId: Long): ChapterNote? {
        return chapterNotes["$bookId-$chapterId"]
    }
    
    fun getBookChapterNotes(bookId: Long): List<ChapterNote> {
        return chapterNotes.values
            .filter { it.bookId == bookId }
            .sortedBy { it.chapterId }
    }
    
    fun deleteChapterNote(bookId: Long, chapterId: Long): Boolean {
        val removed = chapterNotes.remove("$bookId-$chapterId") != null
        if (removed) saveData()
        return removed
    }
    
    // Character Note operations
    
    fun addCharacterNote(
        bookId: Long,
        name: String,
        description: String,
        firstAppearanceChapter: Long? = null,
        traits: List<String> = emptyList(),
        relationships: List<CharacterRelationship> = emptyList()
    ): CharacterNote {
        val character = CharacterNote(
            id = generateId(),
            bookId = bookId,
            name = name,
            description = description,
            firstAppearanceChapter = firstAppearanceChapter,
            traits = traits,
            relationships = relationships,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        characterNotes.add(character)
        saveData()
        return character
    }
    
    fun updateCharacterNote(
        characterId: String,
        name: String? = null,
        description: String? = null,
        traits: List<String>? = null,
        relationships: List<CharacterRelationship>? = null
    ): Boolean {
        val index = characterNotes.indexOfFirst { it.id == characterId }
        if (index == -1) return false
        
        val existing = characterNotes[index]
        characterNotes[index] = existing.copy(
            name = name ?: existing.name,
            description = description ?: existing.description,
            traits = traits ?: existing.traits,
            relationships = relationships ?: existing.relationships,
            updatedAt = System.currentTimeMillis()
        )
        saveData()
        return true
    }
    
    fun getCharacter(characterId: String): CharacterNote? {
        return characterNotes.find { it.id == characterId }
    }
    
    fun getBookCharacters(bookId: Long): List<CharacterNote> {
        return characterNotes
            .filter { it.bookId == bookId }
            .sortedBy { it.name }
    }
    
    fun searchCharacters(bookId: Long, query: String): List<CharacterNote> {
        val lowerQuery = query.lowercase()
        return characterNotes
            .filter { it.bookId == bookId }
            .filter { 
                it.name.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery) ||
                it.traits.any { trait -> trait.lowercase().contains(lowerQuery) }
            }
    }
    
    fun deleteCharacter(characterId: String): Boolean {
        val removed = characterNotes.removeAll { it.id == characterId }
        if (removed) saveData()
        return removed
    }
    
    // Plot Point operations
    
    fun addPlotPoint(
        bookId: Long,
        chapterId: Long,
        title: String,
        description: String,
        type: PlotPointType,
        importance: PlotImportance = PlotImportance.NORMAL
    ): PlotPoint {
        val plotPoint = PlotPoint(
            id = generateId(),
            bookId = bookId,
            chapterId = chapterId,
            title = title,
            description = description,
            type = type,
            importance = importance,
            createdAt = System.currentTimeMillis()
        )
        plotPoints.add(plotPoint)
        saveData()
        return plotPoint
    }
    
    fun updatePlotPoint(
        plotPointId: String,
        title: String? = null,
        description: String? = null,
        type: PlotPointType? = null,
        importance: PlotImportance? = null
    ): Boolean {
        val index = plotPoints.indexOfFirst { it.id == plotPointId }
        if (index == -1) return false
        
        val existing = plotPoints[index]
        plotPoints[index] = existing.copy(
            title = title ?: existing.title,
            description = description ?: existing.description,
            type = type ?: existing.type,
            importance = importance ?: existing.importance
        )
        saveData()
        return true
    }
    
    fun getPlotPoint(plotPointId: String): PlotPoint? {
        return plotPoints.find { it.id == plotPointId }
    }
    
    fun getBookPlotPoints(bookId: Long): List<PlotPoint> {
        return plotPoints
            .filter { it.bookId == bookId }
            .sortedBy { it.chapterId }
    }
    
    fun getPlotPointsByType(bookId: Long, type: PlotPointType): List<PlotPoint> {
        return plotPoints
            .filter { it.bookId == bookId && it.type == type }
            .sortedBy { it.chapterId }
    }
    
    fun getImportantPlotPoints(bookId: Long): List<PlotPoint> {
        return plotPoints
            .filter { it.bookId == bookId && it.importance == PlotImportance.CRITICAL }
            .sortedBy { it.chapterId }
    }
    
    fun deletePlotPoint(plotPointId: String): Boolean {
        val removed = plotPoints.removeAll { it.id == plotPointId }
        if (removed) saveData()
        return removed
    }
    
    // Book Summary generation
    
    fun generateBookSummary(bookId: Long): BookSummary {
        val notes = getBookChapterNotes(bookId)
        val characters = getBookCharacters(bookId)
        val plots = getBookPlotPoints(bookId)
        
        val chapterSummaries = notes
            .filter { it.summary.isNotBlank() }
            .map { "Chapter ${it.chapterId}: ${it.summary}" }
        
        val mainCharacters = characters.take(5)
        val criticalPlots = plots.filter { it.importance == PlotImportance.CRITICAL }
        
        return BookSummary(
            bookId = bookId,
            totalChaptersWithNotes = notes.size,
            totalCharacters = characters.size,
            totalPlotPoints = plots.size,
            chapterSummaries = chapterSummaries,
            mainCharacters = mainCharacters.map { it.name },
            criticalPlotPoints = criticalPlots.map { it.title }
        )
    }
    
    // Export/Import
    
    fun exportBookNotes(bookId: Long): String {
        val notes = getBookChapterNotes(bookId)
        val characters = getBookCharacters(bookId)
        val plots = getBookPlotPoints(bookId)
        
        val export = NotesExport(
            version = 1,
            bookId = bookId,
            exportedAt = System.currentTimeMillis(),
            chapterNotes = notes,
            characterNotes = characters,
            plotPoints = plots
        )
        return json.encodeToString(export)
    }
    
    fun importBookNotes(jsonData: String): ImportResult {
        return try {
            val import = json.decodeFromString<NotesExport>(jsonData)
            var imported = 0
            
            import.chapterNotes.forEach { note ->
                val key = "${note.bookId}-${note.chapterId}"
                if (!chapterNotes.containsKey(key)) {
                    chapterNotes[key] = note
                    imported++
                }
            }
            
            import.characterNotes.forEach { character ->
                if (characterNotes.none { it.bookId == character.bookId && it.name == character.name }) {
                    characterNotes.add(character.copy(id = generateId()))
                    imported++
                }
            }
            
            import.plotPoints.forEach { plot ->
                if (plotPoints.none { it.bookId == plot.bookId && it.title == plot.title }) {
                    plotPoints.add(plot.copy(id = generateId()))
                    imported++
                }
            }
            
            saveData()
            ImportResult(success = true, imported = imported)
        } catch (e: Exception) {
            ImportResult(success = false, error = e.message)
        }
    }
    
    // Statistics
    
    fun getStatistics(bookId: Long): NotesStatistics {
        val notes = getBookChapterNotes(bookId)
        val characters = getBookCharacters(bookId)
        val plots = getBookPlotPoints(bookId)
        
        return NotesStatistics(
            totalChapterNotes = notes.size,
            chaptersWithSummary = notes.count { it.summary.isNotBlank() },
            totalCharacters = characters.size,
            totalPlotPoints = plots.size,
            plotPointsByType = PlotPointType.values().associateWith { type ->
                plots.count { it.type == type }
            },
            totalWordCount = notes.sumOf { it.notes.split(" ").size + it.summary.split(" ").size }
        )
    }
    
    // Private helpers
    
    private fun generateId(): String {
        return "${System.currentTimeMillis()}-${(1000..9999).random()}"
    }
    
    private fun loadData() {
        context?.let { ctx ->
            try {
                val data = ctx.preferences.getString("chapter_notes_data", null)
                if (data != null) {
                    val export = json.decodeFromString<AllNotesExport>(data)
                    chapterNotes.clear()
                    chapterNotes.putAll(export.chapterNotes.associateBy { "${it.bookId}-${it.chapterId}" })
                    characterNotes.clear()
                    characterNotes.addAll(export.characterNotes)
                    plotPoints.clear()
                    plotPoints.addAll(export.plotPoints)
                }
            } catch (_: Exception) {
                // Ignore load errors
            }
        }
    }
    
    private fun saveData() {
        context?.let { ctx ->
            try {
                val export = AllNotesExport(
                    version = 1,
                    chapterNotes = chapterNotes.values.toList(),
                    characterNotes = characterNotes.toList(),
                    plotPoints = plotPoints.toList()
                )
                ctx.preferences.putString("chapter_notes_data", json.encodeToString(export))
            } catch (_: Exception) {
                // Ignore save errors
            }
        }
    }
}

// Data classes

@Serializable
data class ChapterNote(
    val bookId: Long,
    val chapterId: Long,
    val summary: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CharacterNote(
    val id: String,
    val bookId: Long,
    val name: String,
    val description: String,
    val firstAppearanceChapter: Long? = null,
    val traits: List<String> = emptyList(),
    val relationships: List<CharacterRelationship> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
)

@Serializable
data class CharacterRelationship(
    val characterName: String,
    val relationshipType: String,
    val description: String? = null
)

@Serializable
data class PlotPoint(
    val id: String,
    val bookId: Long,
    val chapterId: Long,
    val title: String,
    val description: String,
    val type: PlotPointType,
    val importance: PlotImportance,
    val createdAt: Long
)

enum class PlotPointType {
    INTRODUCTION,
    RISING_ACTION,
    CLIMAX,
    FALLING_ACTION,
    RESOLUTION,
    TWIST,
    FORESHADOWING,
    FLASHBACK,
    OTHER
}

enum class PlotImportance {
    MINOR,
    NORMAL,
    MAJOR,
    CRITICAL
}

@Serializable
data class NotesExport(
    val version: Int,
    val bookId: Long,
    val exportedAt: Long,
    val chapterNotes: List<ChapterNote>,
    val characterNotes: List<CharacterNote>,
    val plotPoints: List<PlotPoint>
)

@Serializable
data class AllNotesExport(
    val version: Int,
    val chapterNotes: List<ChapterNote>,
    val characterNotes: List<CharacterNote>,
    val plotPoints: List<PlotPoint>
)

data class BookSummary(
    val bookId: Long,
    val totalChaptersWithNotes: Int,
    val totalCharacters: Int,
    val totalPlotPoints: Int,
    val chapterSummaries: List<String>,
    val mainCharacters: List<String>,
    val criticalPlotPoints: List<String>
)

data class ImportResult(
    val success: Boolean,
    val imported: Int = 0,
    val error: String? = null
)

data class NotesStatistics(
    val totalChapterNotes: Int,
    val chaptersWithSummary: Int,
    val totalCharacters: Int,
    val totalPlotPoints: Int,
    val plotPointsByType: Map<PlotPointType, Int>,
    val totalWordCount: Int
)
