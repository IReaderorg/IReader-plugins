package io.github.ireaderorg.plugins.openaitranslate

import ireader.plugin.api.*

/**
 * OpenAI GPT Translation Plugin
 * 
 * Uses OpenAI's chat API for high-quality AI-powered translation.
 * Supports context-aware translation with style preservation.
 */
class OpenAITranslatePlugin : TranslationPlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.openai-translate",
        name = "OpenAI Translation",
        version = "1.0.0",
        versionCode = 1,
        description = "AI-powered translation using OpenAI GPT models. High-quality context-aware translation with style preservation.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.TRANSLATION,
        permissions = listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP),
        metadata = mapOf(
            "translation.apiUrl" to "https://api.openai.com/v1/chat/completions",
            "translation.apiType" to "OPENAI_COMPATIBLE",
            "translation.requiresApiKey" to "true",
            "translation.apiKeyHeader" to "Authorization",
            "translation.apiKeyPrefix" to "Bearer ",
            "translation.maxCharsPerRequest" to "6000",
            "translation.rateLimitDelayMs" to "3000",
            "translation.supportsAI" to "true",
            "translation.supportsContextAware" to "true",
            "translation.supportsStylePreservation" to "true",
            "translation.model" to "gpt-3.5-turbo",
            "translation.temperature" to "0.3",
            "translation.maxTokens" to "4000"
        )
    )
    
    private var context: PluginContext? = null
    private var apiKey: String = ""
    
    override fun initialize(context: PluginContext) {
        this.context = context
        apiKey = context.preferences.getString("api_key", "")
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
        if (apiKey.isBlank()) {
            return Result.failure(Exception("OpenAI API key not configured. Please set it in plugin settings."))
        }
        
        val httpClient = this.context?.httpClient
            ?: return Result.failure(Exception("HTTP client not available"))
        
        val sourceLang = if (from == "auto") "the source language" else getLanguageName(from)
        val targetLang = getLanguageName(to)
        
        val prompt = buildPrompt(text, sourceLang, targetLang, context)
        
        return try {
            val requestBody = buildRequestBody(prompt)
            
            val response = httpClient.post(
                url = "https://api.openai.com/v1/chat/completions",
                body = requestBody,
                headers = mapOf(
                    "Authorization" to "Bearer $apiKey",
                    "Content-Type" to "application/json"
                )
            )
            
            when (response.statusCode) {
                401 -> return Result.failure(Exception("Invalid API key"))
                402, 429 -> return Result.failure(Exception("Quota exceeded or rate limited"))
                !in 200..299 -> return Result.failure(Exception("API error: ${response.statusCode}"))
            }
            
            val translatedText = parseResponse(response.body)
            Result.success(translatedText)
        } catch (e: Exception) {
            Result.failure(Exception("Translation failed: ${e.message}"))
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
    
    override fun requiresApiKey(): Boolean = true
    
    override fun configureApiKey(key: String) {
        apiKey = key
        context?.preferences?.putString("api_key", key)
    }
    
    override fun getApiKey(): String? = apiKey.ifBlank { null }
    
    override val supportsAI: Boolean = true
    override val supportsContextAwareTranslation: Boolean = true
    override val supportsStylePreservation: Boolean = true
    override val isOffline: Boolean = false
    override val maxCharsPerRequest: Int = 6000
    override val rateLimitDelayMs: Long = 3000L
    
    private fun buildPrompt(
        text: String,
        sourceLang: String,
        targetLang: String,
        context: TranslationContext
    ): String {
        val contentInstruction = when (context.contentType) {
            TranslationContentType.LITERARY -> "Literary text, preserve style and metaphors."
            TranslationContentType.TECHNICAL -> "Technical text, maintain precise terminology."
            TranslationContentType.CONVERSATION -> "Conversational, keep natural flow."
            TranslationContentType.POETRY -> "Poetry, preserve rhythm and poetic devices."
            else -> ""
        }
        
        val toneInstruction = when (context.toneType) {
            TranslationToneType.FORMAL -> "Use formal language."
            TranslationToneType.CASUAL -> "Use casual language."
            else -> ""
        }
        
        val instructions = listOfNotNull(
            contentInstruction.takeIf { it.isNotEmpty() },
            toneInstruction.takeIf { it.isNotEmpty() }
        ).joinToString(" ")
        
        return if (instructions.isNotEmpty()) {
            "Translate $sourceLang to $targetLang. $instructions Keep ---PARAGRAPH_BREAK--- markers. Output only translation:\n\n$text"
        } else {
            "Translate $sourceLang to $targetLang. Keep ---PARAGRAPH_BREAK--- markers. Output only translation:\n\n$text"
        }
    }
    
    private fun buildRequestBody(prompt: String): String {
        return """
            {
                "model": "gpt-3.5-turbo",
                "messages": [
                    {"role": "system", "content": "You are a professional translator."},
                    {"role": "user", "content": ${prompt.escapeJson()}}
                ],
                "temperature": 0.3,
                "max_tokens": 4000
            }
        """.trimIndent()
    }
    
    private fun parseResponse(body: String): String {
        val contentStart = body.indexOf("\"content\":")
        if (contentStart == -1) throw Exception("Invalid response format")
        
        val valueStart = body.indexOf("\"", contentStart + 10) + 1
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
