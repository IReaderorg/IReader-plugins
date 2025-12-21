# IReader Plugins

<p align="center">
  <img src="repo/Gemini_Generated_Image_9378yo9378yo9378.png" alt="IReader Plugins" width="200"/>
</p>

<p align="center">
  <strong>Official Plugin Repository for IReader</strong><br>
  Extend your reading experience with custom themes, TTS engines, translation services, AI features, and more.
</p>

<p align="center">
  <a href="#-what-is-this">What is This?</a> •
  <a href="#-plugin-types">Plugin Types</a> •
  <a href="#-quick-start">Quick Start</a> •
  <a href="#-documentation">Documentation</a> •
  <a href="#-community">Community</a>
</p>

---

## 🎯 What is This?

**IReader-plugins** is the official plugin development kit and repository for [IReader](https://github.com/IReaderorg/IReader), a cross-platform ebook/novel reader application built with Kotlin Multiplatform.

This repository provides:
- **Plugin Build System** - Gradle plugins and KSP processors for building IReader plugins
- **Plugin API** - Interfaces and utilities for plugin development
- **Example Plugins** - Reference implementations for all plugin types
- **Repository Tools** - Generate and host your own plugin repository

### Why Plugins?

| Problem | Solution |
|---------|----------|
| Monolithic apps are hard to customize | Modular plugin architecture |
| New features require app updates | Install plugins on-demand |
| Third-party contributions are difficult | Open plugin ecosystem |
| Feature bloat in main app | Keep core lightweight |

## 🔌 Plugin Types

| Type | Interface | What It Does | Examples |
|------|-----------|--------------|----------|
| **Theme** | `ThemePlugin` | Custom colors, typography, backgrounds | Ocean Theme, Sakura Night, Cyber Neon |
| **TTS** | `TTSPlugin` | Text-to-speech engines | Piper TTS, Edge TTS, XTTS |
| **Translation** | `TranslationPlugin` | Language translation services | OpenAI, DeepSeek, LibreTranslate |
| **Feature** | `FeaturePlugin` | Custom reader features | Reading Stats, Dictionary, Notes |
| **AI** | `AIPlugin` | AI-powered capabilities | Summarizer, Character Analyzer |

## 🚀 Quick Start

### Prerequisites

- JDK 17+
- Gradle 8.10+

### 1. Clone the Repository

```bash
git clone https://github.com/IReaderorg/IReader-plugins.git
cd IReader-plugins
```

### 2. Create Your Plugin

```bash
# Create plugin directory
mkdir -p plugins/features/my-plugin/src/main/kotlin
```

### 3. Configure build.gradle.kts

```kotlin
// plugins/features/my-plugin/build.gradle.kts
plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("com.yourname.my-plugin")
    name.set("My Plugin")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Description of my plugin")
    author.set("Your Name")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(PluginPermission.READER_CONTEXT))
}
```

### 4. Implement Your Plugin

```kotlin
// plugins/features/my-plugin/src/main/kotlin/MyPlugin.kt
package com.yourname.myplugin

import ireader.plugin.api.*
import ireader.plugin.annotations.*

@IReaderPlugin
@PluginMetadata(
    id = "com.yourname.my-plugin",
    name = "My Plugin",
    version = "1.0.0",
    versionCode = 1,
    description = "Description of my plugin",
    author = "Your Name"
)
class MyPlugin : FeaturePlugin {
    override val manifest = PluginManifest(
        id = "com.yourname.my-plugin",
        name = "My Plugin",
        version = "1.0.0",
        versionCode = 1,
        description = "Description of my plugin",
        author = PluginAuthor("Your Name"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.READER_CONTEXT),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP)
    )
    
    override fun initialize(context: PluginContext) {
        // Setup your plugin
    }
    
    override fun cleanup() {
        // Release resources
    }
    
    override fun getMenuItems() = listOf(
        PluginMenuItem("action", "Do Something", "icon_name")
    )
    
    override fun getScreens() = emptyList<PluginScreen>()
    override fun onReaderContext(context: ReaderContext): PluginAction? = null
}
```

### 5. Build and Test

```bash
# Build your plugin
./gradlew :plugins:features:my-plugin:assemble

# Build all plugins
./gradlew buildAllPlugins

# Generate repository index
./gradlew repo
```

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Project Overview](docs/PROJECT_OVERVIEW.md) | What this project is and why it exists |
| [Developer Guide](docs/DEVELOPER_GUIDE.md) | Comprehensive guide for plugin developers |
| [Plugin Creation Guide](docs/PLUGIN_CREATION_GUIDE.md) | Step-by-step tutorial for creating plugins |
| [Quick Reference](docs/QUICK_REFERENCE.md) | Cheat sheet for common tasks |
| [AI Context](docs/AI_CONTEXT.md) | Context document for AI assistants |

