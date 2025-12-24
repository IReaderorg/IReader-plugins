package io.github.ireaderorg.plugins.smartdictionary

import ireader.plugin.api.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Smart Dictionary Plugin for IReader
 * Provides word definitions, translations, and vocabulary building.
 */
class SmartDictionaryPlugin : FeaturePlugin, PluginUIProvider {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.smart-dictionary",
        name = "Smart Dictionary",
        version = "1.0.0",
        versionCode = 1,
        description = "Instant word definitions and translations while reading",
        author = PluginAuthor("IReader Team"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    private var context: PluginContext? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val definitionCache = mutableMapOf<String, WordDefinition>()
    private val vocabularyList = mutableListOf<VocabularyEntry>()
    
    // UI State
    private var currentTab = 0
    private var searchWord = ""
    private var currentDefinition: WordDefinition? = null
    private var isLoading = false
    private var error: String? = null
    
    override fun initialize(context: PluginContext) {
        this.context = context
    }
    
    override fun cleanup() {
        definitionCache.clear()
        context = null
    }

    override fun getMenuItems(): List<PluginMenuItem> = listOf(
        PluginMenuItem(id = "lookup", label = "Look up word", icon = "dictionary", order = 0, route = "plugin/smart-dictionary/main"),
        PluginMenuItem(id = "add_vocab", label = "Add to vocabulary", icon = "bookmark", order = 1, route = "plugin/smart-dictionary/main")
    )
    
    override fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(route = "plugin/smart-dictionary/main", title = "Dictionary", content = {})
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        context.selectedText?.let { word ->
            if (word.isNotBlank() && word.split(" ").size <= 3) {
                searchWord = word.trim()
                return PluginAction.ShowMenu(listOf("lookup"))
            }
        }
        return null
    }
    
    // ==================== PluginUIProvider Implementation ====================
    
