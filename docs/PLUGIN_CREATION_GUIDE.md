# IReader Plugin Creation Guide

A comprehensive step-by-step guide for creating plugins for IReader.

## Table of Contents

1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
3. [Understanding the Plugin System](#understanding-the-plugin-system)
4. [Creating Your First Plugin](#creating-your-first-plugin)
5. [Plugin Types Deep Dive](#plugin-types-deep-dive)
6. [Working with Plugin APIs](#working-with-plugin-apis)
7. [Building and Testing](#building-and-testing)
8. [Publishing Your Plugin](#publishing-your-plugin)
9. [Best Practices](#best-practices)
10. [Troubleshooting](#troubleshooting)

---

## Introduction

IReader plugins allow you to extend the functionality of the IReader app without modifying its core codebase. This guide will walk you through creating plugins from scratch.

### What You'll Learn

- How the plugin system works
- How to create each type of plugin
- How to use plugin APIs
- How to test and publish your plugins

---

## Prerequisites

### Required Software

- **JDK 17+** - Java Development Kit
- **Gradle 8.10+** - Build system (included via wrapper)
- **IDE** - IntelliJ IDEA or Android Studio recommended

### Required Knowledge

- Kotlin programming language
- Basic understanding of Gradle
- Familiarity with coroutines (for async plugins)

### Setup

```bash
# Clone the repository
git clone https://github.com/IReaderorg/IReader-plugins.git
cd IReader-plugins

# Verify setup
./gradlew --version
```

---

## Understanding the Plugin System

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      IReader App                             │
├─────────────────────────────────────────────────────────────┤
│                    Plugin Manager                            │
│  • Discovers .iplugin files                                  │
│  • Validates manifests and permissions                       │
│  • Manages plugin lifecycle                                  │
│  • Enforces security sandbox                                 │
├─────────────────────────────────────────────────────────────┤
│                      Plugin API                              │
│  • Plugin interfaces (ThemePlugin, TTSPlugin, etc.)          │
│  • Utility classes (HTTP client, JSON helper)                │
│  • Context and preferences                                   │
├─────────────────────────────────────────────────────────────┤
│                    Your Plugins                              │
│  ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐              │
│  │Theme │ │ TTS  │ │Trans │ │Feat. │ │  AI  │              │
│  └──────┘ └──────┘ └──────┘ └──────┘ └──────┘              │
└─────────────────────────────────────────────────────────────┘
```

### Plugin Lifecycle

1. **Discovery** - IReader scans for `.iplugin` files
2. **Loading** - Plugin manifest (`plugin.json`) is read and validated
3. **Registration** - Plugin is added to the registry
4. **Initialization** - `initialize(context)` is called when enabled
5. **Execution** - Plugin methods are called as needed
6. **Cleanup** - `cleanup()` is called when disabled or app closes

### Key Components

| Component | Description |
|-----------|-------------|
| `@IReaderPlugin` | Annotation marking the plugin entry point |
| `@PluginMetadata` | Annotation with plugin metadata |
| `PluginManifest` | Runtime manifest object |
| `PluginContext` | Access to IReader APIs and preferences |
| `PluginConfig` | Gradle DSL for build configuration |

---

## Creating Your First Plugin

Let's create a simple Feature plugin step by step.

### Step 1: Create Directory Structure

```bash
mkdir -p plugins/features/hello-world/src/main/kotlin
```

Your structure should look like:
```
plugins/
└── features/
    └── hello-world/
        ├── build.gradle.kts
        └── src/
            └── main/
                └── kotlin/
                    └── HelloWorldPlugin.kt
```

### Step 2: Create build.gradle.kts

```kotlin
// plugins/features/hello-world/build.gradle.kts
plugins {
    id("ireader-plugin")
}

pluginConfig {
    // Unique identifier (reverse domain notation)
    id.set("com.yourname.hello-world")
    
    // Display name
    name.set("Hello World")
    
    // Semantic version
    version.set("1.0.0")
    
    // Integer version code (increment with each release)
    versionCode.set(1)
    
    // Short description
    description.set("A simple hello world plugin")
    
    // Your name or organization
    author.set("Your Name")
    
    // Plugin type
    type.set(PluginType.FEATURE)
    
    // Required permissions (empty for this simple plugin)
    permissions.set(emptyList())
}
```

### Step 3: Implement the Plugin

```kotlin
// plugins/features/hello-world/src/main/kotlin/HelloWorldPlugin.kt
package com.yourname.helloworld

import ireader.plugin.api.*
import ireader.plugin.annotations.*

@IReaderPlugin
@PluginMetadata(
    id = "com.yourname.hello-world",
    name = "Hello World",
    version = "1.0.0",
    versionCode = 1,
    description = "A simple hello world plugin",
    author = "Your Name"
)
class HelloWorldPlugin : FeaturePlugin {
    
    // Runtime manifest (must match build.gradle.kts)
    override val manifest = PluginManifest(
        id = "com.yourname.hello-world",
        name = "Hello World",
        version = "1.0.0",
        versionCode = 1,
        description = "A simple hello world plugin",
        author = PluginAuthor("Your Name"),
        type = PluginType.FEATURE,
        permissions = emptyList(),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP)
    )
    
    private var context: PluginContext? = null
    
    override fun initialize(context: PluginContext) {
        this.context = context
        // Plugin is now active
    }
    
    override fun cleanup() {
        // Release any resources
        context = null
    }
    
    override fun getMenuItems(): List<PluginMenuItem> {
        return listOf(
            PluginMenuItem(
                id = "say_hello",
                label = "Say Hello",
                icon = "waving_hand"
            )
        )
    }
    
    override fun getScreens(): List<PluginScreen> {
        return emptyList()
    }
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        // Called when reader state changes
        return null
    }
}
```

### Step 4: Build the Plugin

```bash
./gradlew :plugins:features:hello-world:assemble
```

The built plugin will be at:
`plugins/features/hello-world/build/outputs/hello-world-1.0.0.iplugin`

### Step 5: Test the Plugin

1. Copy the `.iplugin` file to your device
2. In IReader, go to **Community Hub → Install from File**
3. Select your plugin file
4. Enable the plugin in settings

---

## Plugin Types Deep Dive

### Theme Plugin

Theme plugins customize the visual appearance of IReader.

#### Interface

```kotlin
interface ThemePlugin : Plugin {
    fun getColorScheme(isDark: Boolean): ThemeColorScheme
    fun getExtraColors(isDark: Boolean): ThemeExtraColors
    fun getTypography(): ThemeTypography?
    fun getBackgroundAssets(): ThemeBackgrounds?
}
```

#### Complete Example

```kotlin
package com.yourname.mytheme

import ireader.plugin.api.*
import ireader.plugin.annotations.*

@IReaderPlugin
@PluginMetadata(
    id = "com.yourname.my-theme",
    name = "My Custom Theme",
    version = "1.0.0",
    versionCode = 1,
    description = "A beautiful custom theme",
    author = "Your Name"
)
class MyTheme : ThemePlugin {
    
    override val manifest = PluginManifest(
        id = "com.yourname.my-theme",
        name = "My Custom Theme",
        version = "1.0.0",
        versionCode = 1,
        description = "A beautiful custom theme",
        author = PluginAuthor("Your Name"),
        type = PluginType.THEME,
        permissions = emptyList(),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    override fun initialize(context: PluginContext) {}
    override fun cleanup() {}
    
    override fun getColorScheme(isDark: Boolean): ThemeColorScheme {
        return if (isDark) darkColors else lightColors
    }
    
    override fun getExtraColors(isDark: Boolean): ThemeExtraColors {
        return ThemeExtraColors(
            bars = if (isDark) 0xFF1A1A2E else 0xFF4A90D9,
            onBars = 0xFFFFFFFF,
            isBarLight = false
        )
    }
    
    override fun getTypography(): ThemeTypography? = null
    override fun getBackgroundAssets(): ThemeBackgrounds? = null
    
    private val lightColors = ThemeColorScheme(
        primary = 0xFF4A90D9,
        onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFD6E4FF,
        onPrimaryContainer = 0xFF001B3D,
        secondary = 0xFF6B5778,
        onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFF2DAFF,
        onSecondaryContainer = 0xFF251431,
        tertiary = 0xFF7D5260,
        onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFFFD8E4,
        onTertiaryContainer = 0xFF31101D,
        error = 0xFFBA1A1A,
        onError = 0xFFFFFFFF,
        errorContainer = 0xFFFFDAD6,
        onErrorContainer = 0xFF410002,
        background = 0xFFFEFBFF,
        onBackground = 0xFF1B1B1F,
        surface = 0xFFFEFBFF,
        onSurface = 0xFF1B1B1F,
        surfaceVariant = 0xFFE1E2EC,
        onSurfaceVariant = 0xFF44474F,
        outline = 0xFF74777F,
        outlineVariant = 0xFFC4C6D0,
        scrim = 0xFF000000,
        inverseSurface = 0xFF303034,
        inverseOnSurface = 0xFFF2F0F4,
        inversePrimary = 0xFFAAC7FF
    )
    
    private val darkColors = ThemeColorScheme(
        primary = 0xFFAAC7FF,
        onPrimary = 0xFF002F64,
        primaryContainer = 0xFF00458D,
        onPrimaryContainer = 0xFFD6E4FF,
        secondary = 0xFFD6BEE4,
        onSecondary = 0xFF3B2948,
        secondaryContainer = 0xFF523F5F,
        onSecondaryContainer = 0xFFF2DAFF,
        tertiary = 0xFFEFB8C8,
        onTertiary = 0xFF492532,
        tertiaryContainer = 0xFF633B48,
        onTertiaryContainer = 0xFFFFD8E4,
        error = 0xFFFFB4AB,
        onError = 0xFF690005,
        errorContainer = 0xFF93000A,
        onErrorContainer = 0xFFFFDAD6,
        background = 0xFF1B1B1F,
        onBackground = 0xFFE3E2E6,
        surface = 0xFF1B1B1F,
        onSurface = 0xFFE3E2E6,
        surfaceVariant = 0xFF44474F,
        onSurfaceVariant = 0xFFC4C6D0,
        outline = 0xFF8E9099,
        outlineVariant = 0xFF44474F,
        scrim = 0xFF000000,
        inverseSurface = 0xFFE3E2E6,
        inverseOnSurface = 0xFF303034,
        inversePrimary = 0xFF005CB7
    )
}
```

#### build.gradle.kts for Theme

```kotlin
plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("com.yourname.my-theme")
    name.set("My Custom Theme")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("A beautiful custom theme")
    author.set("Your Name")
    type.set(PluginType.THEME)
    permissions.set(emptyList())
}
```

---

### TTS Plugin

TTS plugins add text-to-speech capabilities.

#### Interface

```kotlin
interface TTSPlugin : Plugin {
    suspend fun speak(text: String, voice: VoiceConfig): Result<AudioStream>
    fun getAvailableVoices(): List<VoiceModel>
    fun supportsStreaming(): Boolean
    fun getAudioFormat(): AudioFormat
}
```

#### Complete Example

```kotlin
package com.yourname.mytts

import ireader.plugin.api.*
import ireader.plugin.annotations.*

@IReaderPlugin
@PluginMetadata(
    id = "com.yourname.my-tts",
    name = "My TTS Engine",
    version = "1.0.0",
    versionCode = 1,
    description = "Custom TTS engine",
    author = "Your Name"
)
class MyTTS : TTSPlugin {
    
    override val manifest = PluginManifest(
        id = "com.yourname.my-tts",
        name = "My TTS Engine",
        version = "1.0.0",
        versionCode = 1,
        description = "Custom TTS engine",
        author = PluginAuthor("Your Name"),
        type = PluginType.TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP)
    )
    
    private var context: PluginContext? = null
    
    override fun initialize(context: PluginContext) {
        this.context = context
    }
    
    override fun cleanup() {
        context = null
    }
    
    override suspend fun speak(text: String, voice: VoiceConfig): Result<AudioStream> {
        val httpClient = context?.httpClient
            ?: return Result.failure(Exception("HTTP client not available"))
        
        return try {
            // Call your TTS API
            val response = httpClient.post(
                url = "https://your-tts-api.com/synthesize",
                body = """{"text": "$text", "voice": "${voice.voiceId}"}""",
                headers = mapOf("Content-Type" to "application/json")
            )
            
            if (response.statusCode == 200) {
                Result.success(AudioStream(
                    data = response.bodyBytes,
                    format = getAudioFormat()
                ))
            } else {
                Result.failure(Exception("TTS API error: ${response.statusCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getAvailableVoices(): List<VoiceModel> {
        return listOf(
            VoiceModel(
                id = "en-us-female",
                name = "Sarah (English US)",
                language = "en-US",
                gender = VoiceGender.FEMALE
            ),
            VoiceModel(
                id = "en-us-male",
                name = "John (English US)",
                language = "en-US",
                gender = VoiceGender.MALE
            ),
            VoiceModel(
                id = "en-gb-female",
                name = "Emma (English UK)",
                language = "en-GB",
                gender = VoiceGender.FEMALE
            )
        )
    }
    
    override fun supportsStreaming(): Boolean = false
    
    override fun getAudioFormat(): AudioFormat {
        return AudioFormat(
            encoding = AudioEncoding.MP3,
            sampleRate = 22050,
            channels = 1,
            bitDepth = 16
        )
    }
}
```

---

### Translation Plugin

Translation plugins enable reading in different languages.

#### Interface

```kotlin
interface TranslationPlugin : Plugin {
    suspend fun translate(text: String, from: String, to: String): Result<String>
    suspend fun translateBatch(texts: List<String>, from: String, to: String): Result<List<String>>
    fun getSupportedLanguages(): List<LanguagePair>
    fun getAvailableLanguages(): List<Pair<String, String>>
    fun requiresApiKey(): Boolean
    fun configureApiKey(key: String)
    fun getApiKey(): String?
    
    // Optional capabilities
    val supportsAI: Boolean
    val supportsContextAwareTranslation: Boolean
    val supportsStylePreservation: Boolean
    val isOffline: Boolean
    val maxCharsPerRequest: Int
    val rateLimitDelayMs: Long
}
```

#### Complete Example

```kotlin
package com.yourname.mytranslation

import ireader.plugin.api.*
import ireader.plugin.annotations.*

@IReaderPlugin
@PluginMetadata(
    id = "com.yourname.my-translation",
    name = "My Translation Service",
    version = "1.0.0",
    versionCode = 1,
    description = "Custom translation service",
    author = "Your Name"
)
class MyTranslation : TranslationPlugin {
    
    override val manifest = PluginManifest(
        id = "com.yourname.my-translation",
        name = "My Translation Service",
        version = "1.0.0",
        versionCode = 1,
        description = "Custom translation service",
        author = PluginAuthor("Your Name"),
        type = PluginType.TRANSLATION,
        permissions = listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP)
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
        if (apiKey.isBlank()) {
            return Result.failure(Exception("API key not configured"))
        }
        
        val httpClient = context?.httpClient
            ?: return Result.failure(Exception("HTTP client not available"))
        
        return try {
            val response = httpClient.post(
                url = "https://your-translation-api.com/translate",
                body = """{"text": "$text", "source": "$from", "target": "$to"}""",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer $apiKey"
                )
            )
            
            if (response.statusCode == 200) {
                // Parse response and extract translated text
                val translatedText = parseTranslationResponse(response.body)
                Result.success(translatedText)
            } else {
                Result.failure(Exception("Translation API error: ${response.statusCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun translateBatch(
        texts: List<String>,
        from: String,
        to: String
    ): Result<List<String>> {
        // Translate each text individually
        val results = mutableListOf<String>()
        for (text in texts) {
            val result = translate(text, from, to)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull()!!)
            }
            results.add(result.getOrThrow())
        }
        return Result.success(results)
    }
    
    override fun getSupportedLanguages(): List<LanguagePair> {
        return listOf(LanguagePair("*", "*")) // Supports all pairs
    }
    
    override fun getAvailableLanguages(): List<Pair<String, String>> {
        return listOf(
            "auto" to "Auto-detect",
            "en" to "English",
            "es" to "Spanish",
            "fr" to "French",
            "de" to "German",
            "ja" to "Japanese",
            "ko" to "Korean",
            "zh" to "Chinese"
        )
    }
    
    override fun requiresApiKey(): Boolean = true
    
    override fun configureApiKey(key: String) {
        apiKey = key
        context?.preferences?.putString("api_key", key)
    }
    
    override fun getApiKey(): String? = apiKey.ifBlank { null }
    
    override val supportsAI: Boolean = false
    override val supportsContextAwareTranslation: Boolean = false
    override val supportsStylePreservation: Boolean = false
    override val isOffline: Boolean = false
    override val maxCharsPerRequest: Int = 5000
    override val rateLimitDelayMs: Long = 1000L
    
    private fun parseTranslationResponse(body: String): String {
        // Simple JSON parsing - in production use kotlinx.serialization
        val start = body.indexOf("\"translated\":\"") + 14
        val end = body.indexOf("\"", start)
        return body.substring(start, end)
    }
}
```

---

### Feature Plugin

Feature plugins add custom functionality to the reader.

#### Interface

```kotlin
interface FeaturePlugin : Plugin {
    fun getMenuItems(): List<PluginMenuItem>
    fun getScreens(): List<PluginScreen>
    fun onReaderContext(context: ReaderContext): PluginAction?
    fun getPreferencesScreen(): PluginScreen?
}
```

#### Complete Example with UI

```kotlin
package com.yourname.wordcounter

import ireader.plugin.api.*
import ireader.plugin.annotations.*

@IReaderPlugin
@PluginMetadata(
    id = "com.yourname.word-counter",
    name = "Word Counter",
    version = "1.0.0",
    versionCode = 1,
    description = "Count words in selected text",
    author = "Your Name"
)
class WordCounterPlugin : FeaturePlugin, PluginUIProvider {
    
    override val manifest = PluginManifest(
        id = "com.yourname.word-counter",
        name = "Word Counter",
        version = "1.0.0",
        versionCode = 1,
        description = "Count words in selected text",
        author = PluginAuthor("Your Name"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.READER_CONTEXT),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP)
    )
    
    private var context: PluginContext? = null
    private var lastSelectedText: String = ""
    
    override fun initialize(context: PluginContext) {
        this.context = context
    }
    
    override fun cleanup() {
        context = null
    }
    
    override fun getMenuItems(): List<PluginMenuItem> {
        return listOf(
            PluginMenuItem(
                id = "count_words",
                label = "Count Words",
                icon = "format_list_numbered"
            )
        )
    }
    
    override fun getScreens(): List<PluginScreen> {
        return listOf(
            PluginScreen(
                route = "plugin/word-counter/main",
                title = "Word Counter",
                content = {}
            )
        )
    }
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        context.selectedText?.let { text ->
            lastSelectedText = text
        }
        return null
    }
    
    // PluginUIProvider implementation
    override fun getScreen(screenId: String, context: PluginScreenContext): PluginUIScreen? {
        val wordCount = lastSelectedText.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
        val charCount = lastSelectedText.length
        val charNoSpaces = lastSelectedText.replace("\\s".toRegex(), "").length
        
        return PluginUIScreen(
            id = "main",
            title = "Word Counter",
            components = listOf(
                PluginUIComponent.Card(listOf(
                    PluginUIComponent.Text("Statistics", TextStyle.TITLE_MEDIUM),
                    PluginUIComponent.Spacer(8),
                    PluginUIComponent.Row(listOf(
                        PluginUIComponent.Column(listOf(
                            PluginUIComponent.Text("$wordCount", TextStyle.TITLE_LARGE),
                            PluginUIComponent.Text("Words", TextStyle.BODY_SMALL)
                        )),
                        PluginUIComponent.Column(listOf(
                            PluginUIComponent.Text("$charCount", TextStyle.TITLE_LARGE),
                            PluginUIComponent.Text("Characters", TextStyle.BODY_SMALL)
                        )),
                        PluginUIComponent.Column(listOf(
                            PluginUIComponent.Text("$charNoSpaces", TextStyle.TITLE_LARGE),
                            PluginUIComponent.Text("No Spaces", TextStyle.BODY_SMALL)
                        ))
                    ), spacing = 24)
                )),
                PluginUIComponent.Spacer(16),
                PluginUIComponent.Card(listOf(
                    PluginUIComponent.Text("Selected Text", TextStyle.TITLE_SMALL),
                    PluginUIComponent.Spacer(8),
                    PluginUIComponent.Text(
                        if (lastSelectedText.length > 200) 
                            lastSelectedText.take(200) + "..." 
                        else 
                            lastSelectedText.ifBlank { "No text selected" },
                        TextStyle.BODY
                    )
                ))
            )
        )
    }
    
    override suspend fun handleEvent(
        screenId: String,
        event: PluginUIEvent,
        context: PluginScreenContext
    ): PluginUIScreen? {
        // Handle UI events if needed
        return getScreen(screenId, context)
    }
}
```

---

### AI Plugin

AI plugins provide intelligent text processing capabilities.

#### Interface

```kotlin
interface AIPlugin : Plugin {
    val aiCapabilities: List<AICapability>
    val providerType: AIProviderType
    val modelInfo: AIModelInfo
    
    suspend fun summarize(text: String, options: SummarizationOptions): AIResult<String>
    suspend fun analyzeCharacters(text: String, options: CharacterAnalysisOptions): AIResult<List<CharacterInfo>>
    suspend fun answerQuestion(context: String, question: String, options: QAOptions): AIResult<QAResponse>
    suspend fun generateText(prompt: String, options: GenerationOptions): AIResult<String>
}
```

#### Complete Example

```kotlin
package com.yourname.aisummarizer

import ireader.plugin.api.*
import ireader.plugin.annotations.*

@IReaderPlugin
@PluginMetadata(
    id = "com.yourname.ai-summarizer",
    name = "AI Summarizer",
    version = "1.0.0",
    versionCode = 1,
    description = "Summarize chapters using AI",
    author = "Your Name"
)
class AISummarizerPlugin : AIPlugin {
    
    override val manifest = PluginManifest(
        id = "com.yourname.ai-summarizer",
        name = "AI Summarizer",
        version = "1.0.0",
        versionCode = 1,
        description = "Summarize chapters using AI",
        author = PluginAuthor("Your Name"),
        type = PluginType.AI,
        permissions = listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP)
    )
    
    private var context: PluginContext? = null
    private var apiKey: String = ""
    
    override val aiCapabilities = listOf(
        AICapability.SUMMARIZATION,
        AICapability.TEXT_GENERATION
    )
    
    override val providerType = AIProviderType.CLOUD
    
    override val modelInfo = AIModelInfo(
        name = "GPT-3.5 Turbo",
        provider = "OpenAI",
        maxTokens = 4096,
        supportsStreaming = false
    )
    
    override fun initialize(context: PluginContext) {
        this.context = context
        apiKey = context.preferences.getString("api_key", "")
    }
    
    override fun cleanup() {
        context = null
    }
    
    override suspend fun summarize(
        text: String,
        options: SummarizationOptions
    ): AIResult<String> {
        if (apiKey.isBlank()) {
            return AIResult.Error(AIError.ConfigurationError("API key not configured"))
        }
        
        val httpClient = context?.httpClient
            ?: return AIResult.Error(AIError.Unknown("HTTP client not available"))
        
        val lengthInstruction = when (options.length) {
            SummaryLength.SHORT -> "in 2-3 sentences"
            SummaryLength.MEDIUM -> "in a paragraph"
            SummaryLength.LONG -> "in detail"
        }
        
        val prompt = "Summarize the following text $lengthInstruction:\n\n$text"
        
        return try {
            val response = httpClient.post(
                url = "https://api.openai.com/v1/chat/completions",
                body = """
                    {
                        "model": "gpt-3.5-turbo",
                        "messages": [
                            {"role": "system", "content": "You are a helpful assistant that summarizes text."},
                            {"role": "user", "content": ${prompt.escapeJson()}}
                        ],
                        "temperature": 0.5,
                        "max_tokens": 1000
                    }
                """.trimIndent(),
                headers = mapOf(
                    "Authorization" to "Bearer $apiKey",
                    "Content-Type" to "application/json"
                )
            )
            
            when (response.statusCode) {
                200 -> {
                    val summary = parseOpenAIResponse(response.body)
                    AIResult.Success(summary)
                }
                401 -> AIResult.Error(AIError.AuthenticationError("Invalid API key"))
                429 -> AIResult.Error(AIError.RateLimitError("Rate limit exceeded"))
                else -> AIResult.Error(AIError.Unknown("API error: ${response.statusCode}"))
            }
        } catch (e: Exception) {
            AIResult.Error(AIError.NetworkError(e.message ?: "Network error"))
        }
    }
    
    override suspend fun analyzeCharacters(
        text: String,
        options: CharacterAnalysisOptions
    ): AIResult<List<CharacterInfo>> {
        return AIResult.Error(AIError.NotSupported("Character analysis not implemented"))
    }
    
    override suspend fun answerQuestion(
        context: String,
        question: String,
        options: QAOptions
    ): AIResult<QAResponse> {
        return AIResult.Error(AIError.NotSupported("Q&A not implemented"))
    }
    
    override suspend fun generateText(
        prompt: String,
        options: GenerationOptions
    ): AIResult<String> {
        // Similar to summarize but with different prompt
        return AIResult.Error(AIError.NotSupported("Text generation not implemented"))
    }
    
    private fun parseOpenAIResponse(body: String): String {
        val contentStart = body.indexOf("\"content\":\"") + 11
        val contentEnd = body.indexOf("\"", contentStart)
        return body.substring(contentStart, contentEnd)
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
    }
    
    private fun String.escapeJson(): String {
        return "\"" + this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\""
    }
}
```

---

## Working with Plugin APIs

### PluginContext

The `PluginContext` provides access to IReader's APIs:

```kotlin
class MyPlugin : FeaturePlugin {
    private lateinit var context: PluginContext
    
    override fun initialize(context: PluginContext) {
        this.context = context
        
        // Access preferences
        val savedValue = context.preferences.getString("key", "default")
        context.preferences.putString("key", "value")
        
        // Access HTTP client
        val httpClient = context.httpClient
        
        // Access logging
        context.log("Debug message", LogLevel.DEBUG)
        
        // Access APIs
        val analyticsApi = context.getApi(ReadingAnalyticsApi::class)
        val characterApi = context.getApi(CharacterDatabaseApi::class)
    }
}
```

### HTTP Requests

```kotlin
// GET request
val response = context.httpClient.get(
    url = "https://api.example.com/data",
    headers = mapOf("Authorization" to "Bearer token")
)

// POST request
val response = context.httpClient.post(
    url = "https://api.example.com/data",
    body = """{"key": "value"}""",
    headers = mapOf("Content-Type" to "application/json")
)

// Check response
if (response.statusCode == 200) {
    val data = response.body
    val bytes = response.bodyBytes
}
```

### Preferences

```kotlin
// Save preferences
context.preferences.putString("api_key", "your-key")
context.preferences.putInt("count", 42)
context.preferences.putBoolean("enabled", true)

// Load preferences
val apiKey = context.preferences.getString("api_key", "")
val count = context.preferences.getInt("count", 0)
val enabled = context.preferences.getBoolean("enabled", false)
```

### Reading Analytics API

```kotlin
val analyticsApi = context.getApi(ReadingAnalyticsApi::class)

// Get reading stats
val todayStats = analyticsApi.getTodayStats()
val weekStats = analyticsApi.getWeekStats()
val allTimeStats = analyticsApi.getAllTimeStats()

// Get streak info
val streak = analyticsApi.getCurrentStreak()
println("Current streak: ${streak.currentStreak} days")

// Get book-specific stats
val bookStats = analyticsApi.getBookStats(bookId)
```

### Character Database API

```kotlin
val characterApi = context.getApi(CharacterDatabaseApi::class)

// Get characters for a book
val characters = characterApi.getCharactersForBook(bookId)

// Get character relationships
val relationships = characterApi.getRelationships(characterId)

// Add a character
characterApi.addCharacter(CharacterInfo(
    name = "John Doe",
    bookId = bookId,
    description = "Main protagonist"
))
```

---

## Building and Testing

### Build Commands

```bash
# Build a single plugin
./gradlew :plugins:features:my-plugin:assemble

# Build all plugins
./gradlew buildAllPlugins

# Clean and rebuild
./gradlew clean buildAllPlugins

# Generate repository index
./gradlew repo

# Run tests (if you have them)
./gradlew :plugins:features:my-plugin:test
```

### Plugin Output

After building, your plugin will be at:
```
plugins/{type}/{name}/build/outputs/{name}-{version}.iplugin
```

The `.iplugin` file is a ZIP archive containing:
```
my-plugin-1.0.0.iplugin
├── plugin.json          # Generated manifest
├── classes.jar          # JVM/Desktop classes
└── android/
    └── classes.dex      # Android DEX (if d8 available)
```

### Testing Locally

1. **Build your plugin**
   ```bash
   ./gradlew :plugins:features:my-plugin:assemble
   ```

2. **Copy to device**
   - Android: Copy `.iplugin` to device storage
   - Desktop: Copy to IReader's plugin directory

3. **Install in IReader**
   - Go to **Community Hub → Install from File**
   - Select your `.iplugin` file

4. **Enable the plugin**
   - Go to **Settings → Plugins**
   - Enable your plugin

### Debugging

```kotlin
// Add logging to your plugin
override fun initialize(context: PluginContext) {
    context.log("Plugin initialized", LogLevel.INFO)
}

// Log errors
try {
    // ... operation
} catch (e: Exception) {
    context.log("Error: ${e.message}", LogLevel.ERROR)
}
```

---

## Publishing Your Plugin

### 1. Prepare for Release

- Update version in `build.gradle.kts`
- Increment `versionCode`
- Test thoroughly on all target platforms
- Update description and metadata

### 2. Build Release

```bash
./gradlew :plugins:features:my-plugin:assemble
```

### 3. Generate Repository

```bash
./gradlew repo
```

This creates:
```
repo/
├── index.json           # Repository index
├── images/              # Plugin icons
└── plugins/
    └── my-plugin-1.0.0.iplugin
```

### 4. Host Your Repository

**Option A: GitHub Pages**
1. Create a `gh-pages` branch
2. Copy `repo/` contents to the branch
3. Enable GitHub Pages in repository settings
4. Your repo URL: `https://username.github.io/repo-name/repo`

**Option B: Any Web Server**
1. Upload `repo/` directory to your server
2. Ensure files are accessible via HTTP/HTTPS

### 5. Share Your Repository

Users can add your repository in IReader:
1. Go to **Community Hub → Plugin Repositories**
2. Tap **Add Repository**
3. Enter your repository URL
4. Your plugins will appear in the plugin list

---

## Best Practices

### Code Quality

```kotlin
// ✅ Good: Clear, documented code
/**
 * Translates text from source to target language.
 * @param text The text to translate
 * @param from Source language code (e.g., "en")
 * @param to Target language code (e.g., "es")
 * @return Translated text or error
 */
override suspend fun translate(text: String, from: String, to: String): Result<String>

// ❌ Bad: No documentation, unclear purpose
override suspend fun translate(text: String, from: String, to: String): Result<String>
```

### Error Handling

```kotlin
// ✅ Good: Comprehensive error handling
override suspend fun translate(text: String, from: String, to: String): Result<String> {
    if (text.isBlank()) {
        return Result.failure(IllegalArgumentException("Text cannot be empty"))
    }
    
    if (apiKey.isBlank()) {
        return Result.failure(Exception("API key not configured"))
    }
    
    return try {
        val response = httpClient.post(url, body, headers)
        when (response.statusCode) {
            200 -> Result.success(parseResponse(response.body))
            401 -> Result.failure(Exception("Invalid API key"))
            429 -> Result.failure(Exception("Rate limit exceeded"))
            else -> Result.failure(Exception("API error: ${response.statusCode}"))
        }
    } catch (e: Exception) {
        Result.failure(Exception("Network error: ${e.message}"))
    }
}

// ❌ Bad: No error handling
override suspend fun translate(text: String, from: String, to: String): Result<String> {
    val response = httpClient.post(url, body, headers)
    return Result.success(parseResponse(response.body))
}
```

### Resource Management

```kotlin
// ✅ Good: Clean up resources
override fun cleanup() {
    httpClient?.close()
    httpClient = null
    cache.clear()
    context = null
}

// ❌ Bad: Resources not released
override fun cleanup() {
    // Nothing here
}
```

### Permissions

```kotlin
// ✅ Good: Request only needed permissions
permissions.set(listOf(PluginPermission.NETWORK)) // Only network needed

// ❌ Bad: Request all permissions "just in case"
permissions.set(listOf(
    PluginPermission.NETWORK,
    PluginPermission.STORAGE,
    PluginPermission.READER_CONTEXT,
    PluginPermission.LIBRARY_ACCESS,
    PluginPermission.PREFERENCES,
    PluginPermission.NOTIFICATIONS
))
```

### Security

```kotlin
// ✅ Good: API key stored securely
override fun configureApiKey(key: String) {
    apiKey = key
    context?.preferences?.putString("api_key", key)
}

// ❌ Bad: API key hardcoded
private val apiKey = "sk-1234567890abcdef" // NEVER DO THIS
```

---

## Troubleshooting

### Plugin Not Loading

**Symptoms**: Plugin doesn't appear in IReader

**Solutions**:
1. Verify `@IReaderPlugin` annotation is present
2. Check manifest ID matches build config
3. Look for compilation errors in build output
4. Ensure plugin implements correct interface

### Permission Denied

**Symptoms**: Operations fail with permission errors

**Solutions**:
1. Add required permission to `pluginConfig.permissions`
2. Verify permission is declared in manifest
3. Check if user has granted permission

### Network Requests Failing

**Symptoms**: HTTP requests return errors

**Solutions**:
1. Ensure `PluginPermission.NETWORK` is declared
2. Verify URL is HTTPS (HTTP may be blocked)
3. Check API key is configured correctly
4. Handle timeouts appropriately

### Plugin Crashes

**Symptoms**: App crashes when using plugin

**Solutions**:
1. Add try-catch around risky operations
2. Check for null values
3. Verify all resources are initialized
4. Test on all target platforms

### Build Errors

**Symptoms**: Gradle build fails

**Solutions**:
1. Run `./gradlew clean` and rebuild
2. Check for syntax errors in Kotlin code
3. Verify build.gradle.kts syntax
4. Ensure all dependencies are available

---

## Additional Resources

- [Project Overview](./PROJECT_OVERVIEW.md) - What this project is about
- [Developer Guide](./DEVELOPER_GUIDE.md) - Comprehensive developer documentation
- [Quick Reference](./QUICK_REFERENCE.md) - Cheat sheet for common tasks
- [AI Context](./AI_CONTEXT.md) - Context for AI assistants
- [Example Plugins](../plugins/) - Reference implementations

## Getting Help

- **Discord**: [IReader Community](https://discord.gg/ireader)
- **Issues**: [GitHub Issues](https://github.com/IReaderorg/IReader-plugins/issues)
- **Discussions**: [GitHub Discussions](https://github.com/IReaderorg/IReader-plugins/discussions)

---

Happy plugin development! 🚀
