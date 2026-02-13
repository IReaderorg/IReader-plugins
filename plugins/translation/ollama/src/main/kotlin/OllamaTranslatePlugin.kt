package io.github.ireaderorg.plugins.ollamatranslate

import ireader.plugin.api.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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
        version = "2.1.0",
        versionCode = 3,
        description = "Local AI translation using Ollama. Run LLMs locally for private, offline translation. Supports both /api/chat and /api/generate endpoints. Auto-detects available models.",
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
    
    // Cached available models from Ollama server (thread-safe)
    @Volatile
    private var cachedModels: List<ModelInfo> = emptyList()
    @Volatile
    private var lastModelFetchTime: Long = 0
    private val modelCacheDurationMs: Long = 60000 // 1 minute cache
    private val modelFetchLock = Any()
    
    /**
     * Model information from Ollama server
     */
    data class ModelInfo(
        val name: String,
        val displayName: String,
        val size: String?,
        val details: String?
    )
    
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
        PluginConfig.Action(
            key = "refresh_models",
            name = "Refresh Models",
            description = "Fetch available models from Ollama server",
            buttonText = "Refresh"
        ),
        PluginConfig.Select(
            key = "model",
            name = "Model",
            options = emptyList(), // Will be populated dynamically from preferences
            defaultValue = 0,
            description = "Select the LLM model to use for translation (click Refresh to fetch models)"
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
        PluginConfig.Text(
            key = "custom_prompt",
            name = "Custom Prompt",
            defaultValue = "",
            description = "Add custom instructions to append to translation prompts (e.g., 'Use British English', 'Keep honorifics')",
            placeholder = "e.g., Use British English spelling, Keep Japanese honorifics",
            inputType = TextInputType.TEXT
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
    
    /**
     * Get cached model options for the dropdown (thread-safe)
     * Also loads from preferences if in-memory cache is empty
     */
    private fun getCachedModelOptions(): List<String> {
        var models = synchronized(modelFetchLock) { cachedModels.toList() }
        
        // If in-memory cache is empty, try loading from preferences
        if (models.isEmpty()) {
            models = loadModelsFromPreferences()
            if (models.isNotEmpty()) {
                synchronized(modelFetchLock) {
                    cachedModels = models
                }
            }
        }
        
        // Return display names, or empty list if no models available
        return models.map { it.displayName }
    }
    
    override fun onConfigChanged(key: String, value: Any?) {
        when (key) {
            "server_url" -> {
                serverUrl = (value as? String) ?: "http://localhost:11434"
                context?.preferences?.putString("server_url", serverUrl)
                // Clear model cache when URL changes
                synchronized(modelFetchLock) {
                    cachedModels = emptyList()
                }
            }
            "api_endpoint" -> {
                val endpointIndex = (value as? Int) ?: 0
                useGenerateEndpoint = endpointIndex == 1
                context?.preferences?.putBoolean("use_generate_endpoint", useGenerateEndpoint)
            }
            "refresh_models" -> {
                // Trigger model refresh - use synchronous version for immediate feedback
                fetchAvailableModelsSync()
            }
            "model" -> {
                val modelIndex = (value as? Int) ?: 0
                val models = synchronized(modelFetchLock) { cachedModels.toList() }
                if (models.isNotEmpty() && modelIndex < models.size) {
                    model = models[modelIndex].name
                    context?.preferences?.putString("model", model)
                }
            }
            "custom_model" -> {
                val customModel = (value as? String)?.trim()
                if (!customModel.isNullOrBlank()) {
                    model = customModel
                    context?.preferences?.putString("model", model)
                }
            }
            "custom_prompt" -> {
                val prompt = (value as? String) ?: ""
                context?.preferences?.putString("custom_prompt", prompt)
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
            "api_endpoint" -> if (useGenerateEndpoint) 1 else 0
            "model" -> {
                // Return the index of the current model (thread-safe)
                val models = synchronized(modelFetchLock) { cachedModels.toList() }
                if (models.isEmpty()) {
                    return 0
                }
                
                // Find index of current model
                val index = models.indexOfFirst { it.name == model }
                if (index >= 0) {
                    index
                } else {
                    // Model not in list - might be custom, select first as fallback
                    0
                }
            }
            "custom_model" -> {
                // Return custom model if it's not in the cached list
                val models = synchronized(modelFetchLock) { cachedModels.toList() }
                if (model.isNotBlank() && (models.isEmpty() || model !in models.map { it.name })) {
                    model
                } else {
                    ""
                }
            }
            "custom_prompt" -> context?.preferences?.getString("custom_prompt", "") ?: ""
            "cached_models_list" -> {
                // Return the list of model display names for the dropdown
                getCachedModelOptions()
            }
            "temperature" -> context?.preferences?.getFloat("temperature", 0.1f) ?: 0.1f
            "max_tokens" -> context?.preferences?.getInt("max_tokens", 8192) ?: 8192
            else -> null
        }
    }
    
    override fun initialize(context: PluginContext) {
        this.context = context
        serverUrl = context.preferences.getString("server_url", "http://localhost:11434")
        model = context.preferences.getString("model", "mistral")
        useGenerateEndpoint = context.preferences.getBoolean("use_generate_endpoint", false)
        
        // Load cached models from preferences first
        val savedModels = loadModelsFromPreferences()
        if (savedModels.isNotEmpty()) {
            synchronized(modelFetchLock) {
                cachedModels = savedModels
            }
        }
        
        // Then fetch fresh models in background
        fetchAvailableModelsAsyncSafe()
    }
    
    override fun cleanup() {
        context = null
    }
    
    /**
     * Fetch available models from Ollama server asynchronously
     * This is called on initialization and when the refresh button is clicked
     */
    private suspend fun fetchAvailableModelsAsync() {
        val ctx = context ?: return
        val httpClient = ctx.httpClient ?: return
        
        try {
            val tagsUrl = serverUrl.trimEnd('/') + "/api/tags"
            val response = httpClient.get(
                url = tagsUrl,
                headers = mapOf("Content-Type" to "application/json")
            )
            
            if (response.statusCode in 200..299) {
                val models = parseModelsResponse(response.body)
                synchronized(modelFetchLock) {
                    cachedModels = models
                    lastModelFetchTime = System.currentTimeMillis()
                }
                
                // Save models to preferences for persistence
                saveModelsToPreferences(models)
                
                // Auto-select first model if current model is not set
                if (model.isBlank() && models.isNotEmpty()) {
                    model = models[0].name
                    context?.preferences?.putString("model", model)
                }
            }
        } catch (e: Exception) {
            // Silently fail - will use default models
        }
    }
    
    /**
     * Non-suspend wrapper for fetching models (for use in initialize)
     */
    private fun fetchAvailableModelsAsyncSafe() {
        // Use GlobalScope for background model fetching
        // This is safe because we're just caching data
        GlobalScope.launch {
            fetchAvailableModelsAsync()
        }
    }
    
    /**
     * Synchronous fetch for use with refresh button - blocks until complete
     */
    private fun fetchAvailableModelsSync(): Boolean {
        val ctx = context ?: return false
        val httpClient = ctx.httpClient ?: return false
        
        return try {
            val tagsUrl = serverUrl.trimEnd('/') + "/api/tags"
            
            // Use runBlocking for synchronous execution
            runBlocking {
                val response = httpClient.get(
                    url = tagsUrl,
                    headers = mapOf("Content-Type" to "application/json")
                )
                
                if (response.statusCode in 200..299) {
                    val models = parseModelsResponse(response.body)
                    synchronized(modelFetchLock) {
                        cachedModels = models
                        lastModelFetchTime = System.currentTimeMillis()
                    }
                    
                    // Save models to preferences for persistence
                    try {
                        saveModelsToPreferences(models)
                    } catch (e: Exception) {
                        // Continue anyway - models are in memory
                    }
                    
                    // Auto-select first model if current model is not set
                    if (model.isBlank() && models.isNotEmpty()) {
                        model = models[0].name
                        try {
                            context?.preferences?.putString("model", model)
                        } catch (e: Exception) {
                            // Ignore save errors
                        }
                    }
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Save models to preferences as JSON for persistence and app access
     */
    private fun saveModelsToPreferences(models: List<ModelInfo>) {
        try {
            // Format: name1|displayName1|size1;name2|displayName2|size2;...
            val modelsData = models.joinToString(";") { model ->
                "${model.name}|${model.displayName}|${model.size ?: ""}"
            }
            context?.preferences?.putString("cached_models", modelsData)
            context?.preferences?.putLong("models_fetch_time", System.currentTimeMillis())
        } catch (e: Exception) {
            // Silently fail
        }
    }
    
    /**
     * Load models from preferences
     */
    private fun loadModelsFromPreferences(): List<ModelInfo> {
        return try {
            val modelsData = context?.preferences?.getString("cached_models", "") ?: ""
            if (modelsData.isBlank()) return emptyList()
            
            modelsData.split(";").mapNotNull { modelStr ->
                val parts = modelStr.split("|")
                if (parts.size >= 2) {
                    ModelInfo(
                        name = parts[0],
                        displayName = parts[1],
                        size = parts.getOrNull(2)?.takeIf { it.isNotBlank() },
                        details = null
                    )
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Parse the /api/tags response to extract model information
     */
    private fun parseModelsResponse(body: String): List<ModelInfo> {
        val models = mutableListOf<ModelInfo>()
        
        try {
            // Find the "models" array
            val modelsStart = body.indexOf("\"models\"")
            if (modelsStart == -1) return emptyList()
            
            val arrayStart = body.indexOf("[", modelsStart)
            if (arrayStart == -1) return emptyList()
            
            var depth = 0
            var i = arrayStart
            var currentModelStart = -1
            var inModel = false
            
            while (i < body.length) {
                when (body[i]) {
                    '[' -> depth++
                    ']' -> {
                        depth--
                        if (depth == 0) {
                            // End of models array
                            if (inModel && currentModelStart >= 0) {
                                val modelJson = body.substring(currentModelStart, i)
                                parseSingleModel(modelJson)?.let { models.add(it) }
                            }
                            break
                        }
                    }
                    '{' -> {
                        depth++
                        if (depth == 2 && !inModel) {
                            // Model object opening (depth 2 = inside array, at model level)
                            currentModelStart = i
                            inModel = true
                        }
                    }
                    '}' -> {
                        depth--
                        if (depth == 1 && inModel) {
                            // Model object closing
                            val modelJson = body.substring(currentModelStart, i + 1)
                            parseSingleModel(modelJson)?.let { models.add(it) }
                            currentModelStart = -1
                            inModel = false
                        }
                    }
                }
                i++
            }
        } catch (e: Exception) {
            // Return empty list on parse error
        }
        
        return models
    }
    
    /**
     * Parse a single model object from the JSON
     */
    private fun parseSingleModel(json: String): ModelInfo? {
        try {
            val name = extractJsonValue(json, "name") ?: return null
            val sizeStr = extractJsonNumericValue(json, "size")
            val size = sizeStr?.toLongOrNull()
            
            // Extract details if present
            val detailsStart = json.indexOf("\"details\"")
            var details: String? = null
            if (detailsStart >= 0) {
                val family = extractJsonValue(json.substring(detailsStart), "family")
                val paramSize = extractJsonValue(json.substring(detailsStart), "parameter_size")
                details = listOfNotNull(family, paramSize).joinToString(" ")
            }
            
            // Create display name
            val displayName = buildString {
                append(name.removeSuffix(":latest"))
                if (size != null) {
                    append(" (${formatSize(size)})")
                }
            }
            
            return ModelInfo(
                name = name,
                displayName = displayName,
                size = size?.let { formatSize(it) },
                details = details
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Extract a string value from JSON
     */
    private fun extractJsonValue(json: String, key: String): String? {
        val keyStart = json.indexOf("\"$key\"")
        if (keyStart == -1) return null
        
        val colonPos = json.indexOf(":", keyStart)
        if (colonPos == -1) return null
        
        val valueStart = json.indexOf("\"", colonPos)
        if (valueStart == -1) return null
        
        val valueEnd = findJsonStringEnd(json, valueStart + 1)
        return json.substring(valueStart + 1, valueEnd).unescapeJson()
    }
    
    /**
     * Extract a numeric value from JSON
     */
    private fun extractJsonNumericValue(json: String, key: String): String? {
        val keyStart = json.indexOf("\"$key\"")
        if (keyStart == -1) return null
        
        val colonPos = json.indexOf(":", keyStart)
        if (colonPos == -1) return null
        
        // Skip whitespace after colon
        var valueStart = colonPos + 1
        while (valueStart < json.length && json[valueStart].isWhitespace()) {
            valueStart++
        }
        
        // Find end of number (comma, closing brace, or whitespace)
        var valueEnd = valueStart
        while (valueEnd < json.length && 
               json[valueEnd] != ',' && 
               json[valueEnd] != '}' && 
               json[valueEnd] != ']' &&
               !json[valueEnd].isWhitespace()) {
            valueEnd++
        }
        
        if (valueStart >= valueEnd) return null
        return json.substring(valueStart, valueEnd)
    }
    
    /**
     * Format file size in human-readable format
     */
    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
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
     * Uses streaming to avoid connection timeouts with long-running LLM responses
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
                "stream": true,
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
        
        val translatedText = parseChatStreamResponse(response.body)
        return Result.success(translatedText)
    }
    
    /**
     * Translate using /api/generate endpoint (simpler, single-turn completion)
     * Uses streaming to avoid connection timeouts with long-running LLM responses
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
                "stream": true,
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
        
        val translatedText = parseGenerateStreamResponse(response.body)
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
        // Get custom prompt from plugin preferences
        val customPrompt = context?.preferences?.getString("custom_prompt", "") ?: ""
        val customInstruction = if (customPrompt.isNotBlank()) {
            "\n\nAdditional instructions: $customPrompt"
        } else {
            ""
        }
        
        return """You are an expert literary translator. Your ONLY task is to translate text from $sourceLang to $targetLang.

CRITICAL RULES:
1. You MUST output ONLY in $targetLang language
2. Do NOT output any $sourceLang text
3. Do NOT explain or comment - just translate
4. Preserve paragraph structure using ---PARAGRAPH_BREAK--- as separator
5. Maintain narrative tone and style$customInstruction

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
     * Parse /api/chat streaming response format (NDJSON - newline-delimited JSON)
     * Each line is a JSON object with incremental content:
     * {"message":{"content":"Hello"},"done":false}
     * {"message":{"content":" world"},"done":false}
     * {"message":{"content":"!"},"done":true}
     */
    private fun parseChatStreamResponse(body: String): String {
        val result = StringBuilder()
        
        // Split by newlines to get individual JSON objects
        body.lines().forEach { line ->
            if (line.isBlank()) return@forEach
            
            try {
                // Extract content from each streaming chunk
                val contentStart = line.indexOf("\"content\":")
                if (contentStart >= 0) {
                    val valueStart = line.indexOf("\"", contentStart + 10) + 1
                    if (valueStart > 0) {
                        val valueEnd = findJsonStringEnd(line, valueStart)
                        if (valueEnd > valueStart) {
                            val chunk = line.substring(valueStart, valueEnd).unescapeJson()
                            result.append(chunk)
                        }
                    }
                }
            } catch (e: Exception) {
                // Skip malformed chunks
            }
        }
        
        return result.toString().trim()
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
    
    /**
     * Parse /api/generate streaming response format (NDJSON - newline-delimited JSON)
     * Each line is a JSON object with incremental content:
     * {"response":"Hello","done":false}
     * {"response":" world","done":false}
     * {"response":"!","done":true}
     */
    private fun parseGenerateStreamResponse(body: String): String {
        val result = StringBuilder()
        
        // Split by newlines to get individual JSON objects
        body.lines().forEach { line ->
            if (line.isBlank()) return@forEach
            
            try {
                // Extract response from each streaming chunk
                val responseStart = line.indexOf("\"response\":")
                if (responseStart >= 0) {
                    val valueStart = line.indexOf("\"", responseStart + 11) + 1
                    if (valueStart > 0) {
                        val valueEnd = findJsonStringEnd(line, valueStart)
                        if (valueEnd > valueStart) {
                            val chunk = line.substring(valueStart, valueEnd).unescapeJson()
                            result.append(chunk)
                        }
                    }
                }
            } catch (e: Exception) {
                // Skip malformed chunks
            }
        }
        
        return result.toString().trim()
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
