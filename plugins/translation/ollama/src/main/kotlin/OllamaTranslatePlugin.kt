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
        version = "1.0.0",
        versionCode = 1,
        description = "Local AI translation using Ollama. Run LLMs locally for private, offline translation.",
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
            "translation.model" to "mistral"
        )
    )
    
    private var context: PluginContext? = null
    private var serverUrl: String = "http://localhost:11434"
    private var model: String = "mistral"
    
    override fun initialize(context: PluginContext) {
        this.context = context
        serverUrl = context.preferences.getString("server_url", "http://localhost:11434")
        model = context.preferences.getString("model", "mistral")
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
        
        val systemPrompt = buildSystemPrompt(sourceLang, targetLang)
        val userPrompt = buildUserPrompt(text, sourceLang, targetLang)
        
        return try {
            val requestBody = """
                {
                    "model": "$model",
                    "messages": [
                        {"role": "system", "content": ${systemPrompt.escapeJson()}},
                        {"role": "user", "content": ${userPrompt.escapeJson()}}
                    ],
                    "stream": false,
                    "options": {
                        "temperature": 0.1,
                        "num_predict": 8192
                    }
                }
            """.trimIndent()
            
            val chatUrl = serverUrl.trimEnd('/') + "/api/chat"
            
            val response = httpClient.post(
                url = chatUrl,
                body = requestBody,
                headers = mapOf("Content-Type" to "application/json")
            )
            
            if (response.statusCode !in 200..299) {
                return Result.failure(Exception("Ollama API error: ${response.statusCode}. Is Ollama running?"))
            }
            
            val translatedText = parseResponse(response.body)
            Result.success(translatedText)
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("connection", ignoreCase = true) == true ->
                    "Cannot connect to Ollama. Make sure Ollama is running at $serverUrl"
                else -> "Translation failed: ${e.message}"
            }
            Result.failure(Exception(message))
        }
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
    
    private fun parseResponse(body: String): String {
        // Parse {"message": {"content": "..."}}
        val messageStart = body.indexOf("\"message\"")
        if (messageStart == -1) throw Exception("Invalid response format")
        
        val contentStart = body.indexOf("\"content\":", messageStart)
        if (contentStart == -1) throw Exception("Invalid response format")
        
        val valueStart = body.indexOf("\"", contentStart + 10) + 1
        val valueEnd = findJsonStringEnd(body, valueStart)
        
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
