package io.github.ireaderorg.plugins.aisummarizer

import ireader.plugin.api.AIResult
import ireader.plugin.api.AISummarizerPlugin
import ireader.plugin.api.BookSummary
import ireader.plugin.api.BookSummaryOptions
import ireader.plugin.api.Plugin
import ireader.plugin.api.PluginManifest
import ireader.plugin.api.PluginAuthor
import ireader.plugin.api.PluginType
import ireader.plugin.api.PluginPermission
import ireader.plugin.api.Platform
import ireader.plugin.api.PluginContext
import ireader.plugin.api.PluginUIProvider
import ireader.plugin.api.PluginUIScreen
import ireader.plugin.api.PluginUIComponent
import ireader.plugin.api.PluginUIEvent
import ireader.plugin.api.PluginScreenContext
import ireader.plugin.api.UIEventType
import ireader.plugin.api.TextStyle
import ireader.plugin.api.ButtonStyle
import ireader.plugin.api.ChapterSummary
import ireader.plugin.api.ChapterSummaryOptions
import ireader.plugin.api.KeyPoint
import ireader.plugin.api.Tab
import ireader.plugin.api.ListItem
import ireader.plugin.api.PlotPoint
import ireader.plugin.api.PluginAction
import ireader.plugin.api.PluginMenuItem
import ireader.plugin.api.PluginScreen
import ireader.plugin.api.ReaderContext
import ireader.plugin.api.RecapOptions
import ireader.plugin.api.RecapSummary
import ireader.plugin.api.SelectionSummaryOptions
import ireader.plugin.api.SummaryCapability
import ireader.plugin.api.SummaryChapterContent
import ireader.plugin.api.SummaryError
import ireader.plugin.api.SummaryProviderConfig
import ireader.plugin.api.SummaryResult

/**
 * AI Summarizer Plugin Implementation
 * Provides AI-powered text summarization using declarative UI.
 */
