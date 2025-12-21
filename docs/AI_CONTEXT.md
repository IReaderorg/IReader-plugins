# IReader Plugins - AI Assistant Context

This document provides context for AI assistants helping developers with IReader plugin development.

## Project Overview

**IReader-plugins** is the official plugin repository for IReader, a cross-platform novel/book reader built with Kotlin Multiplatform. This repository contains:

1. Plugin build infrastructure (Gradle plugins, KSP processors)
2. Example plugins for reference
3. Repository generation tools for plugin distribution

## Technology Stack

| Technology | Purpose |
|------------|---------|
| Kotlin | Primary language |
| Kotlin Multiplatform | Cross-platform support (Android, iOS, Desktop) |
| Gradle | Build system |
| KSP | Compile-time annotation processing |
| Kotlinx Serialization | JSON serialization |
| Kotlinx Coroutines | Async operations |
| Ktor | HTTP client |

## Project Structure

```
IReader-plugins/
├── buildSrc/                    # Gradle build logic
│   └── src/main/kotlin/
│       ├── IReaderPluginPlugin.kt    # Custom Gradle plugin
│       └── PluginConfig.kt           # Plugin DSL
├── annotations/                 # KSP annotations
│   └── src/commonMain/kotlin/
│       └── ireader/plugin/annotations/
├── compiler/                    # KSP processor
│   └── src/main/kotlin/
│       └── ireader/plugin/compiler/
├── plugins/                     # Actual plugins
│   ├── themes/                  # Theme plugins
│   ├── tts/                     # TTS plugins
│   ├── translation/             # Translation plugins
│   └── features/                # Feature plugins
├── docs/                        # Documentation
└── repo/                        # Generated repository (after build)
```

## Plugin Types and Interfaces

### 1. ThemePlugin
```kotlin
interface ThemePlugin : Plugin {
    fun getColorScheme(isDark: Boolean): ThemeColorScheme
    fun getExtraColors(isDark: Boolean): ThemeExtraColors
    fun getTypography(): ThemeTypography?
    fun getBackgroundAssets(): ThemeBackgrounds?
}
```

### 2. TTSPlugin
```kotlin
interface TTSPlugin : Plugin {
    suspend fun speak(text: String, voice: VoiceConfig): Result<AudioStream>
    fun getAvailableVoices(): List<VoiceModel>
    fun supportsStreaming(): Boolean
    fun getAudioFormat(): AudioFormat
}
```

### 3. TranslationPlugin
```kotlin
interface TranslationPlugin : Plugin {
    suspend fun translate(text: String, from: String, to: String): Result<String>
    suspend fun translateBatch(texts: List<String>, from: String, to: String): Result<List<String>>
    fun getSupportedLanguages(): List<LanguagePair>
    fun requiresApiKey(): Boolean
    fun configureApiKey(key: String)
}
```

### 4. FeaturePlugin
```kotlin
interface FeaturePlugin : Plugin {
    fun getMenuItems(): List<PluginMenuItem>
    fun getScreens(): List<PluginScreen>
    fun onReaderContext(context: ReaderContext): PluginAction?
    fun getPreferencesScreen(): PluginScreen?
}
```

### 5. AIPlugin
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

## Key Annotations

```kotlin
@IReaderPlugin          // Marks class as plugin entry point (required)
@PluginInfo(...)        // Plugin metadata
@RequiresPermissions()  // Declares required permissions
@PremiumPlugin          // Marks as premium
@FreemiumPlugin         // Marks as freemium
```

## Available Permissions

```kotlin
enum class Permission {
    NETWORK,           // HTTP requests
    STORAGE,           // File access
    READER_CONTEXT,    // Current reading state
    LIBRARY_ACCESS,    // User's library
    PREFERENCES,       // Read/write prefs
    NOTIFICATIONS      // Show notifications
}
```

## Plugin APIs Available to Plugins

### ReadingAnalyticsApi
Provides access to reading statistics:
- Session tracking (current session, duration, words read)
- Daily/weekly/monthly stats
- Reading speed (WPM)
- Streaks (current, longest)
- Goals and achievements
- Reading patterns

### CharacterDatabaseApi
Provides access to character tracking:
- Character CRUD
- Relationships between characters
- Appearance tracking
- Notes and timeline
- Statistics

## Common Tasks

### Creating a New Plugin

1. Create directory: `plugins/{type}/{plugin-name}/`
2. Create `build.gradle.kts` with `ireader-plugin` plugin
3. Create main plugin class with `@IReaderPlugin` annotation
4. Implement appropriate interface (ThemePlugin, TTSPlugin, etc.)
5. Build with `./gradlew :plugins:{type}:{plugin-name}:assemble`

### Plugin build.gradle.kts Template

```kotlin
plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("com.example.plugin-name")
    name.set("Plugin Name")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Description")
    author.set("Author Name")
    type.set(PluginType.FEATURE)  // THEME, TTS, TRANSLATION, FEATURE, AI
    permissions.set(listOf(Permission.NETWORK))
    
    // Optional monetization
    monetization.set(PluginMonetizationType.FREE)  // FREE, PREMIUM, FREEMIUM
}
```

