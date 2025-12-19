package io.github.ireaderorg.plugins.deepseektranslate

import ireader.plugin.api.*

/**
 * DeepSeek AI Translation Plugin
 * 
 * Uses DeepSeek's chat API for high-quality AI-powered translation.
 * Supports context-aware translation with style preservation.
 */
class DeepSeekTranslatePlugin : TranslationPlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.deepseek-translate",
        name = "DeepSeek Translation",
        version = "1.0.0",
        versionCode = 1,
        description = "AI-powered translation using DeepSeek API. Supports context-aware translation with style preservation for literary content.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.TRANSLATION,
        permissions = listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP),
        metadata = mapOf(
            "translation.apiUrl" to "https://api.deepseek.com/v1/chat/completions",
            "translation.apiType" to "OPENAI_COMPATIBLE",
            "translation.requiresApiKey" to "true",
            "translation.apiKeyHeader" to "Authorization",
            "translation.apiKeyPrefix" to "Bearer ",
            "translation.maxCharsPerRequest" to "8000",
            "translation.rateLimitDelayMs" to "3000",
            "translation.supportsAI" to "true",
            "translation.supportsContextAware" to "true",
            "translation.supportsStylePreservation" to "true",
            "translation.model" to "deepseek-chat",
            "translation.temperature" to "0.1",
            "translation.maxTokens" to "2000"
        )
    )
    
    private var context: PluginContext? = null
    private var apiKey: String = ""
    
    override fun initialize(context: PluginContext) {
        this.context = context
        // Load API key from preferences
        apiKey = context.preferences.getString("api_key", "")
    }
    
    override fun cleanup() {
        context = null
    }
    
    // ==================== Translation Implementation ====================
    
    override suspend fun translate(text: String, from: String, to: String): Result<String> {
        return translateWithContext(text, from, to, TranslationContext())
    }
    
    override suspend fun translateWithContext(
        text: String,
        from: String,
        to: String,
        context: TranslationContext
    ): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(Exception("DeepSeek API key not configured. Please set it in plugin settings."))
        }
        
        val httpClient = this.context?.httpClient
            ?: return Result.failure(Exception("HTTP client not available"))
        
        val sourceLang = if (from == "auto") "the source language" else getLanguageName(from)
        val targetLang = getLanguageName(to)
        
        val prompt = buildPrompt(text, sourceLang, targetLang, context)
        
        return try {
            val requestBody = buildRequestBody(prompt)
            
            val response = httpClient.post(
                url = "https://api.deepseek.com/v1/chat/completions",
                body = requestBody,
                headers = mapOf(
                    "Authorization" to "Bearer $apiKey",
                    "Content-Type" to "application/json"
                )
            )
            
            if (response.statusCode == 401) {
                return Result.failure(Exception("Invalid API key"))
            }
            if (response.statusCode == 402) {
                return Result.failure(Exception("Payment required - check your DeepSeek account balance"))
            }
            if (response.statusCode == 429) {
                return Result.failure(Exception("Rate limit exceeded"))
            }
            if (response.statusCode !in 200..299) {
                return Result.failure(Exception("API error: ${response.statusCode}"))
            }
            
            val translatedText = parseResponse(response.body)
            Result.success(translatedText)
        } catch (e: Exception) {
            Result.failure(Exception("Translation failed: ${e.message}"))
        }
    }
    
    override suspend fun translateBatch(texts: List<String>, from: String, to: String): Result<List<String>> {
        return translateBatchWithContext(texts, from, to, TranslationContext())
    }
    
    override suspend fun translateBatchWithContext(
        texts: List<String>,
        from: String,
        to: String,
        context: TranslationContext
    ): Result<List<String>> {
        if (texts.isEmpty()) {
            return Result.success(emptyList())
        }
        
        // Combine texts with paragraph markers for batch translation
        val combinedText = texts.joinToString("\n---PARAGRAPH_BREAK---\n")
        
        return translateWithContext(combinedText, from, to, context).map { result ->
            if (result.contains("---PARAGRAPH_BREAK---")) {
                result.split("---PARAGRAPH_BREAK---").map { it.trim() }
            } else {
                // If markers not preserved, return as single result
                listOf(result)
            }
        }
    }
    
    // ==================== Language Support ====================
    
    override fun getSupportedLanguages(): List<LanguagePair> {
        // DeepSeek supports translation between any of these languages
        return listOf(LanguagePair("*", "*"))
    }
    
    override fun getAvailableLanguages(): List<Pair<String, String>> = SUPPORTED_LANGUAGES
    
    // ==================== API Key ====================
    
    override fun requiresApiKey(): Boolean = true
    
    override fun configureApiKey(key: String) {
        apiKey = key
        context?.preferences?.putString("api_key", key)
    }
    
    override fun getApiKey(): String? = apiKey.ifBlank { null }
    
    // ==================== Capabilities ====================
    
    override val supportsAI: Boolean = true
    override val supportsContextAwareTranslation: Boolean = true
    override val supportsStylePreservation: Boolean = true
    override val isOffline: Boolean = false
    override val maxCharsPerRequest: Int = 8000
    override val rateLimitDelayMs: Long = 3000L
    
    // ==================== Private Helpers ====================
    
    private fun buildPrompt(
        text: String,
        sourceLang: String,
        targetLang: String,
        context: TranslationContext
    ): String {
        // Minimal prompt to reduce token usage
        return "Translate $sourceLang to $targetLang. Keep ---PARAGRAPH_BREAK--- markers. Output only translation:\n\n$text"
    }
    
    private fun buildRequestBody(prompt: String): String {
        return """
            {
                "model": "deepseek-chat",
                "messages": [
                    {"role": "system", "content": "Translator."},
                    {"role": "user", "content": ${prompt.escapeJson()}}
                ],
                "temperature": 0.1,
                "max_tokens": 2000
            }
        """.trimIndent()
    }
    
    private fun parseResponse(body: String): String {
        // Simple JSON parsing - extract content from choices[0].message.content
        val contentStart = body.indexOf("\"content\":")
        if (contentStart == -1) {
            throw Exception("Invalid response format")
        }
        
        val valueStart = body.indexOf("\"", contentStart + 10) + 1
        val valueEnd = findJsonStringEnd(body, valueStart)
        
        return body.substring(valueStart, valueEnd).unescapeJson()
    }
    
    private fun findJsonStringEnd(json: String, start: Int): Int {
        var i = start
        while (i < json.length) {
            if (json[i] == '\\') {
                i += 2
                continue
            }
            if (json[i] == '"') {
                return i
            }
            i++
        }
        return json.length
    }
    
    private fun String.escapeJson(): String {
        return "\"" + this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\""
    }
    
    private fun String.unescapeJson(): String {
        return this
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
    
    private fun getLanguageName(code: String): String {
        return SUPPORTED_LANGUAGES.find { it.first == code }?.second ?: code
    }
    
    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            "auto" to "Auto-detect",
            "af" to "Afrikaans",
            "ar" to "Arabic",
            "bg" to "Bulgarian",
            "zh" to "Chinese",
            "hr" to "Croatian",
            "cs" to "Czech",
            "da" to "Danish",
            "nl" to "Dutch",
            "en" to "English",
            "et" to "Estonian",
            "fi" to "Finnish",
            "fr" to "French",
            "de" to "German",
            "el" to "Greek",
            "he" to "Hebrew",
            "hi" to "Hindi",
            "hu" to "Hungarian",
            "id" to "Indonesian",
            "it" to "Italian",
            "ja" to "Japanese",
            "ko" to "Korean",
            "lv" to "Latvian",
            "lt" to "Lithuanian",
            "ms" to "Malay",
            "no" to "Norwegian",
            "fa" to "Persian",
            "pl" to "Polish",
            "pt" to "Portuguese",
            "ro" to "Romanian",
            "ru" to "Russian",
            "sk" to "Slovak",
            "sl" to "Slovenian",
            "es" to "Spanish",
            "sv" to "Swedish",
            "th" to "Thai",
            "tr" to "Turkish",
            "uk" to "Ukrainian",
            "vi" to "Vietnamese"
        )
    }
}
