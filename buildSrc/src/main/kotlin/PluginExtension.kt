import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty

/**
 * Extension for configuring IReader plugins
 */
abstract class PluginExtension {
    /** Unique plugin identifier (e.g., "com.example.ocean-theme") */
    abstract val id: Property<String>
    
    /** Display name */
    abstract val name: Property<String>
    
    /** Semantic version (e.g., "1.0.0") */
    abstract val version: Property<String>
    
    /** Numeric version code for updates */
    abstract val versionCode: Property<Int>
    
    /** Brief description */
    abstract val description: Property<String>
    
    /** Author name */
    abstract val author: Property<String>
    
    /** Author email (optional) */
    abstract val authorEmail: Property<String>
    
    /** Author website (optional) */
    abstract val authorWebsite: Property<String>
    
    /** Plugin type: THEME, TTS, TRANSLATION, FEATURE */
    abstract val type: Property<PluginType>
    
    /** Required permissions */
    abstract val permissions: ListProperty<PluginPermission>
    
    /** Minimum IReader version required */
    abstract val minIReaderVersion: Property<String>
    
    /** Icon URL (optional) */
    abstract val iconUrl: Property<String>
    
    /** Monetization type (optional) */
    abstract val monetization: Property<PluginMonetizationType>
    
    /** Price for premium plugins */
    abstract val price: Property<Double>
    
    /** Currency for premium plugins */
    abstract val currency: Property<String>
    
    /** Trial days for premium plugins */
    abstract val trialDays: Property<Int>
    
    /** Fully qualified main class name (auto-detected if not specified) */
    abstract val mainClass: Property<String>
    
    /** Supported platforms (defaults to all: ANDROID, IOS, DESKTOP) */
    abstract val platforms: ListProperty<PluginPlatform>
    
    /** Whether this plugin is featured in the store */
    abstract val featured: Property<Boolean>
    
    /** Tags for categorization and search (e.g., "dark", "minimal", "colorful") */
    abstract val tags: ListProperty<String>
    
    /** 
     * Skip this plugin from the online repository.
     * Use for internal plugins, development plugins, or plugins that should not be publicly distributed
     * (e.g., Tachi Source Loaders, Reader Screens).
     */
    abstract val skipFromRepo: Property<Boolean>
    
    /** Optional reason why this plugin is skipped from the repo */
    abstract val skipFromRepoReason: Property<String>
    
    /**
     * Custom metadata for plugin-specific configuration.
     * A flexible key-value store for plugin-type-specific data.
     * 
     * For GRADIO_TTS plugins, use these keys:
     * - "gradio.spaceUrl": Hugging Face Space URL (required)
     * - "gradio.apiName": API endpoint name (e.g., "/predict")
     * - "gradio.apiType": API type (AUTO, GRADIO_API, GRADIO_API_CALL, etc.)
     * - "gradio.audioOutputIndex": Output index for audio (default: "0")
     * - "gradio.params": JSON array of parameter definitions
     * - "gradio.languages": Comma-separated language codes (e.g., "en,es,fr")
     * - "gradio.supportsVoiceCloning": "true" if voice cloning is supported
     */
    abstract val metadata: MapProperty<String, String>
}

enum class PluginPlatform {
    ANDROID,
    IOS,
    DESKTOP
}

// Alias for compatibility
typealias Platform = PluginPlatform

enum class PluginType {
    THEME,
    TTS,
    TRANSLATION,
    FEATURE,
    JS_ENGINE,
    GRADIO_TTS,
    TACHI_SOURCE_LOADER,
    READER_SCREEN,
    SOURCE_LOADER,
    CLOUDFLARE_BYPASS
}

enum class PluginPermission {
    NETWORK,
    STORAGE,
    READER_CONTEXT,
    LIBRARY_ACCESS,
    PREFERENCES,
    NOTIFICATIONS
}

enum class PluginMonetizationType {
    FREE,
    PREMIUM,
    FREEMIUM
}
