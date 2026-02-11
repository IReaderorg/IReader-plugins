package io.github.ireaderorg.plugins.huggingfacetranslate

import ireader.plugin.api.*

/**
 * Hugging Face Translation Plugin
 * 
 * AI translation using Hugging Face Inference API with Helsinki-NLP models.
 * API key recommended for reliable access (free tier available at huggingface.co/settings/tokens).
 */
class HuggingFaceTranslatePlugin : TranslationPlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.huggingface-translate",
        name = "Hugging Face Translation",
        version = "1.1.0",
        versionCode = 2,
        description = "AI translation using Hugging Face Inference API with Helsinki-NLP models. API key recommended for reliable access.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.TRANSLATION,
        permissions = listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES),
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
    private var apiKey: String = ""
    private val baseUrl = "https://router.huggingface.co/hf-inference/models"
    
    override fun initialize(context: PluginContext) {
        this.context = context
        // Load API key from preferences
        apiKey = context.preferences.getString("api_key", "")
    }
    
    override fun cleanup() {
        context = null
    }
    
    override suspend fun translate(text: String, from: String, to: String): Result<String> {
        println("HuggingFaceTranslatePlugin: translate() called")
        println("HuggingFaceTranslatePlugin: from=$from, to=$to, text length=${text.length}")
        println("HuggingFaceTranslatePlugin: text preview: ${text.take(100)}...")
        
        if (text.isBlank()) {
            println("HuggingFaceTranslatePlugin: Text is blank, returning as-is")
            return Result.success(text)
        }
        
        val httpClient = context?.httpClient
        if (httpClient == null) {
            println("HuggingFaceTranslatePlugin: ERROR - HTTP client not available")
            return Result.failure(Exception("HTTP client not available"))
        }
        
        // Handle auto-detection (default to English)
        val sourceCode = if (from == "auto") "en" else from
        println("HuggingFaceTranslatePlugin: sourceCode=$sourceCode (after auto-detection)")
        
        // Skip if same language
        if (sourceCode == to) {
            println("HuggingFaceTranslatePlugin: Source and target are same, returning as-is")
            return Result.success(text)
        }
        
        // Check if language pair is supported
        if (!isLanguagePairSupported(sourceCode, to)) {
            println("HuggingFaceTranslatePlugin: ERROR - Language pair $sourceCode -> $to not supported")
            return Result.failure(Exception("Language pair $sourceCode -> $to not supported"))
        }
        
        return try {
            val modelId = getModelId(sourceCode, to)
            val modelUrl = "$baseUrl/$modelId"
            val requestBody = """{"inputs": ${text.escapeJson()}}"""
            
            println("HuggingFaceTranslatePlugin: Model ID: $modelId")
            println("HuggingFaceTranslatePlugin: URL: $modelUrl")
            println("HuggingFaceTranslatePlugin: Using API key: ${apiKey.isNotBlank()}")
            
            val headers = mutableMapOf("Content-Type" to "application/json")
            if (apiKey.isNotBlank()) {
                headers["Authorization"] = "Bearer $apiKey"
            }
            
            println("HuggingFaceTranslatePlugin: Sending request...")
            val response = httpClient.post(
                url = modelUrl,
                body = requestBody,
                headers = headers
            )
            
            println("HuggingFaceTranslatePlugin: Response status: ${response.statusCode}")
            println("HuggingFaceTranslatePlugin: Response body preview: ${response.body.take(500)}")
            
            if (response.statusCode == 401) {
                println("HuggingFaceTranslatePlugin: ERROR 401 - Invalid API key")
                return Result.failure(Exception("Invalid API key. Please check your Hugging Face API key."))
            }
            if (response.statusCode == 403) {
                println("HuggingFaceTranslatePlugin: ERROR 403 - Access forbidden")
                return Result.failure(Exception("Access forbidden. You may need to accept the model terms on Hugging Face or add an API key."))
            }
            if (response.statusCode == 429) {
                println("HuggingFaceTranslatePlugin: ERROR 429 - Rate limit exceeded")
                return Result.failure(Exception("Rate limit exceeded. Please add a free Hugging Face API key for higher limits."))
            }
            if (response.statusCode == 503) {
                println("HuggingFaceTranslatePlugin: ERROR 503 - Model loading")
                return Result.failure(Exception("Model is loading. Please try again in a few moments."))
            }
            if (response.statusCode !in 200..299) {
                println("HuggingFaceTranslatePlugin: ERROR ${response.statusCode} - API error")
                return Result.failure(Exception("API error: ${response.statusCode} - ${response.body.take(200)}"))
            }
            
            val translatedText = parseResponse(response.body)
            println("HuggingFaceTranslatePlugin: SUCCESS - Translated text length: ${translatedText.length}")
            println("HuggingFaceTranslatePlugin: Translated text preview: ${translatedText.take(100)}...")
            Result.success(translatedText)
        } catch (e: Exception) {
            println("HuggingFaceTranslatePlugin: EXCEPTION - ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Translation failed: ${e.message}"))
        }
    }
    
    /**
     * Get the model ID for a language pair.
     * Maps language codes to the correct Helsinki-NLP model names.
     */
    private fun getModelId(from: String, to: String): String {
        // Special cases for models with different naming conventions
        val pairKey = "$from-$to"
        return when (pairKey) {
            "en-zh" -> "Helsinki-NLP/opus-mt-en-zh"
            "zh-en" -> "Helsinki-NLP/opus-mt-zh-en"
            "en-ja" -> "Helsinki-NLP/opus-mt-en-ja"
            "ja-en" -> "Helsinki-NLP/opus-mt-ja-en"
            "en-ko" -> "Helsinki-NLP/opus-mt-en-ko"
            "ko-en" -> "Helsinki-NLP/opus-mt-ko-en"
            else -> "Helsinki-NLP/opus-mt-$from-$to"
        }
    }
    
    override suspend fun translateBatch(texts: List<String>, from: String, to: String): Result<List<String>> {
        if (texts.isEmpty()) return Result.success(emptyList())
        
        // Translate each text individually (HF API doesn't support batch well)
        val results = mutableListOf<String>()
        for ((index, text) in texts.withIndex()) {
            val result = translate(text, from, to)
            if (result.isFailure) {
                // Propagate the error instead of silently returning original text
                val error = result.exceptionOrNull() ?: Exception("Unknown translation error")
                println("HuggingFaceTranslatePlugin: Translation failed for text[$index]: ${error.message}")
                return Result.failure(Exception("Failed to translate text ${index + 1}/${texts.size}: ${error.message}"))
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
        apiKey = key
        context?.preferences?.putString("api_key", key)
    }
    
    override fun getApiKey(): String? = apiKey.ifBlank { null }
    
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
