package io.github.ireaderorg.plugins.j2v8engine

import ireader.plugin.api.*

/**
 * J2V8 JavaScript Engine Plugin for Android.
 * 
 * This plugin bundles the J2V8 native libraries and Java API.
 * It handles its own native library loading during initialization.
 * 
 * The host app should:
 * 1. Load this plugin via PluginManager
 * 2. Use the plugin's ClassLoader to access J2V8 classes via reflection
 */
class J2V8EnginePlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    private var nativeLibraryLoaded = false
    private var loadError: String? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.j2v8-engine",
        name = "J2V8 JavaScript Engine",
        version = "6.2.1",
        versionCode = 1,
        description = "V8 JavaScript engine for Android. Enables LNReader-compatible JS sources.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.JS_ENGINE,
        permissions = listOf(PluginPermission.STORAGE),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID),
        nativeLibraries = mapOf(
            "android-arm64" to listOf("native/android/arm64-v8a/libj2v8.so"),
            "android-arm32" to listOf("native/android/armeabi-v7a/libj2v8.so"),
            "android-x64" to listOf("native/android/x86_64/libj2v8.so"),
            "android-x86" to listOf("native/android/x86/libj2v8.so")
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "J2V8 plugin initializing...")
        
        // Load native library during initialization using PluginContext
        try {
            // Extract native libraries to plugin's native directory
            val nativeDir = context.extractNativeLibraries()
            if (nativeDir == null) {
                throw IllegalStateException("No native libraries available for this platform")
            }
            context.log(LogLevel.INFO, "Native libraries extracted to: $nativeDir")
            
            // Load the j2v8 native library
            // This is called from within the plugin's ClassLoader context,
            // so JNI FindClass will use this ClassLoader to find J2V8 classes
            context.loadNativeLibrary("j2v8")
            
            // Verify the library loaded correctly by trying to access V8 class
            try {
                val v8Class = Class.forName("com.eclipsesource.v8.V8")
                context.log(LogLevel.INFO, "V8 class loaded: ${v8Class.name}")
                
                // Try to create a test runtime
                val createMethod = v8Class.getMethod("createV8Runtime")
                val runtime = createMethod.invoke(null)
                
                // Release it
                val releaseMethod = v8Class.getMethod("release")
                releaseMethod.invoke(runtime)
                
                nativeLibraryLoaded = true
                context.log(LogLevel.INFO, "J2V8 native library loaded and verified successfully")
            } catch (e: Exception) {
                throw IllegalStateException("J2V8 classes not accessible: ${e.message}", e)
            }
        } catch (e: Exception) {
            loadError = e.message
            context.log(LogLevel.ERROR, "Failed to load J2V8 native library: ${e.message}")
        }
    }
    
    override fun cleanup() {
        pluginContext = null
    }
    
    /**
     * Check if the native library is loaded and ready.
     */
    fun isReady(): Boolean = nativeLibraryLoaded
    
    /**
     * Get the error message if loading failed.
     */
    fun getLoadError(): String? = loadError
}
