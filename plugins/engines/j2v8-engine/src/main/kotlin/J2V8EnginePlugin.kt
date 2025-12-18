package io.github.ireaderorg.plugins.j2v8engine

import ireader.plugin.api.*

/**
 * J2V8 JavaScript Engine Plugin for Android.
 * 
 * This plugin bundles the J2V8 native libraries and Java API.
 * It provides a loadNativeLibrary() method that MUST be called from within
 * this plugin's context to ensure JNI FindClass uses the correct ClassLoader.
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
        context.log(LogLevel.INFO, "J2V8 plugin initialized (native library will be loaded on demand)")
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
    
    /**
     * Load the native library from the given path.
     * 
     * IMPORTANT: This method MUST be called to load the native library because
     * System.load() uses the ClassLoader of the calling class. When this method
     * is called, the calling class is J2V8EnginePlugin which is loaded by the
     * plugin's DexClassLoader, so JNI FindClass will find V8 classes correctly.
     * 
     * @param libraryPath Full path to the libj2v8.so file
     * @return true if loaded successfully, false otherwise
     */
    fun loadNativeLibrary(libraryPath: String): Boolean {
        if (nativeLibraryLoaded) {
            return true
        }
        
        try {
            pluginContext?.log(LogLevel.INFO, "Loading native library from plugin context: $libraryPath")
            
            // This System.load() call happens from within the plugin's ClassLoader context
            // JNI_OnLoad will use this ClassLoader to find com.eclipsesource.v8.V8
            System.load(libraryPath)
            
            // Verify the library loaded correctly
            val v8Class = Class.forName("com.eclipsesource.v8.V8")
            pluginContext?.log(LogLevel.INFO, "V8 class loaded: ${v8Class.name}")
            
            // Try to create a test runtime
            val createMethod = v8Class.getMethod("createV8Runtime")
            val runtime = createMethod.invoke(null)
            
            // Release it
            val releaseMethod = v8Class.getMethod("release")
            releaseMethod.invoke(runtime)
            
            nativeLibraryLoaded = true
            pluginContext?.log(LogLevel.INFO, "J2V8 native library loaded and verified successfully")
            return true
            
        } catch (e: UnsatisfiedLinkError) {
            loadError = "Native library load failed: ${e.message}"
            pluginContext?.log(LogLevel.ERROR, loadError!!)
            return false
        } catch (e: Exception) {
            loadError = "Initialization failed: ${e.message}"
            pluginContext?.log(LogLevel.ERROR, loadError!!)
            return false
        }
    }
}
