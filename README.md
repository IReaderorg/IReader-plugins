# IReader Plugins

Official plugin repository for IReader app. Create custom themes, TTS engines, translation services, AI features, and more.


## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Project Overview](docs/PROJECT_OVERVIEW.md) | What this project is and why it exists |
| [Developer Guide](docs/DEVELOPER_GUIDE.md) | Comprehensive guide for plugin developers |
| [Quick Reference](docs/QUICK_REFERENCE.md) | Cheat sheet for common tasks |
| [AI Context](docs/AI_CONTEXT.md) | Context document for AI assistants |

## Getting Started

### Prerequisites

- JDK 17+
- Gradle 8.10+

### Building

```bash
# Build all plugins
./gradlew buildAllPlugins

# Build specific plugin
./gradlew :plugins:themes:ocean-theme:assemble

# Generate repository index
./gradlew repo
```

## Creating a Plugin

### 1. Create Plugin Directory

```
plugins/
└── themes/           # or tts/, translation/, features/
    └── my-plugin/
        ├── build.gradle.kts
        └── src/main/kotlin/
            └── MyPlugin.kt
```

### 2. Configure build.gradle.kts

```kotlin
plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("com.example.my-plugin")
    name.set("My Plugin")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Description of my plugin")
    author.set("Your Name")
    type.set(PluginType.THEME)  // THEME, TTS, TRANSLATION, or FEATURE
    permissions.set(listOf())   // Required permissions
}

// No dependencies needed - plugin-api is provided as compileOnly
```

### 3. Implement Plugin Interface

Use KSP annotations for automatic plugin discovery:

```kotlin
import ireader.plugin.api.*
import ireader.plugin.annotations.*

@IReaderPlugin
@PluginInfo(
    id = "com.example.my-plugin",
    name = "My Plugin",
    version = "1.0.0",
    versionCode = 1,
    description = "Description of my plugin",
    author = "Your Name"
)
@RequiresPermissions(Permission.NETWORK) // Optional
class MyTheme : ThemePlugin {
    override val manifest = PluginManifest(
        id = "com.example.my-plugin",
        name = "My Plugin",
        // ... other fields from annotation
    )
    
    override fun initialize(context: PluginContext) { }
    override fun cleanup() { }
    
    override fun getColorScheme(isDark: Boolean): ThemeColorScheme {
        // Return your color scheme
    }
    
    override fun getExtraColors(isDark: Boolean): ThemeExtraColors {
        // Return extra colors
    }
}
```

## KSP Annotations

The plugin system uses KSP (Kotlin Symbol Processing) for compile-time plugin discovery:

| Annotation | Description |
|------------|-------------|
| `@IReaderPlugin` | Marks a class as a plugin entry point (required) |
| `@PluginInfo` | Plugin metadata (id, name, version, etc.) |
| `@RequiresPermissions` | Declares required permissions |
| `@PremiumPlugin` | Marks plugin as premium with price info |
| `@FreemiumPlugin` | Marks plugin as freemium |

The KSP compiler generates a `PluginFactory` class that enables runtime plugin discovery without reflection.

## Plugin Types

### Theme Plugin

Customize app appearance with colors, typography, and backgrounds.

```kotlin
class MyTheme : ThemePlugin {
    override fun getColorScheme(isDark: Boolean): ThemeColorScheme
    override fun getExtraColors(isDark: Boolean): ThemeExtraColors
    override fun getTypography(): ThemeTypography?
    override fun getBackgroundAssets(): ThemeBackgrounds?
}
```

### TTS Plugin

Add text-to-speech engines.

```kotlin
class MyTTS : TTSPlugin {
    override suspend fun speak(text: String, voice: VoiceConfig): Result<AudioStream>
    override fun getAvailableVoices(): List<VoiceModel>
    override fun supportsStreaming(): Boolean
    override fun getAudioFormat(): AudioFormat
}
```

### Translation Plugin

Add translation services.

```kotlin
class MyTranslation : TranslationPlugin {
    override suspend fun translate(text: String, from: String, to: String): Result<String>
    override suspend fun translateBatch(texts: List<String>, from: String, to: String): Result<List<String>>
    override fun getSupportedLanguages(): List<LanguagePair>
    override fun requiresApiKey(): Boolean
    override fun configureApiKey(key: String)
}
```

### Feature Plugin

Add custom features to the reader.

