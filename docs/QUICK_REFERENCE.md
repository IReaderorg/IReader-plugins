# IReader Plugin Quick Reference

## Plugin Types at a Glance

| Type | Interface | Purpose | Key Methods |
|------|-----------|---------|-------------|
| Theme | `ThemePlugin` | Visual customization | `getColorScheme()`, `getTypography()` |
| TTS | `TTSPlugin` | Text-to-speech | `speak()`, `getAvailableVoices()` |
| Translation | `TranslationPlugin` | Language translation | `translate()`, `getSupportedLanguages()` |
| Feature | `FeaturePlugin` | Custom features | `getMenuItems()`, `onReaderContext()` |
| AI | `AIPlugin` | AI capabilities | `summarize()`, `analyzeCharacters()` |

## Permissions

| Permission | Description | When to Use |
|------------|-------------|-------------|
| `NETWORK` | HTTP requests | API calls, cloud services |
| `STORAGE` | File access | Caching, downloads |
| `READER_CONTEXT` | Reading state | Text selection, position |
| `LIBRARY_ACCESS` | User's library | Book metadata |
| `PREFERENCES` | Settings | User preferences |
| `NOTIFICATIONS` | Alerts | Background updates |

## Quick Start Commands

```bash
# Create plugin structure
mkdir -p plugins/features/my-plugin/src/main/kotlin

# Build single plugin
./gradlew :plugins:features:my-plugin:assemble

# Build all plugins
./gradlew buildAllPlugins

# Generate repository
./gradlew repo

# Clean and rebuild
./gradlew clean buildAllPlugins
```

## Minimal Plugin Template

```kotlin
@IReaderPlugin
@PluginInfo(id = "com.example.myplugin", name = "My Plugin", 
            version = "1.0.0", versionCode = 1, 
            description = "Description", author = "Author")
class MyPlugin : FeaturePlugin {
    override val manifest = PluginManifest(/* ... */)
    override fun initialize(context: PluginContext) {}
    override fun cleanup() {}
    override fun getMenuItems() = emptyList<PluginMenuItem>()
    override fun getScreens() = emptyList<PluginScreen>()
    override fun onReaderContext(context: ReaderContext) = null
}
```

## Minimal build.gradle.kts

```kotlin
plugins { id("ireader-plugin") }

pluginConfig {
    id.set("com.example.myplugin")
    name.set("My Plugin")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Description")
    author.set("Author")
    type.set(PluginType.FEATURE)
}
```

## Common Patterns

### HTTP Request
```kotlin
PluginHttpClient.create().use { client ->
    val response = client.getText("https://api.example.com")
}
```

### JSON Parsing
```kotlin
val data = JsonHelper.decode<MyClass>(jsonString)
val json = JsonHelper.encode(myObject)
```

### Error Handling
```kotlin
return try {
    Result.success(doWork())
} catch (e: Exception) {
    Result.failure(e)
}
```

### Accessing APIs
```kotlin
val analyticsApi = context.getApi(ReadingAnalyticsApi::class)
val characterApi = context.getApi(CharacterDatabaseApi::class)
```

## File Structure

```
plugins/features/my-plugin/
├── build.gradle.kts
└── src/main/kotlin/
    └── com/example/myplugin/
        └── MyPlugin.kt
```

## Monetization Options

| Type | Config | Description |
|------|--------|-------------|
| Free | `FREE` | No cost |
| Premium | `PREMIUM` + `price` | One-time purchase |
| Freemium | `FREEMIUM` | Free + paid features |

## Checklist Before Publishing

- [ ] Unique plugin ID (reverse domain)
- [ ] Version and versionCode set
- [ ] Description is clear
- [ ] Minimal permissions
- [ ] Error handling complete
- [ ] cleanup() releases resources
- [ ] Tested on target platforms
- [ ] No hardcoded secrets
