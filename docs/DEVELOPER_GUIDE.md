# IReader Plugin Developer Guide

A comprehensive guide for developers creating plugins for IReader.

## Table of Contents

1. [Introduction](#introduction)
2. [Project Purpose](#project-purpose)
3. [Architecture Overview](#architecture-overview)
4. [Plugin Types](#plugin-types)
5. [Getting Started](#getting-started)
6. [Advanced Features](#advanced-features)
7. [Best Practices](#best-practices)
8. [Testing & Debugging](#testing--debugging)
9. [Publishing](#publishing)
10. [Monetization](#monetization)

---

## Introduction

IReader is a cross-platform novel/book reader application built with Kotlin Multiplatform. The plugin system allows developers to extend IReader's functionality without modifying the core application.

### What You Can Build

- **Theme Plugins**: Custom color schemes, typography, backgrounds
- **TTS Plugins**: Text-to-speech engines (cloud or local)
- **Translation Plugins**: Translation services for reading in different languages
- **Feature Plugins**: Custom features like dictionaries, note-taking, AI assistants
- **AI Plugins**: Summarization, character analysis, Q&A about books

---

## Project Purpose

### Why Plugins?

1. **Extensibility**: Users can customize their reading experience
2. **Community**: Developers can contribute without core access
3. **Modularity**: Features can be added/removed independently
4. **Monetization**: Developers can earn from premium plugins
5. **Innovation**: Experiment with new features safely

### Goals

- Provide a stable, well-documented API
- Enable rich plugin capabilities while maintaining security
- Support cross-platform (Android, iOS, Desktop)
- Foster a healthy plugin ecosystem

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      IReader App                             │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │   Reader    │  │   Library   │  │   Plugin Manager    │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                      Plugin API                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │  Theme   │ │   TTS    │ │ Translate│ │   Feature    │   │
│  │  Plugin  │ │  Plugin  │ │  Plugin  │ │   Plugin     │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │
├─────────────────────────────────────────────────────────────┤
│                    Plugin Sandbox                            │
│  • Resource limits  • Permission control  • Isolation       │
└─────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Description |
|-----------|-------------|
| Plugin API | Interfaces and utilities for plugin development |
| Plugin Manager | Loads, enables, disables plugins |
| Plugin Sandbox | Security and resource isolation |
| Plugin Registry | Tracks installed plugins |
| Plugin Marketplace | Discovery and distribution |

---

## Plugin Types

### 1. Theme Plugin

Customize the visual appearance of IReader.

```kotlin
@IReaderPlugin
class OceanTheme : ThemePlugin {
    override fun getColorScheme(isDark: Boolean) = ThemeColorScheme(
        primary = if (isDark) Color(0xFF64B5F6) else Color(0xFF1976D2),
        background = if (isDark) Color(0xFF0D1B2A) else Color(0xFFF5F5F5),
        // ... more colors
    )
}
```

**Use Cases:**
- Dark/light themes
- High contrast themes
- Seasonal themes
- Brand themes

### 2. TTS Plugin

Add text-to-speech capabilities.

```kotlin
@IReaderPlugin
class CloudTTS : TTSPlugin {
    override suspend fun speak(text: String, voice: VoiceConfig): Result<AudioStream> {
        // Call TTS API and return audio stream
    }
    
    override fun getAvailableVoices() = listOf(
        VoiceModel("en-us-1", "English (US)", "en-US", VoiceGender.FEMALE)
    )
}
```

**Use Cases:**
- Cloud TTS (Google, Amazon, Azure)
- Local TTS engines
- Custom voice models
- Multi-language support

### 3. Translation Plugin

Enable reading in different languages.

```kotlin
@IReaderPlugin
class DeepLTranslation : TranslationPlugin {
    override suspend fun translate(text: String, from: String, to: String): Result<String> {
        // Call translation API
    }
}
```

**Use Cases:**
- Cloud translation (DeepL, Google, etc.)
- Offline translation
- Specialized dictionaries
- Language learning tools

### 4. Feature Plugin

Add custom functionality to the reader.

```kotlin
@IReaderPlugin
class DictionaryPlugin : FeaturePlugin {
    override fun getMenuItems() = listOf(
        PluginMenuItem("lookup", "Look up word", "dictionary")
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? {
        context.selectedText?.let { word ->
            return PluginAction.Navigate("dictionary/$word")
        }
        return null
    }
}
```

**Use Cases:**
- Dictionary lookup
- Note-taking
- Bookmarks with tags
- Reading statistics
- Social sharing

### 5. AI Plugin

Intelligent text processing capabilities.

```kotlin
@IReaderPlugin
class SummarizerPlugin : AIPlugin {
    override val aiCapabilities = listOf(
        AICapability.SUMMARIZATION,
        AICapability.CHARACTER_ANALYSIS
    )
    
    override suspend fun summarize(text: String, options: SummarizationOptions): AIResult<String> {
        // Use local or cloud AI
    }
}
```

**Use Cases:**
- Chapter/book summarization
- Character analysis
- Q&A about the book
- Content recommendations
- Sentiment analysis

---

## Getting Started

### Prerequisites

- JDK 17 or higher
- Gradle 8.10+
- IDE: IntelliJ IDEA or Android Studio

### Step 1: Clone the Repository

```bash
git clone https://github.com/IReaderorg/IReader-plugins.git
cd IReader-plugins
```

### Step 2: Create Plugin Directory

```bash
mkdir -p plugins/features/my-plugin/src/main/kotlin
```

### Step 3: Create build.gradle.kts

```kotlin
// plugins/features/my-plugin/build.gradle.kts
plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("com.yourname.my-plugin")
    name.set("My Awesome Plugin")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Does something awesome")
    author.set("Your Name")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(Permission.READER_CONTEXT))
}
```

### Step 4: Implement Your Plugin

```kotlin
// plugins/features/my-plugin/src/main/kotlin/MyPlugin.kt
package com.yourname.myplugin

import ireader.plugin.api.*
import ireader.plugin.annotations.*

@IReaderPlugin
@PluginInfo(
    id = "com.yourname.my-plugin",
    name = "My Awesome Plugin",
    version = "1.0.0",
    versionCode = 1,
    description = "Does something awesome",
    author = "Your Name"
)
class MyPlugin : FeaturePlugin {
    override val manifest = PluginManifest(
        id = "com.yourname.my-plugin",
        name = "My Awesome Plugin",
        version = "1.0.0",
        versionCode = 1,
        description = "Does something awesome",
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
        PluginMenuItem("action1", "Do Something")
    )
    
    override fun getScreens() = emptyList<PluginScreen>()
    
    override fun onReaderContext(context: ReaderContext): PluginAction? = null
}
```

### Step 5: Build and Test

```bash
# Build your plugin
./gradlew :plugins:features:my-plugin:assemble

# Build all plugins
./gradlew buildAllPlugins

# Generate repository
./gradlew repo
```

---

## Advanced Features

### Accessing Reading Analytics

Plugins can access reading statistics:

```kotlin
class StatsPlugin : FeaturePlugin {
    private lateinit var analyticsApi: ReadingAnalyticsApi
    
    override fun initialize(context: PluginContext) {
        analyticsApi = context.getApi(ReadingAnalyticsApi::class)
    }
    
    suspend fun showStats() {
        val stats = analyticsApi.getOverallStats()
        val streak = analyticsApi.getCurrentStreak()
        // Display to user
    }
}
```

### Accessing Character Database

Track and display character information:

```kotlin
class CharacterPlugin : FeaturePlugin {
    private lateinit var characterApi: CharacterDatabaseApi
    
    override fun initialize(context: PluginContext) {
        characterApi = context.getApi(CharacterDatabaseApi::class)
    }
    
    suspend fun showCharacters(bookId: Long) {
        val characters = characterApi.getCharactersForBook(bookId)
        val relationships = characters.flatMap { 
            characterApi.getRelationships(it.id) 
        }
        // Display character map
    }
}
```

### Plugin Composition (Chaining)

Combine plugins into workflows:

```kotlin
// In IReader app, users can create pipelines:
// Translate → TTS (translate text, then read aloud)
// Summarize → Translate (summarize, then translate summary)
```

### Cross-Plugin Communication

Plugins can communicate via events:

```kotlin
class MyPlugin : FeaturePlugin, EventHandler {
    override fun getSubscribedEventTypes() = setOf(
        CommonEventTypes.TEXT_SELECTED,
        CommonEventTypes.CHAPTER_CHANGED
    )
    
    override suspend fun handleEvent(event: PluginEvent) {
        when (event.eventType) {
            CommonEventTypes.TEXT_SELECTED -> {
                val text = event.payload["text"]
                // Handle selected text
            }
        }
    }
}
```

---

## Best Practices

### Performance

1. **Lazy Loading**: Don't load resources until needed
2. **Caching**: Cache API responses and computed results
3. **Background Work**: Use coroutines for heavy operations
4. **Memory**: Release resources in `cleanup()`

### Security

1. **Minimal Permissions**: Only request what you need
2. **Input Validation**: Validate all user input
3. **Secure Storage**: Use PluginContext for sensitive data
4. **API Keys**: Never hardcode API keys

### User Experience

1. **Responsive**: Show loading states
2. **Error Handling**: Graceful error messages
3. **Offline Support**: Handle network failures
4. **Localization**: Support multiple languages

### Code Quality

1. **Documentation**: Comment public APIs
2. **Testing**: Write unit tests
3. **Versioning**: Follow semantic versioning
4. **Changelog**: Document changes

---

## Testing & Debugging

### Local Testing

```bash
# Run tests
./gradlew :plugins:features:my-plugin:test

# Check for issues
./gradlew :plugins:features:my-plugin:check
```

### Hot Reload (Development)

Enable hot reload in IReader for faster development:

1. Enable Developer Mode in IReader settings
2. Point to your local plugin directory
3. Changes reload automatically

### Debugging

```kotlin
// Use logging
context.log("Debug message", LogLevel.DEBUG)

// Check plugin state
context.log("State: ${myState}", LogLevel.INFO)
```

---

## Publishing

### 1. Build Release

```bash
./gradlew :plugins:features:my-plugin:assembleRelease
```

### 2. Generate Repository

```bash
./gradlew repo
```

### 3. Host Repository

Upload `repo/` directory to:
- GitHub Pages
- Your own server
- Any static hosting

### 4. Share Repository URL

Users add your repository in:
**IReader → Community Hub → Plugin Repositories → Add**

---

## Monetization

### Free Plugins

Default model. Great for building reputation.

### Premium Plugins

```kotlin
pluginConfig {
    monetization.set(PluginMonetizationType.PREMIUM)
    price.set(4.99)
    currency.set("USD")
    trialDays.set(7)
}
```

### Freemium Plugins

Basic features free, premium features paid.

```kotlin
pluginConfig {
    monetization.set(PluginMonetizationType.FREEMIUM)
}

// In your plugin
fun isPremiumFeature(): Boolean {
    return context.isPurchased() || context.isInTrial()
}
```

---

## Resources

- [Plugin API Reference](./API_REFERENCE.md)
- [Example Plugins](../plugins/)
- [IReader Documentation](https://ireader.org/docs)
- [Discord Community](https://discord.gg/ireader)
- [GitHub Issues](https://github.com/IReaderorg/IReader-plugins/issues)

---

## License

Plugins are licensed under MPL 2.0 unless otherwise specified.