class AISummarizerPluginImpl : AISummarizerPlugin, PluginUIProvider {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.ai-summarizer",
        name = "AI Summarizer",
        version = "1.0.0",
        versionCode = 1,
        description = "AI-powered text summarization for novels",
        author = PluginAuthor(name = "IReader Team", website = "https://github.com/IReaderorg"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP),
        metadata = mapOf(
            "summarizer.defaultProvider" to "openai",
            "summarizer.defaultModel" to "gpt-3.5-turbo"
        )
    )
    
    override val summaryCapabilities: List<SummaryCapability> = listOf(
        SummaryCapability.CHAPTER_SUMMARY,
        SummaryCapability.KEY_POINTS,
        SummaryCapability.CHARACTER_TRACKING
    )
    
    override val providerConfig: SummaryProviderConfig
        get() = SummaryProviderConfig(
            providerName = currentProvider,
            modelName = currentModel,
            maxContextLength = 16000,
            supportsStreaming = true,
            requiresApiKey = true,
            costPer1kTokens = 0.002f
        )
    
    private var context: PluginContext? = null
    private var apiKey: String = ""
    private var currentProvider: String = "openai"
    private var currentModel: String = "gpt-3.5-turbo"
    
    // UI State
    private var currentTab = 0
    private var isLoading = false
    private var summary: String? = null
    private var keyPoints = listOf<String>()
    private var characters = listOf<String>()
    private var error: String? = null
    private var summaryLength = "Medium"
    
    override fun initialize(context: PluginContext) {
        this.context = context
        apiKey = context.preferences.getString("api_key", "")
        currentProvider = context.preferences.getString("provider", "openai")
        currentModel = context.preferences.getString("model", "gpt-3.5-turbo")
    }
    
    override fun cleanup() {
        context = null
    }
    
    override suspend fun isReady(): Boolean = apiKey.isNotBlank() && context != null
    
    // ==================== FeaturePlugin Implementation ====================
    
    fun getMenuItems(): List<PluginMenuItem> = listOf(
        PluginMenuItem(
            id = "summarize",
            label = "Summarize Chapter",
            icon = "auto_awesome",
            order = 0
        ),
        PluginMenuItem(id = "key_points", label = "Extract Key Points", icon = "list", order = 1),
        PluginMenuItem(id = "recap", label = "Previously On...", icon = "history", order = 2)
    )
    
    fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(
            route = "plugin/ai-summarizer/main",
            title = "AI Summarizer",
            content = {} // Content provided via PluginUIProvider
        )
    )
    
    fun onReaderContext(context: ReaderContext): PluginAction? {
        return PluginAction.ShowMenu(listOf("summarize", "key_points"))
    }
    
    // ==================== PluginUIProvider Implementation ====================
    
    override fun getScreen(screenId: String, context: PluginScreenContext): PluginUIScreen? {
        return buildMainScreen(context)
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
                if (event.componentId == "api_key") {
                    apiKey = event.data["value"] ?: ""
                    this.context?.preferences?.putString("api_key", apiKey)
                }
            }
            UIEventType.CHIP_SELECTED -> {
                when (event.componentId) {
                    "provider" -> {
                        currentProvider = event.data["value"] ?: "openai"
                        this.context?.preferences?.putString("provider", currentProvider)
                    }
                    "length" -> {
                        summaryLength = event.data["value"] ?: "Medium"
                    }
                }
            }
            UIEventType.CLICK -> {
                when (event.componentId) {
                    "generate" -> {
                        if (apiKey.isBlank()) {
                            error = "Please configure your API key in Settings"
                        } else {
                            isLoading = true
                            error = null
                            try {
                                val result = generateSummary(context.chapterContent ?: "")
                                summary = result.summary
                                keyPoints = result.keyPoints
                                characters = result.characters
                            } catch (e: Exception) {
                                error = e.message ?: "Failed to generate summary"
                            }
                            isLoading = false
                        }
                    }
                    "regenerate" -> {
                        summary = null
                        keyPoints = emptyList()
                        characters = emptyList()
                        // Trigger regeneration
                        if (apiKey.isNotBlank()) {
                            isLoading = true
                            error = null
                            try {
                                val result = generateSummary(context.chapterContent ?: "")
                                summary = result.summary
                                keyPoints = result.keyPoints
                                characters = result.characters
                            } catch (e: Exception) {
                                error = e.message ?: "Failed to generate summary"
                            }
                            isLoading = false
                        }
                    }
                }
            }
            else -> {}
        }
        
        return buildMainScreen(context)
    }
    
    private fun buildMainScreen(context: PluginScreenContext): PluginUIScreen {
        val tabs = listOf(
            Tab(
                id = "summary",
                title = "Summary",
                icon = "auto_awesome",
                content = buildSummaryTab(context)
            ),
            Tab(
                id = "key_points",
                title = "Key Points",
                icon = "list",
                content = buildKeyPointsTab()
            ),
            Tab(
                id = "characters",
                title = "Characters",
                icon = "people",
                content = buildCharactersTab()
            ),
            Tab(
                id = "settings",
                title = "Settings",
                icon = "settings",
                content = buildSettingsTab()
            )
        )
        
        return PluginUIScreen(
            id = "main",
            title = "AI Summarizer",
            components = listOf(PluginUIComponent.Tabs(tabs))
        )
    }
    
    private fun buildSummaryTab(context: PluginScreenContext): List<PluginUIComponent> {
        val components = mutableListOf<PluginUIComponent>()
        
        // Chapter info
        components.add(PluginUIComponent.Card(listOf(
            PluginUIComponent.Text("Chapter", TextStyle.LABEL),
            PluginUIComponent.Text(context.chapterTitle ?: "Unknown", TextStyle.TITLE_SMALL)
        )))
        
        components.add(PluginUIComponent.Spacer(16))
        
        // API key warning
        if (apiKey.isBlank()) {
            components.add(PluginUIComponent.Error("API key not configured. Go to Settings tab."))
            components.add(PluginUIComponent.Spacer(16))
        }
        
        // Generate button or loading
        if (isLoading) {
            components.add(PluginUIComponent.Loading("Generating summary..."))
        } else if (summary == null) {
            components.add(PluginUIComponent.Button(
                id = "generate",
                label = "Generate Summary",
                style = ButtonStyle.PRIMARY,
                icon = "auto_awesome"
            ))
        }
        
        // Error
        error?.let {
            components.add(PluginUIComponent.Spacer(16))
            components.add(PluginUIComponent.Error(it))
        }
        
        // Summary content
        summary?.let {
            components.add(PluginUIComponent.Card(listOf(
                PluginUIComponent.Text("Summary", TextStyle.TITLE_SMALL),
                PluginUIComponent.Spacer(8),
                PluginUIComponent.Text(it, TextStyle.BODY)
            )))
            
            components.add(PluginUIComponent.Spacer(16))
            components.add(PluginUIComponent.Button(
                id = "regenerate",
                label = "Regenerate",
                style = ButtonStyle.OUTLINED,
                icon = "refresh"
            ))
        }
        
        return components
    }
    
    private fun buildKeyPointsTab(): List<PluginUIComponent> {
        if (isLoading) {
            return listOf(PluginUIComponent.Loading())
        }
        
        if (keyPoints.isEmpty()) {
            return listOf(PluginUIComponent.Empty(
                icon = "list",
                message = "No key points yet",
                description = "Generate a summary first"
            ))
        }
        
        return keyPoints.map { point ->
            PluginUIComponent.Card(listOf(
                PluginUIComponent.Row(listOf(
                    PluginUIComponent.Text("•", TextStyle.BODY),
                    PluginUIComponent.Text(point, TextStyle.BODY)
                ), spacing = 8)
            ))
        }
    }
    
    private fun buildCharactersTab(): List<PluginUIComponent> {
        if (isLoading) {
            return listOf(PluginUIComponent.Loading())
        }
        
        if (characters.isEmpty()) {
            return listOf(PluginUIComponent.Empty(
                icon = "people",
                message = "No characters detected",
                description = "Generate a summary first"
            ))
        }
        
        val items = characters.map { char ->
            ListItem(id = char, title = char, icon = "person")
        }
        return listOf(PluginUIComponent.ItemList(id = "characters", items = items))
    }
    
    private fun buildSettingsTab(): List<PluginUIComponent> {
        return listOf(
            PluginUIComponent.Text("AI Provider Settings", TextStyle.TITLE_SMALL),
            PluginUIComponent.Spacer(16),
            
            PluginUIComponent.Card(listOf(
                PluginUIComponent.Text("API Key", TextStyle.LABEL),
                PluginUIComponent.Spacer(8),
                PluginUIComponent.TextField(
                    id = "api_key",
                    label = "Enter your API key",
                    value = apiKey
                )
            )),
            
            PluginUIComponent.Spacer(16),
            
            PluginUIComponent.Card(listOf(
                PluginUIComponent.Text("Provider", TextStyle.LABEL),
                PluginUIComponent.Spacer(8),
                PluginUIComponent.ChipGroup(
                    id = "provider",
                    chips = listOf(
                        PluginUIComponent.Chip("openai", "OpenAI", currentProvider == "openai"),
                        PluginUIComponent.Chip("claude", "Claude", currentProvider == "claude")
                    ),
                    singleSelection = true
                )
            )),
            
            PluginUIComponent.Spacer(16),
            
            PluginUIComponent.Card(listOf(
                PluginUIComponent.Text("Summary Length", TextStyle.LABEL),
                PluginUIComponent.Spacer(8),
                PluginUIComponent.ChipGroup(
                    id = "length",
                    chips = listOf(
                        PluginUIComponent.Chip("Short", "Short", summaryLength == "Short"),
                        PluginUIComponent.Chip("Medium", "Medium", summaryLength == "Medium"),
                        PluginUIComponent.Chip("Long", "Long", summaryLength == "Long")
                    ),
                    singleSelection = true
                )
            ))
        )
    }
    
    // ==================== AI Summary Logic ====================

    // Local result class for internal use
    private data class LocalSummaryResult(
        val summary: String,
        val keyPoints: List<String>,
        val characters: List<String>
    )
    
    private suspend fun generateSummary(content: String): LocalSummaryResult {
        if (apiKey.isBlank()) {
            throw Exception("API key not configured")
        }
        
        // Mock implementation - real implementation would call OpenAI/Claude API
        // using context?.httpClient
        kotlinx.coroutines.delay(1500)
        
        return LocalSummaryResult(
            summary = "This chapter follows the protagonist as they face new challenges. " +
                    "Key events unfold that advance the main plot, with character development " +
                    "and world-building elements woven throughout the narrative.",
            keyPoints = listOf(
                "Main character encounters a significant obstacle",
                "New information revealed about the antagonist",
                "Alliance formed with unexpected ally",
                "Foreshadowing of future conflict"
            ),
            characters = listOf("Protagonist", "Mentor", "Antagonist", "New Ally")
        )
    }
    
    // AISummarizerPlugin interface methods (simplified stubs)
    override suspend fun summarizeChapter(chapterContent: String, options: ChapterSummaryOptions): SummaryResult<ChapterSummary> {
        return SummaryResult.Error(SummaryError.ModelNotReady("Use the UI to generate summaries"))
    }
    
    override suspend fun summarizeBook(chapters: List<SummaryChapterContent>, options: BookSummaryOptions): SummaryResult<BookSummary> {
        return SummaryResult.Error(SummaryError.ModelNotReady("Not implemented"))
    }
    
    override suspend fun generateRecap(previousChapters: List<SummaryChapterContent>, currentChapterNumber: Int, options: RecapOptions): SummaryResult<RecapSummary> {
        return SummaryResult.Error(SummaryError.ModelNotReady("Not implemented"))
    }
    
    override suspend fun summarizeSelection(selectedText: String, context: String?, options: SelectionSummaryOptions): SummaryResult<String> {
        return SummaryResult.Error(SummaryError.ModelNotReady("Not implemented"))
    }
    
    override suspend fun extractKeyPoints(text: String, maxPoints: Int): SummaryResult<List<KeyPoint>> {
        return SummaryResult.Error(SummaryError.ModelNotReady("Not implemented"))
    }
    
    override suspend fun suggestChapterTitle(chapterContent: String): SummaryResult<List<String>> {
        return SummaryResult.Error(SummaryError.ModelNotReady("Not implemented"))
    }
    
    override suspend fun detectPlotPoints(chapterContent: String): SummaryResult<List<PlotPoint>> {
        return SummaryResult.Error(SummaryError.ModelNotReady("Not implemented"))
    }
    
    override suspend fun streamSummary(text: String, options: ChapterSummaryOptions, onToken: (String) -> Unit): SummaryResult<String> {
        return SummaryResult.Error(SummaryError.ModelNotReady("Not implemented"))
    }
    
    override fun cancelSummarization() {}
    override fun estimateTokens(text: String): Int = text.length / 4
    override fun getMaxContextLength(): Int = 16000
}