## 📁 Project Structure

```
IReader-plugins/
├── annotations/           # KSP annotations (@IReaderPlugin, @PluginMetadata, etc.)
├── compiler/              # KSP processor for compile-time validation
├── buildSrc/              # Gradle plugin DSL and build logic
├── plugins/               # Plugin implementations
│   ├── themes/            # Theme plugins (12+ themes)
│   ├── tts/               # TTS plugins (14+ engines)
│   ├── translation/       # Translation plugins (5+ services)
│   ├── features/          # Feature plugins (9+ features)
│   ├── engines/           # JavaScript engines
│   ├── screens/           # Custom reader screens
│   └── sources/           # Source loaders
├── example/               # Example plugin templates
├── docs/                  # Documentation
├── repo/                  # Generated repository (after build)
└── gradle/
    └── libs.versions.toml # Version catalog
```

## 🎨 Available Plugins

### Themes (12)
Arctic Aurora, Coffee Bean, Coral Reef, Cyber Neon, Desert Sand, Mint Fresh, Nordic Frost, Ocean Theme, Royal Velvet, Sakura Night, Slate Gray, Vintage Sepia

### TTS Engines (14)
Piper TTS, Edge TTS, XTTS v2, Fish Speech, OpenVoice, Bark TTS, Parler TTS, Silero TTS, Style TTS 2, Tortoise TTS, Persian variants

### Translation Services (5)
OpenAI, DeepSeek, Ollama, HuggingFace, LibreTranslate

### Features (9)
AI Summarizer, Bookmark Manager, Chapter Notes, Quote Highlighter, Reading Goals, Reading Stats, Reading Timer, Reading Tracker, Smart Dictionary

## 🔐 Permissions

Plugins must declare required permissions:

| Permission | Description | Use Case |
|------------|-------------|----------|
| `NETWORK` | Make HTTP requests | API calls, cloud services |
| `STORAGE` | Access local storage | Caching, downloads |
| `READER_CONTEXT` | Access reading state | Text selection, position |
| `LIBRARY_ACCESS` | Access user's library | Book metadata |
| `PREFERENCES` | Read/write preferences | User settings |
| `NOTIFICATIONS` | Show notifications | Background updates |

## 💰 Monetization

Plugins support three monetization models:

```kotlin
// Free (default)
monetization.set(PluginMonetizationType.FREE)

// Premium - one-time purchase
monetization.set(PluginMonetizationType.PREMIUM)
price.set(4.99)
currency.set("USD")
trialDays.set(7)

// Freemium - free with paid features
monetization.set(PluginMonetizationType.FREEMIUM)
```

## 📦 Plugin Package Format

Built plugins are packaged as `.iplugin` files (ZIP archives):

```
my-plugin.iplugin
├── plugin.json          # Generated manifest
├── classes.jar          # JVM/Desktop classes
└── android/
    └── classes.dex      # Android DEX (if d8 available)
```

## 🌐 Publishing Your Plugins

1. Build your plugins: `./gradlew buildAllPlugins`
2. Generate repository: `./gradlew repo`
3. Host the `repo/` directory on GitHub Pages or any web server
4. Users add your repository URL in IReader:
   **Community Hub → Plugin Repositories → Add Repository**

## 🤝 Community

- **Discord**: [IReader Community](https://discord.gg/ireader)
- **Issues**: [GitHub Issues](https://github.com/IReaderorg/IReader-plugins/issues)
- **Discussions**: [GitHub Discussions](https://github.com/IReaderorg/IReader-plugins/discussions)

## 📄 License

Mozilla Public License 2.0

---

<p align="center">
  Made with ❤️ by the IReader Team
</p>