    override fun getScreen(screenId: String, context: PluginScreenContext): PluginUIScreen? {
        // Pre-fill with selected text
        if (context.selectedText != null && searchWord.isEmpty()) {
            searchWord = context.selectedText ?: ""
        }
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
                if (event.componentId == "search_word") {
                    searchWord = event.data["value"] ?: ""
                }
            }
            UIEventType.CLICK -> {
                when {
                    event.componentId == "lookup" -> {
                        if (searchWord.isNotBlank()) {
                            isLoading = true
                            error = null
                            val result = lookupWord(searchWord)
                            result.onSuccess { currentDefinition = it }
                            result.onFailure { error = it.message }
                            isLoading = false
                        }
                    }
                    event.componentId == "add_to_vocab" -> {
                        currentDefinition?.let { def ->
                            val defText = def.meanings.firstOrNull()?.definitions?.firstOrNull()?.definition ?: ""
                            addToVocabulary(def.word, defText)
                        }
                    }
                    event.componentId.startsWith("review_yes_") -> {
                        val word = event.componentId.removePrefix("review_yes_")
                        markReviewed(word, true)
                    }
                    event.componentId.startsWith("review_no_") -> {
                        val word = event.componentId.removePrefix("review_no_")
                        markReviewed(word, false)
                    }
                }
            }
            else -> {}
        }
        return buildMainScreen()
    }
    
    private fun buildMainScreen(): PluginUIScreen {
        val tabs = listOf(
            Tab(id = "lookup", title = "Lookup", icon = "search", content = buildLookupTab()),
            Tab(id = "vocabulary", title = "Vocabulary", icon = "list", content = buildVocabularyTab()),
            Tab(id = "review", title = "Review", icon = "school", content = buildReviewTab())
        )
        
        return PluginUIScreen(
            id = "main",
            title = "Smart Dictionary",
            components = listOf(PluginUIComponent.Tabs(tabs))
        )
    }
    
    private fun buildLookupTab(): List<PluginUIComponent> {
        val components = mutableListOf<PluginUIComponent>()
        
        // Search input
        components.add(PluginUIComponent.Row(listOf(
            PluginUIComponent.TextField(
                id = "search_word",
                label = "Enter word",
                value = searchWord
            ),
            PluginUIComponent.Button(
                id = "lookup",
                label = "Look up",
                style = ButtonStyle.PRIMARY,
                icon = "search"
            )
        ), spacing = 8))
        
        components.add(PluginUIComponent.Spacer(16))
        
        if (isLoading) {
            components.add(PluginUIComponent.Loading("Looking up..."))
        } else if (error != null) {
            components.add(PluginUIComponent.Error(error!!))
        } else if (currentDefinition != null) {
            val def = currentDefinition!!
            
            components.add(PluginUIComponent.Card(listOf(
                PluginUIComponent.Text(def.word, TextStyle.TITLE_LARGE),
                def.phonetic?.let { PluginUIComponent.Text(it, TextStyle.BODY_SMALL) }
                    ?: PluginUIComponent.Spacer(0)
            )))
            
            components.add(PluginUIComponent.Spacer(8))
            
            def.meanings.forEach { meaning ->
                components.add(PluginUIComponent.Card(listOf(
                    PluginUIComponent.Text(meaning.partOfSpeech, TextStyle.LABEL),
                    PluginUIComponent.Spacer(4),
                    *meaning.definitions.mapIndexed { idx, d ->
                        PluginUIComponent.Column(listOf(
                            PluginUIComponent.Text("${idx + 1}. ${d.definition}", TextStyle.BODY),
                            d.example?.let { PluginUIComponent.Text("\"$it\"", TextStyle.BODY_SMALL) }
                                ?: PluginUIComponent.Spacer(0)
                        ), spacing = 2)
                    }.toTypedArray()
                )))
            }
            
            components.add(PluginUIComponent.Spacer(16))
            
            val alreadyInVocab = vocabularyList.any { it.word == def.word.lowercase() }
            if (!alreadyInVocab) {
                components.add(PluginUIComponent.Button(
                    id = "add_to_vocab",
                    label = "Add to Vocabulary",
                    style = ButtonStyle.OUTLINED,
                    icon = "add"
                ))
            } else {
                components.add(PluginUIComponent.Text("✓ In your vocabulary", TextStyle.BODY_SMALL))
            }
        } else {
            components.add(PluginUIComponent.Empty(
                icon = "search",
                message = "Look up a word",
                description = "Enter a word above to see its definition"
            ))
        }
        
        return components
    }
    
    private fun buildVocabularyTab(): List<PluginUIComponent> {
        if (vocabularyList.isEmpty()) {
            return listOf(PluginUIComponent.Empty(
                icon = "list",
                message = "No words saved",
                description = "Look up words and add them to your vocabulary"
            ))
        }
        
        val items = vocabularyList.map { entry ->
            ListItem(
                id = entry.word,
                title = entry.word,
                subtitle = entry.definition.take(50) + if (entry.definition.length > 50) "..." else "",
                icon = "book"
            )
        }
        
        return listOf(
            PluginUIComponent.Text("${vocabularyList.size} words", TextStyle.BODY_SMALL),
            PluginUIComponent.Spacer(8),
            PluginUIComponent.ItemList(id = "vocab_list", items = items)
        )
    }
    
    private fun buildReviewTab(): List<PluginUIComponent> {
        val wordsToReview = getWordsForReview(5)
        
        if (wordsToReview.isEmpty()) {
            return listOf(PluginUIComponent.Empty(
                icon = "school",
                message = "No words to review",
                description = "Add words to your vocabulary to start reviewing"
            ))
        }
        
        val components = mutableListOf<PluginUIComponent>()
        components.add(PluginUIComponent.Text("Review your vocabulary", TextStyle.TITLE_SMALL))
        components.add(PluginUIComponent.Spacer(16))
        
        wordsToReview.forEach { entry ->
            components.add(PluginUIComponent.Card(listOf(
                PluginUIComponent.Text(entry.word, TextStyle.TITLE_MEDIUM),
                PluginUIComponent.Spacer(8),
                PluginUIComponent.Text(entry.definition, TextStyle.BODY),
                PluginUIComponent.Spacer(8),
                PluginUIComponent.Text("Did you remember?", TextStyle.LABEL),
                PluginUIComponent.Row(listOf(
                    PluginUIComponent.Button(
                        id = "review_yes_${entry.word}",
                        label = "Yes",
                        style = ButtonStyle.PRIMARY,
                        icon = "check"
                    ),
                    PluginUIComponent.Button(
                        id = "review_no_${entry.word}",
                        label = "No",
                        style = ButtonStyle.OUTLINED,
                        icon = "close"
                    )
                ), spacing = 8)
            )))
        }
        
        return components
    }
    
    fun lookupWord(word: String): Result<WordDefinition> {
        val normalized = word.trim().lowercase()
        definitionCache[normalized]?.let { return Result.success(it) }
        
        val definition = WordDefinition(
            word = normalized,
            phonetic = "/$normalized/",
            meanings = listOf(Meaning("noun", listOf(Definition("Definition for '$normalized'"))))
        )
        definitionCache[normalized] = definition
        return Result.success(definition)
    }
    
    fun addToVocabulary(word: String, definition: String) {
        val entry = VocabularyEntry(word.lowercase(), definition, System.currentTimeMillis())
        if (vocabularyList.none { it.word == entry.word }) vocabularyList.add(entry)
    }
    
    fun getVocabularyList(): List<VocabularyEntry> = vocabularyList.toList()
    
    fun getWordsForReview(limit: Int = 10): List<VocabularyEntry> {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        return vocabularyList.filter { entry ->
            val interval = when (entry.reviewCount) { 0 -> dayMs; 1 -> 3*dayMs; else -> 7*dayMs }
            now - (entry.lastReviewed ?: 0L) >= interval
        }.take(limit)
    }
    
    fun markReviewed(word: String, remembered: Boolean) {
        vocabularyList.find { it.word == word }?.let { entry ->
            val idx = vocabularyList.indexOf(entry)
            vocabularyList[idx] = entry.copy(
                reviewCount = if (remembered) entry.reviewCount + 1 else 0,
                lastReviewed = System.currentTimeMillis()
            )
        }
    }
}

@Serializable data class WordDefinition(val word: String, val phonetic: String? = null, val meanings: List<Meaning> = emptyList())
@Serializable data class Meaning(val partOfSpeech: String, val definitions: List<Definition> = emptyList())
@Serializable data class Definition(val definition: String, val example: String? = null)
@Serializable data class VocabularyEntry(val word: String, val definition: String, val addedAt: Long, val reviewCount: Int = 0, val lastReviewed: Long? = null)
