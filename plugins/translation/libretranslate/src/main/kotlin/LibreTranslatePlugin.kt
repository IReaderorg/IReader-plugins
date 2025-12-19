package io.github.ireaderorg.plugins.libretranslate

import ireader.plugin.api.*

/**
 * LibreTranslate Plugin
 * 
 * Free and open-source translation using LibreTranslate API.
 * No API key required.
 */
class LibreTranslatePlugin : TranslationPlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.libretranslate",
        name = "LibreTranslate",
        version = "1.0.0",
        versionCode = 1,
        description = "Free and open-source translation using LibreTranslate API. No API key required.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.TRANSLATION,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP),
        metadata = mapOf(
            "translation.apiUrl" to "https://lt.neat.computer/translate",
            "translation.apiType" to "REST_JSON",
            "translation.requiresApiKey" to "false",
            "translation.maxCharsPerRequest" to "5000",
            "translation.rateLimitDelayMs" to "1000",
            "translation.supportsAI" to "false"
        )
    )
    
    private var context: PluginContext? = null
    private val apiUrl = "https://lt.neat.computer/translate"
    
    override fun initialize(context: PluginContext) {
        this.context = context
    }
    
    override fun cleanup() {
        context = null
    }
    
    override suspend fun translate(text: String, from: String, to: String): Result<String> {
        if (text.isBlank()) return Result.success(text)
        
        val httpClient = context?.httpClient
            ?: return Result.failure(Exception("HTTP client not available"))
        
        val sourceCode = if (from == "auto") "auto" else from
        
        return try {
            val requestBody = """
                {
                    "q": ${text.escapeJson()},
                    "source": "$sourceCode",
                    "target": "$to",
                    "format": "text"
                }
            """.trimIndent()
            
            val response = httpClient.post(
                url = apiUrl,
                body = requestBody,
                headers = mapOf("Content-Type" to "application/json")
            )
            
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
        if (texts.isEmpty()) return Result.success(emptyList())
        
        // Combine with markers for batch translation
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
        // Not required
    }
    
    override val supportsAI: Boolean = false
    override val supportsContextAwareTranslation: Boolean = false
    override val isOffline: Boolean = false
    override val maxCharsPerRequest: Int = 5000
    override val rateLimitDelayMs: Long = 1000L
    
    private fun parseResponse(body: String): String {
        // Parse {"translatedText": "..."}
        val key = "\"translatedText\":"
        val start = body.indexOf(key)
        if (start == -1) throw Exception("Invalid response format")
        
        val valueStart = body.indexOf("\"", start + key.length) + 1
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
    
    companion object {
        val SUPPORTED_LANGUAGES = listOf(
            "auto" to "Auto-detect",
            "ar" to "Arabic",
            "az" to "Azerbaijani",
            "cs" to "Czech",
            "da" to "Danish",
            "de" to "German",
            "el" to "Greek",
            "en" to "English",
            "eo" to "Esperanto",
            "es" to "Spanish",
            "fa" to "Persian",
            "fi" to "Finnish",
            "fr" to "French",
            "ga" to "Irish",
            "he" to "Hebrew",
            "hi" to "Hindi",
            "hu" to "Hungarian",
            "id" to "Indonesian",
            "it" to "Italian",
            "ja" to "Japanese",
            "ko" to "Korean",
            "nl" to "Dutch",
            "pl" to "Polish",
            "pt" to "Portuguese",
            "ru" to "Russian",
            "sk" to "Slovak",
            "sv" to "Swedish",
            "th" to "Thai",
            "tr" to "Turkish",
            "uk" to "Ukrainian",
            "vi" to "Vietnamese",
            "zh" to "Chinese"
        )
    }
}
