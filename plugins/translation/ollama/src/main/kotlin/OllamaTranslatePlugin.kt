package io.github.ireaderorg.plugins.ollamatranslate

import ireader.plugin.api.*

/**
 * Ollama Local LLM Translation Plugin
 * 
 * Uses locally-hosted Ollama for private, offline AI translation.
 * Requires Ollama server running on localhost or configured URL.
 */
class OllamaTranslatePlugin : TranslationPlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.ollama-translate",
        name = "Ollama Translation",
        version = "2.0.0",
        versionCode = 2,
        description = "Local AI translation using Ollama. Run LLMs locally for private, offline translation. Supports both /api/chat and /api/generate endpoints.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.TRANSLATION,
        permissions = listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP),
        metadata = mapOf(
            "translation.apiUrl" to "http://localhost:11434/api/chat",
            "translation.apiType" to "CUSTOM",
            "translation.requiresApiKey" to "false",
            "translation.maxCharsPerRequest" to "10000",
            "translation.rateLimitDelayMs" to "100",
            "translation.supportsAI" to "true",
            "translation.supportsContextAware" to "true",
            "translation.supportsStylePreservation" to "true",
            "translation.isOffline" to "true",
            "translation.model" to "mistral",
            "translation.endpoints" to "chat,generate"
        )
    )
    
    private var context: PluginContext? = null
    private var serverUrl: String = "http://localhost:11434"
    private var model: String = "mistral"
    private var useGenerateEndpoint: Boolean = false
    
    // ==================== Plugin Configuration ====================
    
    override fun getConfigFields(): List<PluginConfig<*>> = listOf(
        PluginConfig.Header(
            key = "server_header",
            name = "Server Settings",
            description = "Configure your Ollama server connection"
        ),
        PluginConfig.Text(
            key = "server_url",
            name = "Server URL",
            defaultValue = "http://localhost:11434",
            description = "URL of your Ollama server",
            placeholder = "http://localhost:11434",
            inputType = TextInputType.URL,
            required = true
        ),
        PluginConfig.Select(
            key = "api_endpoint",
            name = "API Endpoint",
            options = listOf("Chat (/api/chat)", "Generate (/api/generate)"),
            defaultValue = 0,
            description = "Choose which Ollama API endpoint to use"
        ),
        PluginConfig.Select(
            key = "model",
            name = "Model",
            options = listOf(
                "mistral", 
                "llama3.3", 
                "llama3.2", 
                "llama3.1", 
                "llama2", 
                "gemma2", 
                "gemma", 
                "phi4",
                "phi3", 
                "qwen2.5",
                "qwen2", 
                "deepseek-r1",
                "deepseek-coder",
                "codellama",
                "smollm2"
            ),
            defaultValue = 0,
            description = "Select the LLM model to use for translation"
        ),
        PluginConfig.Text(
            key = "custom_model",
            name = "Custom Model",
            defaultValue = "",
            description = "Or enter a custom model name (overrides selection above)",
            placeholder = "e.g., mixtral:8x7b"
        ),
        PluginConfig.Header(
            key = "advanced_header",
            name = "Advanced Settings"
        ),
        PluginConfig.Slider(
            key = "temperature",
            name = "Temperature",
            defaultValue = 0.1f,
            min = 0f,
            max = 1f,
            steps = 10,
            description = "Lower = more consistent, Higher = more creative",
            valueFormat = "%.1f"
        ),
        PluginConfig.Number(
            key = "max_tokens",
            name = "Max Tokens",
            defaultValue = 8192,
            min = 1000,
            max = 32000,
            description = "Maximum tokens in response"
        ),
        PluginConfig.Divider(key = "divider1"),
        PluginConfig.Header(
            key = "testing_header",
            name = "Testing Connection"
        ),
        PluginConfig.Note(
            key = "test_note",
            name = "How to Test",
            description = "To verify Ollama is running, open a terminal and run: curl http://localhost:11434/api/tags",
            noteType = NoteType.TIP
        ),
        PluginConfig.Note(
            key = "test_translation",
            name = "Test Translation",
            description = "The best way to test is to try translating some text in the app. If Ollama is not running, you'll see an error message.",
            noteType = NoteType.INFO
        ),
        PluginConfig.Divider(key = "divider2"),
        PluginConfig.Note(
            key = "api_note",
            name = "API Endpoints",
            description = "Chat endpoint (/api/chat) is recommended for better context handling. Generate endpoint (/api/generate) is simpler but may produce less consistent results.",
            noteType = NoteType.INFO
        ),
        PluginConfig.Note(
            key = "install_note",
            name = "Installation",
            description = "Install Ollama from ollama.com. Make sure the server is running before using. Pull your desired model with: ollama pull <model-name>",
            noteType = NoteType.INFO
        ),
        PluginConfig.Link(
            key = "ollama_website",
            name = "Get Ollama",
            url = "https://ollama.com",
            description = "Download and install Ollama",
            linkType = LinkType.EXTERNAL
        ),
        PluginConfig.Link(
            key = "ollama_models",
            name = "Browse Models",
            url = "https://ollama.com/library",
            description = "Explore available Ollama models",
            linkType = LinkType.EXTERNAL
        )
    )
    
    override fun onConfigChanged(key: String, value: Any?) {
        when (key) {
            "server_url" -> {
                serverUrl = (value as? String) ?: "http://localhost:11434"
                context?.preferences?.putString("server_url", serverUrl)
            }
            "api_endpoint" -> {
                val endpointIndex = (value as? Int) ?: 0
                useGenerateEndpoint = endpointIndex == 1
                context?.preferences?.putBoolean("use_generate_endpoint", useGenerateEndpoint)
            }
            "model" -> {
                val modelIndex = (value as? Int) ?: 0
                val models = listOf(
                    "mistral", 
                    "llama3.3", 
                    "llama3.2", 
                    "llama3.1", 
                    "llama2", 
                    "gemma2", 
                    "gemma", 
                    "phi4",
                    "phi3", 
                    "qwen2.5",
                    "qwen2", 
                    "deepseek-r1",
                    "deepseek-coder",
                    "codellama",
                    "smollm2"
                )
                model = models.getOrElse(modelIndex) { "mistral" }
                context?.preferences?.putString("model", model)
            }
            "custom_model" -> {
                val customModel = (value as? String)
                if (!customModel.isNullOrBlank()) {
                    model = customModel
                    context?.preferences?.putString("model", model)
                }
            }
            "temperature" -> {
                val temp = (value as? Float) ?: 0.1f
                context?.preferences?.putFloat("temperature", temp)
            }
            "max_tokens" -> {
                val tokens = (value as? Int) ?: 8192
                context?.preferences?.putInt("max_tokens", tokens)
            }
        }
    }
    
    override fun getConfigValue(key: String): Any? {
        return when (key) {
            "server_url" -> serverUrl
            "model" -> model
            "custom_model" -> model
            else -> null
        }
    }
    
    override fun initialize(context: PluginContext) {
        this.context = context
        serverUrl = context.preferences.getString("server_url", "http://localhost:11434")
        model = context.preferences.getString("model", "mistral")
        useGenerateEndpoint = context.preferences.getBoolean("use_generate_endpoint", false)
    }
    
    override fun cleanup() {
        context = null
    }
    
    override suspend fun translate(text: String, from: String, to: String): Result<String> {
        return translateWithContext(text, from, to, TranslationContext())
    }
    
    override suspend fun translateWithContext(
        text: String,
        from: String,
        to: String,
        context: TranslationContext
    ): Result<String> {
        if (text.isBlank()) return Result.success(text)
        
        val httpClient = this.context?.httpClient
            ?: return Result.failure(Exception("HTTP client not available"))
        
        val sourceLang = getLanguageName(from)
        val targetLang = getLanguageName(to)
        
        return try {
            if (useGenerateEndpoint) {
                translateWithGenerate(httpClient, text, sourceLang, targetLang)
            } else {
                translateWithChat(httpClient, text, sourceLang, targetLang)
            }
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("connection", ignoreCase = true) == true ->
                    "Cannot connect to Ollama. Make sure Ollama is running at $serverUrl"
                e.message?.contains("404", ignoreCase = true) == true ->
                    "Model '$model' not found. Pull it first with: ollama pull $model"
                else -> "Translation failed: ${e.message}"
            }
            Result.failure(Exception(message))
        }
    }
    
    /**
     * Translate using /api/chat endpoint (recommended for conversational context)
     */
    private suspend fun translateWithChat(
        httpClient: Any,
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> {
        val systemPrompt = buildSystemPrompt(sourceLang, targetLang)
        val userPrompt = buildUserPrompt(text, sourceLang, targetLang)
        
        val temperature = this.context?.preferences?.getFloat("temperature", 0.1f) ?: 0.1f
        val maxTokens = this.context?.preferences?.getInt("max_tokens", 8192) ?: 8192
        
        val requestBody = """
            {
                "model": "$model",
                "messages": [
                    {"role": "system", "content": ${systemPrompt.escapeJson()}},
                    {"role": "user", "content": ${userPrompt.escapeJson()}}
                ],
                "stream": false,
                "options": {
                    "temperature": $temperature,
                    "num_predict": $maxTokens
                }
            }
        """.trimIndent()
        
        val chatUrl = serverUrl.trimEnd('/') + "/api/chat"
        
        val response = (httpClient as? ireader.plugin.api.PluginHttpClientProvider)?.post(
            url = chatUrl,
            body = requestBody,
            headers = mapOf("Content-Type" to "application/json")
        ) ?: return Result.failure(Exception("HTTP client error"))
        
        if (response.statusCode !in 200..299) {
            return Result.failure(Exception("Ollama API error: ${response.statusCode}. Is Ollama running?"))
        }
        
        val translatedText = parseChatResponse(response.body)
        return Result.success(translatedText)
    }
    
    /**
     * Translate using /api/generate endpoint (simpler, single-turn completion)
     */
    private suspend fun translateWithGenerate(
        httpClient: Any,
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> {
        val systemPrompt = buildSystemPrompt(sourceLang, targetLang)
        val userPrompt = buildUserPrompt(text, sourceLang, targetLang)
        val fullPrompt = "$systemPrompt\n\n$userPrompt"
        
        val temperature = this.context?.preferences?.getFloat("temperature", 0.1f) ?: 0.1f
        val maxTokens = this.context?.preferences?.getInt("max_tokens", 8192) ?: 8192
        
        val requestBody = """
            {
                "model": "$model",
                "prompt": ${fullPrompt.escapeJson()},
                "stream": false,
                "options": {
                    "temperature": $temperature,
                    "num_predict": $maxTokens
                }
            }
        """.trimIndent()
        
        val generateUrl = serverUrl.trimEnd('/') + "/api/generate"
        
        val response = (httpClient as? ireader.plugin.api.PluginHttpClientProvider)?.post(
            url = generateUrl,
            body = requestBody,
            headers = mapOf("Content-Type" to "application/json")
        ) ?: return Result.failure(Exception("HTTP client error"))
        
        if (response.statusCode !in 200..299) {
            return Result.failure(Exception("Ollama API error: ${response.statusCode}. Is Ollama running?"))
        }
        
        val translatedText = parseGenerateResponse(response.body)
        return Result.success(translatedText)
    }
    
    override suspend fun translateBatch(texts: List<String>, from: String, to: String): Result<List<String>> {
        if (texts.isEmpty()) return Result.success(emptyList())
        
        val combinedText = texts.joinToString("\n---PARAGRAPH_BREAK---\n")
        return translate(combinedText, from, to).map { result ->
            if (result.contains("---PARAGRAPH_BREAK---")) {
                result.split("---PARAGRAPH_BREAK---").map { it.trim() }
            } else {
                listOf(result)
            }
        }
    }
    
    override fun getSupportedLanguages(): List<LanguagePair> = listOf(LanguagePair("*", "*"))
    
    override fun getAvailableLanguages(): List<Pair<String, String>> = SUPPORTED_LANGUAGES
    
    override fun requiresApiKey(): Boolean = false
    
    override fun configureApiKey(key: String) {
        // Not required - but we can use this to configure server URL
    }
    
    /**
     * Configure the Ollama server URL
     */
    fun configureServerUrl(url: String) {
        serverUrl = url
        context?.preferences?.putString("server_url", url)
    }
    
    /**
     * Configure the model to use
     */
    fun configureModel(modelName: String) {
        model = modelName
        context?.preferences?.putString("model", modelName)
    }
    
    override val supportsAI: Boolean = true
    override val supportsContextAwareTranslation: Boolean = true
    override val supportsStylePreservation: Boolean = true
    override val isOffline: Boolean = true
    override val maxCharsPerRequest: Int = 10000
    override val rateLimitDelayMs: Long = 100L
    
    private fun buildSystemPrompt(sourceLang: String, targetLang: String): String {
        return """You are an expert literary translator. Your ONLY task is to translate text from $sourceLang to $targetLang.

CRITICAL RULES:
1. You MUST output ONLY in $targetLang language
2. Do NOT output any $sourceLang text
3. Do NOT explain or comment - just translate
4. Preserve paragraph structure using ---PARAGRAPH_BREAK--- as separator
5. Maintain narrative tone and style

You are translating a novel/fiction text."""
    }
    
    private fun buildUserPrompt(text: String, sourceLang: String, targetLang: String): String {
        return """Translate the following $sourceLang text to $targetLang.
OUTPUT MUST BE IN $targetLang ONLY.

Text to translate:

$text"""
    }
    
    /**
     * Parse /api/chat response format:
     * {
     *   "model": "...",
     *   "created_at": "...",
     *   "message": {
     *     "role": "assistant",
     *     "content": "..."
     *   },
     *   "done": true,
     *   "done_reason": "stop"
     * }
     */
    private fun parseChatResponse(body: String): String {
        val messageStart = body.indexOf("\"message\"")
        if (messageStart == -1) throw Exception("Invalid response format: missing 'message' field")
        
        val contentStart = body.indexOf("\"content\":", messageStart)
        if (contentStart == -1) throw Exception("Invalid response format: missing 'content' field")
        
        val valueStart = body.indexOf("\"", contentStart + 10) + 1
        val valueEnd = findJsonStringEnd(body, valueStart)
        
        if (valueStart <= 0 || valueEnd <= valueStart) {
            throw Exception("Invalid response format: could not parse content")
        }
        
        return body.substring(valueStart, valueEnd).unescapeJson().trim()
    }
    
    /**
     * Parse /api/generate response format:
     * {
     *   "model": "...",
     *   "created_at": "...",
     *   "response": "...",
     *   "done": true,
     *   "done_reason": "stop"
     * }
     */
    private fun parseGenerateResponse(body: String): String {
        val responseStart = body.indexOf("\"response\":")
        if (responseStart == -1) throw Exception("Invalid response format: missing 'response' field")
        
        val valueStart = body.indexOf("\"", responseStart + 11) + 1
        val valueEnd = findJsonStringEnd(body, valueStart)
        
        if (valueStart <= 0 || valueEnd <= valueStart) {
            throw Exception("Invalid response format: could not parse response")
        }
        
        return body.substring(valueStart, valueEnd).unescapeJson().trim()
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
    
    private fun getLanguageName(code: String): String =
        SUPPORTED_LANGUAGES.find { it.first == code }?.second ?: code
    
    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            "auto" to "Auto-detect",
            "en" to "English",
            "zh" to "Chinese",
            "ja" to "Japanese",
            "ko" to "Korean",
            "es" to "Spanish",
            "fr" to "French",
            "de" to "German",
            "it" to "Italian",
            "pt" to "Portuguese",
            "ru" to "Russian",
            "ar" to "Arabic",
            "hi" to "Hindi",
            "th" to "Thai",
            "vi" to "Vietnamese",
            "id" to "Indonesian",
            "ms" to "Malay",
            "tr" to "Turkish",
            "pl" to "Polish",
            "nl" to "Dutch",
            "sv" to "Swedish",
            "da" to "Danish",
            "no" to "Norwegian",
            "fi" to "Finnish",
            "cs" to "Czech",
            "hu" to "Hungarian",
            "ro" to "Romanian",
            "bg" to "Bulgarian",
            "uk" to "Ukrainian",
            "el" to "Greek",
            "he" to "Hebrew",
            "fa" to "Persian"
        )
    }
}
