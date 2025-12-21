package io.github.ireaderorg.plugins.aisummarizer

import ireader.plugin.api.*

/**
 * AI Summarizer Plugin Implementation
 * 
 * Provides AI-powered text summarization for novels using OpenAI or Claude APIs.
 * Features:
 * - Chapter summaries with character and event extraction
 * - Book-level summaries across multiple chapters
 * - "Previously on" recaps for returning readers
 * - Key point extraction
 * - Plot point detection
 * - Streaming support for real-time output
 */
class AISummarizerPluginImpl : AISummarizerPlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.ai-summarizer",
        name = "AI Summarizer",
        version = "1.0.0",
        versionCode = 1,
        description = "AI-powered text summarization for novels. Supports chapter summaries, book overviews, 'previously on' recaps, and key point extraction.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.FEATURE,
        permissions = listOf(
            PluginPermission.NETWORK,
            PluginPermission.PREFERENCES,
            PluginPermission.READER_CONTEXT
        ),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP),
        metadata = mapOf(
            "summarizer.defaultProvider" to "openai",
            "summarizer.defaultModel" to "gpt-3.5-turbo",
            "summarizer.maxContextLength" to "16000",
            "summarizer.supportsStreaming" to "true"
        )
    )
    
    override val summaryCapabilities: List<SummaryCapability> = listOf(
        SummaryCapability.CHAPTER_SUMMARY,
        SummaryCapability.BOOK_SUMMARY,
        SummaryCapability.RECAP,
        SummaryCapability.SELECTION_SUMMARY,
        SummaryCapability.KEY_POINTS,
        SummaryCapability.PLOT_DETECTION,
        SummaryCapability.CHARACTER_TRACKING,
        SummaryCapability.STREAMING,
        SummaryCapability.MULTI_LANGUAGE
    )
    
    override val providerConfig: SummaryProviderConfig
        get() = SummaryProviderConfig(
            providerName = currentProvider,
            modelName = currentModel,
            maxContextLength = maxContextLength,
            supportsStreaming = true,
            requiresApiKey = true,
            costPer1kTokens = when (currentProvider) {
                "openai" -> 0.002f
                "claude" -> 0.003f
                else -> null
            }
        )
    
    private var context: PluginContext? = null
    private var apiKey: String = ""
    private var currentProvider: String = "openai"
    private var currentModel: String = "gpt-3.5-turbo"
    private var maxContextLength: Int = 16000
    private var isCancelled: Boolean = false
    
    override fun initialize(context: PluginContext) {
        this.context = context
        apiKey = context.preferences.getString("api_key", "")
        currentProvider = context.preferences.getString("provider", "openai")
        currentModel = context.preferences.getString("model", "gpt-3.5-turbo")
        maxContextLength = context.preferences.getInt("max_context", 16000)
    }
    
    override fun cleanup() {
        context = null
        isCancelled = true
    }
    
    override suspend fun isReady(): Boolean {
        return apiKey.isNotBlank() && context != null
    }

    override suspend fun summarizeChapter(
        chapterContent: String,
        options: ChapterSummaryOptions
    ): SummaryResult<ChapterSummary> {
        if (!isReady()) {
            return SummaryResult.Error(SummaryError.ModelNotReady("API key not configured"))
        }
        
        val tokenCount = estimateTokens(chapterContent)
        if (tokenCount > maxContextLength) {
            return SummaryResult.Error(SummaryError.ContextTooLong(maxContextLength, tokenCount))
        }
        
        val prompt = buildChapterSummaryPrompt(chapterContent, options)
        
        return try {
            val response = callApi(prompt)
            val parsed = parseChapterSummaryResponse(response, chapterContent)
            SummaryResult.Success(parsed)
        } catch (e: Exception) {
            handleApiError(e)
        }
    }
    
    override suspend fun summarizeBook(
        chapters: List<SummaryChapterContent>,
        options: BookSummaryOptions
    ): SummaryResult<BookSummary> {
        if (!isReady()) {
            return SummaryResult.Error(SummaryError.ModelNotReady("API key not configured"))
        }
        
        // Summarize each chapter first, then combine
        val chapterSummaries = mutableListOf<ChapterSummary>()
        val combinedContent = StringBuilder()
        
        for (chapter in chapters) {
            combinedContent.append("Chapter ${chapter.chapterNumber}: ${chapter.title ?: "Untitled"}\n")
            combinedContent.append(chapter.content.take(2000)) // Truncate for context
            combinedContent.append("\n\n")
        }
        
        val prompt = buildBookSummaryPrompt(combinedContent.toString(), options, chapters.size)
        
        return try {
            val response = callApi(prompt)
            val parsed = parseBookSummaryResponse(response, chapters.size)
            SummaryResult.Success(parsed)
        } catch (e: Exception) {
            handleApiError(e)
        }
    }
    
    override suspend fun generateRecap(
        previousChapters: List<SummaryChapterContent>,
        currentChapterNumber: Int,
        options: RecapOptions
    ): SummaryResult<RecapSummary> {
        if (!isReady()) {
            return SummaryResult.Error(SummaryError.ModelNotReady("API key not configured"))
        }
        
        val chaptersToUse = previousChapters.takeLast(options.chaptersToConsider)
        val combinedContent = chaptersToUse.joinToString("\n\n") { chapter ->
            "Chapter ${chapter.chapterNumber}: ${chapter.title ?: "Untitled"}\n${chapter.content.take(1500)}"
        }
        
        val prompt = buildRecapPrompt(combinedContent, currentChapterNumber, options)
        
        return try {
            val response = callApi(prompt)
            val parsed = parseRecapResponse(response, chaptersToUse, currentChapterNumber)
            SummaryResult.Success(parsed)
        } catch (e: Exception) {
            handleApiError(e)
        }
    }
    
    override suspend fun summarizeSelection(
        selectedText: String,
        context: String?,
        options: SelectionSummaryOptions
    ): SummaryResult<String> {
        if (!isReady()) {
            return SummaryResult.Error(SummaryError.ModelNotReady("API key not configured"))
        }
        
        val prompt = buildSelectionSummaryPrompt(selectedText, context, options)
        
        return try {
            val response = callApi(prompt)
            SummaryResult.Success(response.trim())
        } catch (e: Exception) {
            handleApiError(e)
        }
    }
    
    override suspend fun extractKeyPoints(
        text: String,
        maxPoints: Int
    ): SummaryResult<List<KeyPoint>> {
        if (!isReady()) {
            return SummaryResult.Error(SummaryError.ModelNotReady("API key not configured"))
        }
        
        val prompt = """Extract the $maxPoints most important key points from this text.
For each point, provide:
- The key point (1-2 sentences)
- Importance level (1-5, where 5 is most important)
- Category (plot, character, setting, theme, or other)

Format each point as: [IMPORTANCE] CATEGORY: Point text

Text:
$text"""
        
        return try {
            val response = callApi(prompt)
            val keyPoints = parseKeyPointsResponse(response)
            SummaryResult.Success(keyPoints.take(maxPoints))
        } catch (e: Exception) {
            handleApiError(e)
        }
    }
    
    override suspend fun suggestChapterTitle(
        chapterContent: String
    ): SummaryResult<List<String>> {
        if (!isReady()) {
            return SummaryResult.Error(SummaryError.ModelNotReady("API key not configured"))
        }
        
        val prompt = """Based on this chapter content, suggest 3 compelling chapter titles.
The titles should be:
- Evocative and intriguing
- Relevant to the main events or themes
- Not spoilery but hint at the content

Return only the 3 titles, one per line.

Chapter content:
${chapterContent.take(3000)}"""
        
        return try {
            val response = callApi(prompt)
            val titles = response.lines().filter { it.isNotBlank() }.take(3)
            SummaryResult.Success(titles)
        } catch (e: Exception) {
            handleApiError(e)
        }
    }
    
    override suspend fun detectPlotPoints(
        chapterContent: String
    ): SummaryResult<List<PlotPoint>> {
        if (!isReady()) {
            return SummaryResult.Error(SummaryError.ModelNotReady("API key not configured"))
        }
        
        val prompt = """Analyze this chapter and identify key plot points.
For each plot point, provide:
- Description (1-2 sentences)
- Type (INTRODUCTION, RISING_ACTION, CONFLICT, CLIMAX, FALLING_ACTION, RESOLUTION, TWIST, FORESHADOWING, FLASHBACK, CHARACTER_DEVELOPMENT)
- Characters involved (comma-separated names)
- Significance (1-5, where 5 is most significant)

Format: TYPE | Significance | Characters | Description

Chapter:
${chapterContent.take(4000)}"""
        
        return try {
            val response = callApi(prompt)
            val plotPoints = parsePlotPointsResponse(response)
            SummaryResult.Success(plotPoints)
        } catch (e: Exception) {
            handleApiError(e)
        }
    }

    override suspend fun streamSummary(
        text: String,
        options: ChapterSummaryOptions,
        onToken: (String) -> Unit
    ): SummaryResult<String> {
        if (!isReady()) {
            return SummaryResult.Error(SummaryError.ModelNotReady("API key not configured"))
        }
        
        isCancelled = false
        val prompt = buildChapterSummaryPrompt(text, options)
        
        return try {
            val fullResponse = StringBuilder()
            val httpClient = context?.httpClient
                ?: return SummaryResult.Error(SummaryError.NetworkError("HTTP client not available"))
            
            val requestBody = buildStreamingRequestBody(prompt)
            val response = httpClient.post(
                url = getApiUrl(),
                body = requestBody,
                headers = getApiHeaders()
            )
            
            if (response.statusCode !in 200..299) {
                return handleHttpError(response.statusCode)
            }
            
            // For non-streaming fallback, emit the whole response
            val content = parseApiResponse(response.body)
            for (char in content) {
                if (isCancelled) break
                onToken(char.toString())
                fullResponse.append(char)
            }
            
            SummaryResult.Success(fullResponse.toString())
        } catch (e: Exception) {
            handleApiError(e)
        }
    }
    
    override fun cancelSummarization() {
        isCancelled = true
    }
    
    override fun estimateTokens(text: String): Int {
        // Rough estimation: ~4 characters per token for English
        return (text.length / 4) + 1
    }
    
    override fun getMaxContextLength(): Int = maxContextLength
    
    // ==================== Private Helper Methods ====================
    
    private suspend fun callApi(prompt: String): String {
        val httpClient = context?.httpClient
            ?: throw Exception("HTTP client not available")
        
        val requestBody = buildRequestBody(prompt)
        val response = httpClient.post(
            url = getApiUrl(),
            body = requestBody,
            headers = getApiHeaders()
        )
        
        when (response.statusCode) {
            401 -> throw ApiException(401, "Invalid API key")
            402, 429 -> throw ApiException(429, "Rate limited or quota exceeded")
            !in 200..299 -> throw ApiException(response.statusCode, "API error: ${response.statusCode}")
        }
        
        return parseApiResponse(response.body)
    }
    
    private fun getApiUrl(): String = when (currentProvider) {
        "openai" -> "https://api.openai.com/v1/chat/completions"
        "claude" -> "https://api.anthropic.com/v1/messages"
        else -> "https://api.openai.com/v1/chat/completions"
    }
    
    private fun getApiHeaders(): Map<String, String> = when (currentProvider) {
        "openai" -> mapOf(
            "Authorization" to "Bearer $apiKey",
            "Content-Type" to "application/json"
        )
        "claude" -> mapOf(
            "x-api-key" to apiKey,
            "anthropic-version" to "2023-06-01",
            "Content-Type" to "application/json"
        )
        else -> mapOf(
            "Authorization" to "Bearer $apiKey",
            "Content-Type" to "application/json"
        )
    }
    
    private fun buildRequestBody(prompt: String): String = when (currentProvider) {
        "openai" -> """
            {
                "model": "$currentModel",
                "messages": [
                    {"role": "system", "content": "You are a skilled literary analyst and summarizer. Provide clear, accurate summaries that capture the essence of the text."},
                    {"role": "user", "content": ${prompt.escapeJson()}}
                ],
                "temperature": 0.3,
                "max_tokens": 2000
            }
        """.trimIndent()
        "claude" -> """
            {
                "model": "$currentModel",
                "max_tokens": 2000,
                "messages": [
                    {"role": "user", "content": ${prompt.escapeJson()}}
                ]
            }
        """.trimIndent()
        else -> """
            {
                "model": "$currentModel",
                "messages": [
                    {"role": "system", "content": "You are a skilled literary analyst and summarizer."},
                    {"role": "user", "content": ${prompt.escapeJson()}}
                ],
                "temperature": 0.3,
                "max_tokens": 2000
            }
        """.trimIndent()
    }
    
    private fun buildStreamingRequestBody(prompt: String): String = when (currentProvider) {
        "openai" -> """
            {
                "model": "$currentModel",
                "messages": [
                    {"role": "system", "content": "You are a skilled literary analyst and summarizer."},
                    {"role": "user", "content": ${prompt.escapeJson()}}
                ],
                "temperature": 0.3,
                "max_tokens": 2000,
                "stream": true
            }
        """.trimIndent()
        else -> buildRequestBody(prompt)
    }
    
    private fun parseApiResponse(body: String): String = when (currentProvider) {
        "openai" -> parseOpenAIResponse(body)
        "claude" -> parseClaudeResponse(body)
        else -> parseOpenAIResponse(body)
    }
    
    private fun parseOpenAIResponse(body: String): String {
        val contentStart = body.indexOf("\"content\":")
        if (contentStart == -1) throw Exception("Invalid response format")
        
        val valueStart = body.indexOf("\"", contentStart + 10) + 1
        val valueEnd = findJsonStringEnd(body, valueStart)
        
        return body.substring(valueStart, valueEnd).unescapeJson()
    }
    
    private fun parseClaudeResponse(body: String): String {
        val textStart = body.indexOf("\"text\":")
        if (textStart == -1) throw Exception("Invalid response format")
        
        val valueStart = body.indexOf("\"", textStart + 7) + 1
        val valueEnd = findJsonStringEnd(body, valueStart)
        
        return body.substring(valueStart, valueEnd).unescapeJson()
    }
    
    private fun findJsonStringEnd(json: String, start: Int): Int {
        var i = start
        while (i < json.length) {
            if (json[i] == '\\') { i += 2; continue }
            if (json[i] == '"') return i
            i++
        }
        return json.length
    }

    // ==================== Prompt Builders ====================
    
    private fun buildChapterSummaryPrompt(content: String, options: ChapterSummaryOptions): String {
        val lengthInstruction = when (options.length) {
            SummaryLength.SHORT -> "Keep the summary brief (2-3 sentences)."
            SummaryLength.MEDIUM -> "Provide a moderate summary (1-2 paragraphs)."
            SummaryLength.LONG -> "Provide a detailed summary (3-4 paragraphs)."
            SummaryLength.CUSTOM -> "Provide a comprehensive summary covering all major points."
        }
        
        val styleInstruction = when (options.style) {
            SummaryStyle.NARRATIVE -> "Write in a narrative style, as if telling the story."
            SummaryStyle.BULLET_POINTS -> "Use bullet points for clarity."
            SummaryStyle.CONCISE -> "Be concise and to the point."
            SummaryStyle.DETAILED -> "Provide detailed analysis."
        }
        
        val spoilerInstruction = when (options.spoilerLevel) {
            SpoilerLevel.NONE -> "Avoid any spoilers - be vague about specific events."
            SpoilerLevel.MINOR -> "Include general events but avoid major plot twists."
            SpoilerLevel.FULL -> "Include all relevant details."
        }
        
        val extras = buildList {
            if (options.includeCharacters) add("List the main characters mentioned.")
            if (options.includeKeyEvents) add("Highlight key events.")
            if (options.focusAspects.isNotEmpty()) add("Focus on: ${options.focusAspects.joinToString(", ")}")
        }.joinToString(" ")
        
        return """Summarize this chapter.
$lengthInstruction
$styleInstruction
$spoilerInstruction
$extras

After the summary, on separate lines provide:
CHARACTERS: [comma-separated list of characters mentioned]
KEY_EVENTS: [comma-separated list of key events]
MOOD: [one word describing the overall mood]

Chapter content:
${content.take(maxContextLength * 3)}"""
    }
    
    private fun buildBookSummaryPrompt(combinedContent: String, options: BookSummaryOptions, totalChapters: Int): String {
        val lengthInstruction = when (options.length) {
            SummaryLength.SHORT -> "Keep the summary brief (1 paragraph)."
            SummaryLength.MEDIUM -> "Provide a moderate summary (2-3 paragraphs)."
            SummaryLength.LONG -> "Provide a detailed summary (4-5 paragraphs)."
            SummaryLength.CUSTOM -> "Provide a comprehensive summary."
        }
        
        val extras = buildList {
            if (options.includeCharacterArcs) add("Describe main character arcs.")
            if (options.includeThemes) add("Identify major themes.")
        }.joinToString(" ")
        
        return """Summarize this book ($totalChapters chapters).
$lengthInstruction
$extras

After the summary, provide:
MAIN_CHARACTERS: [name - role - brief description] (one per line)
MAJOR_PLOT_POINTS: [comma-separated list]
${if (options.includeThemes) "THEMES: [comma-separated list]" else ""}

Book content:
$combinedContent"""
    }
    
    private fun buildRecapPrompt(content: String, currentChapter: Int, options: RecapOptions): String {
        val lengthInstruction = when (options.length) {
            SummaryLength.SHORT -> "Keep the recap brief (2-3 sentences)."
            SummaryLength.MEDIUM -> "Provide a moderate recap (1 paragraph)."
            SummaryLength.LONG -> "Provide a detailed recap (2 paragraphs)."
            SummaryLength.CUSTOM -> "Provide a comprehensive recap."
        }
        
        val styleInstruction = when (options.style) {
            SummaryStyle.NARRATIVE -> "Write as 'Previously on...' narrative."
            SummaryStyle.BULLET_POINTS -> "Use bullet points."
            SummaryStyle.CONCISE -> "Be concise and direct."
            SummaryStyle.DETAILED -> "Provide detailed analysis."
        }
        
        val focusInstruction = if (options.focusCharacters.isNotEmpty()) {
            "Focus especially on: ${options.focusCharacters.joinToString(", ")}"
        } else ""
        
        return """Create a "Previously on..." recap for someone about to read Chapter $currentChapter.
$lengthInstruction
$styleInstruction
$focusInstruction
${if (options.includeCliffhangers) "Mention any unresolved cliffhangers." else ""}

After the recap, provide:
KEY_EVENTS: [comma-separated list of events to remember]
CHARACTERS: [comma-separated list of relevant characters]
${if (options.includeCliffhangers) "CLIFFHANGERS: [comma-separated list of unresolved points]" else ""}

Previous chapters:
$content"""
    }
    
    private fun buildSelectionSummaryPrompt(text: String, context: String?, options: SelectionSummaryOptions): String {
        val lengthInstruction = when (options.length) {
            SummaryLength.SHORT -> "Summarize in 1-2 sentences."
            SummaryLength.MEDIUM -> "Summarize in 2-3 sentences."
            SummaryLength.LONG -> "Summarize in a short paragraph."
            SummaryLength.CUSTOM -> "Provide a detailed summary."
        }
        
        val contextInstruction = if (options.includeContext && context != null) {
            "Consider this surrounding context: $context"
        } else ""
        
        return """$lengthInstruction
$contextInstruction

Text to summarize:
$text"""
    }

    // ==================== Response Parsers ====================
    
    private fun parseChapterSummaryResponse(response: String, originalContent: String): ChapterSummary {
        val lines = response.lines()
        var summary = StringBuilder()
        var characters = emptyList<String>()
        var keyEvents = emptyList<String>()
        var mood: String? = null
        
        var inSummary = true
        for (line in lines) {
            when {
                line.startsWith("CHARACTERS:") -> {
                    inSummary = false
                    characters = line.removePrefix("CHARACTERS:").trim()
                        .split(",").map { it.trim() }.filter { it.isNotBlank() }
                }
                line.startsWith("KEY_EVENTS:") -> {
                    inSummary = false
                    keyEvents = line.removePrefix("KEY_EVENTS:").trim()
                        .split(",").map { it.trim() }.filter { it.isNotBlank() }
                }
                line.startsWith("MOOD:") -> {
                    inSummary = false
                    mood = line.removePrefix("MOOD:").trim()
                }
                inSummary -> summary.appendLine(line)
            }
        }
        
        val summaryText = summary.toString().trim()
        val originalWordCount = originalContent.split(Regex("\\s+")).size
        val summaryWordCount = summaryText.split(Regex("\\s+")).size
        
        return ChapterSummary(
            summary = summaryText,
            characters = characters,
            keyEvents = keyEvents,
            mood = mood,
            originalWordCount = originalWordCount,
            summaryWordCount = summaryWordCount,
            compressionRatio = if (originalWordCount > 0) summaryWordCount.toFloat() / originalWordCount else 0f
        )
    }
    
    private fun parseBookSummaryResponse(response: String, totalChapters: Int): BookSummary {
        val lines = response.lines()
        var summary = StringBuilder()
        val mainCharacters = mutableListOf<CharacterSummary>()
        var majorPlotPoints = emptyList<String>()
        var themes = emptyList<String>()
        
        var inSummary = true
        var inCharacters = false
        
        for (line in lines) {
            when {
                line.startsWith("MAIN_CHARACTERS:") -> {
                    inSummary = false
                    inCharacters = true
                }
                line.startsWith("MAJOR_PLOT_POINTS:") -> {
                    inCharacters = false
                    majorPlotPoints = line.removePrefix("MAJOR_PLOT_POINTS:").trim()
                        .split(",").map { it.trim() }.filter { it.isNotBlank() }
                }
                line.startsWith("THEMES:") -> {
                    themes = line.removePrefix("THEMES:").trim()
                        .split(",").map { it.trim() }.filter { it.isNotBlank() }
                }
                inCharacters && line.contains(" - ") -> {
                    val parts = line.split(" - ")
                    if (parts.size >= 2) {
                        mainCharacters.add(CharacterSummary(
                            name = parts[0].trim(),
                            role = parts.getOrElse(1) { "Unknown" }.trim(),
                            description = parts.getOrElse(2) { "" }.trim()
                        ))
                    }
                }
                inSummary -> summary.appendLine(line)
            }
        }
        
        return BookSummary(
            overallSummary = summary.toString().trim(),
            mainCharacters = mainCharacters,
            majorPlotPoints = majorPlotPoints,
            themes = themes,
            totalChapters = totalChapters
        )
    }
    
    private fun parseRecapResponse(
        response: String,
        chapters: List<SummaryChapterContent>,
        currentChapter: Int
    ): RecapSummary {
        val lines = response.lines()
        var recap = StringBuilder()
        var keyEvents = emptyList<String>()
        var characters = emptyList<String>()
        var cliffhangers = emptyList<String>()
        
        var inRecap = true
        for (line in lines) {
            when {
                line.startsWith("KEY_EVENTS:") -> {
                    inRecap = false
                    keyEvents = line.removePrefix("KEY_EVENTS:").trim()
                        .split(",").map { it.trim() }.filter { it.isNotBlank() }
                }
                line.startsWith("CHARACTERS:") -> {
                    inRecap = false
                    characters = line.removePrefix("CHARACTERS:").trim()
                        .split(",").map { it.trim() }.filter { it.isNotBlank() }
                }
                line.startsWith("CLIFFHANGERS:") -> {
                    inRecap = false
                    cliffhangers = line.removePrefix("CLIFFHANGERS:").trim()
                        .split(",").map { it.trim() }.filter { it.isNotBlank() }
                }
                inRecap -> recap.appendLine(line)
            }
        }
        
        val startChapter = chapters.firstOrNull()?.chapterNumber ?: 1
        val endChapter = chapters.lastOrNull()?.chapterNumber ?: (currentChapter - 1)
        
        return RecapSummary(
            recap = recap.toString().trim(),
            keyEvents = keyEvents,
            characters = characters,
            cliffhangers = cliffhangers,
            chaptersCoveredStart = startChapter,
            chaptersCoveredEnd = endChapter
        )
    }
    
    private fun parseKeyPointsResponse(response: String): List<KeyPoint> {
        return response.lines()
            .filter { it.isNotBlank() && it.contains(":") }
            .mapNotNull { line ->
                val match = Regex("""\[(\d)\]\s*(\w+):\s*(.+)""").find(line)
                if (match != null) {
                    val (importance, category, point) = match.destructured
                    KeyPoint(
                        point = point.trim(),
                        importance = importance.toIntOrNull() ?: 3,
                        category = category.lowercase()
                    )
                } else {
                    // Fallback parsing
                    KeyPoint(point = line.trim(), importance = 3, category = null)
                }
            }
    }
    
    private fun parsePlotPointsResponse(response: String): List<PlotPoint> {
        return response.lines()
            .filter { it.contains("|") }
            .mapNotNull { line ->
                val parts = line.split("|").map { it.trim() }
                if (parts.size >= 4) {
                    val typeStr = parts[0].uppercase().replace(" ", "_")
                    val type = try {
                        PlotPointType.valueOf(typeStr)
                    } catch (e: Exception) {
                        PlotPointType.RISING_ACTION
                    }
                    
                    PlotPoint(
                        description = parts[3],
                        type = type,
                        characters = parts[2].split(",").map { it.trim() }.filter { it.isNotBlank() },
                        significance = parts[1].toIntOrNull() ?: 3
                    )
                } else null
            }
    }
    
    // ==================== Error Handling ====================
    
    private fun <T> handleApiError(e: Exception): SummaryResult<T> {
        return when (e) {
            is ApiException -> when (e.code) {
                401 -> SummaryResult.Error(SummaryError.AuthenticationFailed(e.message ?: "Invalid API key"))
                429 -> SummaryResult.Error(SummaryError.RateLimited(null))
                else -> SummaryResult.Error(SummaryError.ApiError(e.code, e.message ?: "Unknown error"))
            }
            else -> SummaryResult.Error(SummaryError.NetworkError(e.message ?: "Network error"))
        }
    }
    
    private fun <T> handleHttpError(statusCode: Int): SummaryResult<T> {
        return when (statusCode) {
            401 -> SummaryResult.Error(SummaryError.AuthenticationFailed("Invalid API key"))
            429 -> SummaryResult.Error(SummaryError.RateLimited(null))
            else -> SummaryResult.Error(SummaryError.ApiError(statusCode, "HTTP error: $statusCode"))
        }
    }
    
    // ==================== Utility Extensions ====================
    
    private fun String.escapeJson(): String = "\"" + this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t") + "\""
    
    private fun String.unescapeJson(): String = this
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t")
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
    
    private class ApiException(val code: Int, message: String) : Exception(message)
    
    // ==================== Configuration ====================
    
    fun setApiKey(key: String) {
        apiKey = key
        context?.preferences?.putString("api_key", key)
    }
    
    fun setProvider(provider: String) {
        currentProvider = provider
        context?.preferences?.putString("provider", provider)
        // Update model based on provider
        currentModel = when (provider) {
            "openai" -> "gpt-3.5-turbo"
            "claude" -> "claude-3-haiku-20240307"
            else -> "gpt-3.5-turbo"
        }
        context?.preferences?.putString("model", currentModel)
    }
    
    fun setModel(model: String) {
        currentModel = model
        context?.preferences?.putString("model", model)
    }
    
    companion object {
        val SUPPORTED_PROVIDERS = listOf(
            "openai" to "OpenAI (GPT)",
            "claude" to "Anthropic (Claude)"
        )
        
        val OPENAI_MODELS = listOf(
            "gpt-3.5-turbo" to "GPT-3.5 Turbo (Fast, Affordable)",
            "gpt-4" to "GPT-4 (Most Capable)",
            "gpt-4-turbo" to "GPT-4 Turbo (Fast + Capable)"
        )
        
        val CLAUDE_MODELS = listOf(
            "claude-3-haiku-20240307" to "Claude 3 Haiku (Fast)",
            "claude-3-sonnet-20240229" to "Claude 3 Sonnet (Balanced)",
            "claude-3-opus-20240229" to "Claude 3 Opus (Most Capable)"
        )
    }
}