```kotlin
class MyFeature : FeaturePlugin {
    override fun getMenuItems(): List<PluginMenuItem>
    override fun getScreens(): List<PluginScreen>
    override fun onReaderContext(context: ReaderContext): PluginAction?
    override fun getPreferencesScreen(): PluginScreen?
}
```

## Permissions

Declare required permissions in your plugin config:

| Permission | Description |
|------------|-------------|
| `NETWORK` | Make HTTP requests |
| `STORAGE` | Access local storage |
| `READER_CONTEXT` | Access current reading state |
| `LIBRARY_ACCESS` | Access user's library |
| `PREFERENCES` | Read/write preferences |
| `NOTIFICATIONS` | Show notifications |

## Monetization

Plugins support three monetization models:

```kotlin
// Free plugin (default)
monetization.set(PluginMonetizationType.FREE)

// Premium plugin
monetization.set(PluginMonetizationType.PREMIUM)
price.set(4.99)
currency.set("USD")
trialDays.set(7)

// Freemium plugin
monetization.set(PluginMonetizationType.FREEMIUM)
```

## Project Structure

```
IReader-plugins/
├── buildSrc/              # Build logic and plugin DSL
├── plugins/
│   ├── themes/            # Theme plugins
│   ├── tts/               # TTS plugins
│   ├── translation/       # Translation plugins
│   └── features/          # Feature plugins
├── repo/                  # Generated repository (after ./gradlew repo)
└── gradle/
    └── libs.versions.toml # Version catalog
```

## Dependencies

All dependencies are `compileOnly` - IReader provides them at runtime:

- `io.github.ireaderorg:plugin-api` - Plugin interfaces and utilities
- `kotlinx-coroutines-core` - Coroutines support
- `kotlinx-serialization-json` - JSON serialization
- `kotlinx-datetime` - Date/time utilities
- `ktor-client-*` - HTTP client

### Using HTTP in Plugins

```kotlin
import ireader.plugin.api.http.PluginHttpClient
import ireader.plugin.api.http.getText

// Create client and make request
val client = PluginHttpClient.create()
val response = client.getText("https://api.example.com/data")
client.close()

// Or use with block
PluginHttpClient.create().use { client ->
    val data = client.getText("https://api.example.com/data")
}
```

### Using JSON in Plugins

```kotlin
import ireader.plugin.api.util.JsonHelper

// Encode to JSON
val json = JsonHelper.encode(myObject)

// Decode from JSON
val obj = JsonHelper.decode<MyClass>(jsonString)
```

## Plugin Package Format

Built plugins are packaged as `.iplugin` files (ZIP archives):

```
my-plugin.iplugin
├── plugin.json          # Generated manifest
├── classes.jar          # JVM/Desktop classes
└── android/
    └── classes.dex      # Android DEX (if d8 available)
```

## Publishing

After building, run `./gradlew repo` to generate the repository index at `repo/index.json`.

```bash
# Build all plugins and generate repository
./gradlew buildAllPlugins repo
```

The generated `repo/` directory structure:

```
repo/
├── index.json           # Repository index with all plugins
└── plugins/
    ├── plugin-a-1.0.0.iplugin
    └── plugin-b-2.0.0.iplugin
```

Host the `repo/` directory on a web server or GitHub Pages to distribute your plugins.

Users can add your repository URL in IReader:
**Community Hub → Plugin Repositories → Add Repository**

## Plugin Types

| Type | Interface | Description |
|------|-----------|-------------|
| Theme | `ThemePlugin` | Custom colors, typography, backgrounds |
| TTS | `TTSPlugin` | Text-to-speech engines |
| Translation | `TranslationPlugin` | Language translation services |
| Feature | `FeaturePlugin` | Custom reader features |
| AI | `AIPlugin` | AI-powered features (summarization, Q&A) |

## Available Plugin APIs

Plugins can access these IReader APIs:

| API | Description |
|-----|-------------|
| `ReadingAnalyticsApi` | Reading statistics, goals, achievements |
| `CharacterDatabaseApi` | Character tracking, relationships |
| `PluginHttpClient` | HTTP requests |
| `JsonHelper` | JSON serialization |

## Community

- **Discord**: [IReader Community](https://discord.gg/ireader)
- **Issues**: [GitHub Issues](https://github.com/IReaderorg/IReader-plugins/issues)
- **Discussions**: [GitHub Discussions](https://github.com/IReaderorg/IReader-plugins/discussions)

## License

Mozilla Public License 2.0
