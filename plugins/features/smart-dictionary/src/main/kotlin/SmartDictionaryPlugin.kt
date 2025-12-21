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
class SmartDictionaryPlugin : FeaturePlugin {
    
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
    
    override fun initialize(context: PluginContext) {
        this.context = context
    }
    
    override fun cleanup() {
        definitionCache.clear()
        context = null
    }

    override fun getMenuItems(): List<PluginMenuItem> = listOf(
        PluginMenuItem(id = "lookup", label = "Look up word", icon = "dictionary", order = 0),
        PluginMenuItem(id = "add_vocab", label = "Add to vocabulary", icon = "bookmark", order = 1)
    )
    
    override fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(route = "dictionary/lookup/{word}", title = "Definition", content = {}),
        PluginScreen(route = "dictionary/vocabulary", title = "My Vocabulary", content = {})
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        context.selectedText?.let { word ->
            if (word.isNotBlank() && word.split(" ").size <= 3) {
                return PluginAction.Navigate("dictionary/lookup/${word.trim()}")
            }
        }
        return null
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