### Plugin Class Template

```kotlin
package com.example.pluginname

import ireader.plugin.api.*
import ireader.plugin.annotations.*

@IReaderPlugin
@PluginInfo(
    id = "com.example.plugin-name",
    name = "Plugin Name",
    version = "1.0.0",
    versionCode = 1,
    description = "Description",
    author = "Author Name"
)
class MyPlugin : FeaturePlugin {
    override val manifest = PluginManifest(
        id = "com.example.plugin-name",
        name = "Plugin Name",
        version = "1.0.0",
        versionCode = 1,
        description = "Description",
        author = PluginAuthor("Author Name"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP)
    )
    
    private lateinit var context: PluginContext
    
    override fun initialize(context: PluginContext) {
        this.context = context
    }
    
    override fun cleanup() {
        // Release resources
    }
    
    override fun getMenuItems(): List<PluginMenuItem> = emptyList()
    override fun getScreens(): List<PluginScreen> = emptyList()
    override fun onReaderContext(context: ReaderContext): PluginAction? = null
}
```

## Build Commands

```bash
# Build single plugin
./gradlew :plugins:features:my-plugin:assemble

# Build all plugins
./gradlew buildAllPlugins

# Generate repository index
./gradlew repo

# Run tests
./gradlew :plugins:features:my-plugin:test

# Clean build
./gradlew clean
```

## Important Constraints

### Kotlin Multiplatform Compatibility
- NO Java-only APIs (java.io.File, java.util.Date, etc.)
- Use kotlinx-datetime for dates
- Use okio for file I/O
- Use kotlinx-coroutines for async

### Plugin Sandbox
- Plugins run in sandboxed environment
- Resource limits enforced (CPU, memory, time)
- Network requests go through proxy
- File access is restricted

### Dependencies
- All dependencies are `compileOnly`
- IReader provides them at runtime
- Don't bundle dependencies in plugin

## Error Handling Patterns

```kotlin
// Use Result for operations that can fail
suspend fun doSomething(): Result<String> {
    return try {
        val result = // ... operation
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// For AI operations, use AIResult
suspend fun aiOperation(): AIResult<String> {
    return try {
        AIResult.Success(data)
    } catch (e: Exception) {
        AIResult.Error(AIError.Unknown(e.message ?: "Unknown error"))
    }
}
```

## HTTP Requests

```kotlin
import ireader.plugin.api.http.PluginHttpClient

suspend fun fetchData(): String {
    return PluginHttpClient.create().use { client ->
        client.getText("https://api.example.com/data")
    }
}
```

## JSON Handling

```kotlin
import ireader.plugin.api.util.JsonHelper
import kotlinx.serialization.Serializable

@Serializable
data class MyData(val field: String)

// Encode
val json = JsonHelper.encode(MyData("value"))

// Decode
val data = JsonHelper.decode<MyData>(jsonString)
```

## Common Issues and Solutions

### Issue: Plugin not loading
- Check `@IReaderPlugin` annotation is present
- Verify manifest ID matches build config
- Check for compilation errors

### Issue: Permission denied
- Add required permission to `pluginConfig.permissions`
- Request permission at runtime if needed

### Issue: Network request failing
- Ensure `Permission.NETWORK` is declared
- Check URL is HTTPS
- Handle timeouts appropriately

### Issue: Plugin crashes on iOS
- Avoid JVM-only APIs
- Use expect/actual for platform-specific code
- Test on all target platforms

## Related Files in Main IReader Repository

When helping with plugin development, these files in the main IReader repo are relevant:

- `plugin-api/src/commonMain/kotlin/ireader/plugin/api/` - All plugin interfaces
- `domain/src/commonMain/kotlin/ireader/domain/plugins/` - Plugin management
- `data/src/commonMain/kotlin/ireader/data/plugins/` - Plugin data layer

## Version Compatibility

| IReader Version | Plugin API Version | Notes |
|-----------------|-------------------|-------|
| 1.0.x | 1.0.x | Initial release |
| 1.1.x | 1.1.x | Added AI plugins |
| 1.2.x | 1.2.x | Added analytics API |

## Testing Checklist

When reviewing or creating plugins:

- [ ] Plugin compiles without errors
- [ ] All required annotations present
- [ ] Manifest matches build config
- [ ] Permissions are minimal and justified
- [ ] Error handling is comprehensive
- [ ] Resources are released in cleanup()
- [ ] No hardcoded API keys
- [ ] Works on all target platforms
- [ ] Documentation is complete

## Related Documentation

- [Plugin Creation Guide](./PLUGIN_CREATION_GUIDE.md) - Step-by-step tutorials with complete examples
- [Developer Guide](./DEVELOPER_GUIDE.md) - Comprehensive reference documentation
- [Quick Reference](./QUICK_REFERENCE.md) - Cheat sheet for common tasks
- [Project Overview](./PROJECT_OVERVIEW.md) - What this project is about
