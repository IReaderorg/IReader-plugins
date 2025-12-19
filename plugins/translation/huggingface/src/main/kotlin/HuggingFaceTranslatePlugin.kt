package io.github.ireaderorg.plugins.huggingfacetranslate

import ireader.plugin.api.*

/**
 * Hugging Face Translation Plugin
 * 
 * Free AI translation using Hugging Face Inference API with Helsinki-NLP models.
 * No API key required for basic usage.
 */
class HuggingFaceTranslatePlugin : TranslationPlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.huggingface-translate",
        name = "Hugging Face Translation",
        version = "1.0.0",
        versionCode = 1,
        description = "Free AI translation using Hugging Face Inference API with Helsinki-NLP models. No API key required.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.TRANSLATION,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP),
        metadata = mapOf(
            "translation.apiType" to "REST_JSON",
            "translation.requiresApiKey" to "false",
            "translation.maxCharsPerRequest" to "1000",
            "translation.rateLimitDelayMs" to "2000",
            "translation.supportsAI" to "true"
        )
    )
    
    private var context: PluginContext? = null
    private val baseUrl = "https://api-inference.huggingface.co/models/Helsinki-NLP/opus-mt"
    
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
        
        // Handle auto-detection (default to English)
        val sourceCode = if (from == "auto") "en" else from
        
        // Skip if same language
        if (sourceCode == to) return Result.success(text)
        
        // Check if language pair is supported
        if (!isLanguagePairSupported(sourceCode, to)) {
            return Result.failure(Exception("Language pair $sourceCode -> $to not supported"))
        }
        
        return try {
            val modelUrl = "$baseUrl-$sourceCode-$to"
            val requestBody = """{"inputs": ${text.escapeJson()}}"""
            
            val response = httpClient.post(
                url = modelUrl,
                body = requestBody,
                headers = mapOf("Content-Type" to "application/json")
            )
            
            if (response.statusCode == 429) {
                return Result.failure(Exception("Rate limit exceeded. Please wait and try again."))
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
        
        // Translate each text individually (HF API doesn't support batch well)
        val results = mutableListOf<String>()
        for (text in texts) {
            val result = translate(text, from, to)
            if (result.isFailure) {
                // On failure, keep original text
                results.add(text)
            } else {
                results.add(result.getOrThrow())
            }
        }
        return Result.success(results)
    }
    
    override fun getSupportedLanguages(): List<LanguagePair> {
        // Helsinki-NLP models support specific language pairs
        return SUPPORTED_PAIRS.map { LanguagePair(it.first, it.second) }
    }
    
    override fun getAvailableLanguages(): List<Pair<String, String>> = SUPPORTED_LANGUAGES
    
    override fun requiresApiKey(): Boolean = false
    
    override fun configureApiKey(key: String) {
        // Not required
    }
    
    override val supportsAI: Boolean = true
    override val supportsContextAwareTranslation: Boolean = false
    override val isOffline: Boolean = false
    override val maxCharsPerRequest: Int = 1000
    override val rateLimitDelayMs: Long = 2000L
    
    private fun isLanguagePairSupported(from: String, to: String): Boolean {
        return SUPPORTED_PAIRS.any { it.first == from && it.second == to }
    }
    
    private fun parseResponse(body: String): String {
        // Response is array: [{"translation_text": "..."}] or [{"generated_text": "..."}]
        val translationKey = "\"translation_text\":"
        val generatedKey = "\"generated_text\":"
        
        var start = body.indexOf(translationKey)
        var keyLen = translationKey.length
        
        if (start == -1) {
            start = body.indexOf(generatedKey)
            keyLen = generatedKey.length
        }
        
        if (start == -1) throw Exception("Invalid response format")
        
        val valueStart = body.indexOf("\"", start + keyLen) + 1
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
            "en" to "English",
            "zh" to "Chinese",
            "es" to "Spanish",
            "fr" to "French",
            "de" to "German",
            "ja" to "Japanese",
            "ko" to "Korean",
            "pt" to "Portuguese",
            "ru" to "Russian",
            "it" to "Italian"
        )
        
        // Helsinki-NLP supported language pairs
        val SUPPORTED_PAIRS = listOf(
            // English to others
            "en" to "zh", "en" to "es", "en" to "fr", "en" to "de",
            "en" to "ja", "en" to "ko", "en" to "pt", "en" to "ru", "en" to "it",
            // Others to English
            "zh" to "en", "es" to "en", "fr" to "en", "de" to "en",
            "ja" to "en", "ko" to "en", "pt" to "en", "ru" to "en", "it" to "en",
            // Some cross-language pairs
            "zh" to "ja", "ja" to "zh", "es" to "pt", "pt" to "es",
            "fr" to "de", "de" to "fr", "es" to "fr", "fr" to "es"
        )
    }
}
