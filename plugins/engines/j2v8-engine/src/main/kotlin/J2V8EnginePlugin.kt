package io.github.ireaderorg.plugins.j2v8engine

import ireader.plugin.api.*

/**
 * J2V8 JavaScript Engine Plugin for Android.
 * 
 * This plugin bundles the J2V8 native libraries and Java API.
 * It provides a loadNativeLibrary() method that loads the native library
 * from within this plugin's ClassLoader context.
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
     * IMPORTANT: This method loads the native library from within this plugin's
     * ClassLoader context. The System.load() call uses this class's ClassLoader,
     * which is the DexClassLoader that contains the J2V8 classes.
     * 
     * @param libraryPath Full path to the libj2v8.so file
     * @return true if loaded successfully, false otherwise
     */
    fun loadNativeLibrary(libraryPath: String): Boolean {
        if (nativeLibraryLoaded) {
            return true
        }
        
        // Use println for logging since pluginContext might be null
        // (this method can be called via reflection on a new instance)
        fun log(message: String) {
            println("[J2V8Plugin] $message")
            pluginContext?.log(LogLevel.INFO, message)
        }
        
        fun logError(message: String) {
            println("[J2V8Plugin] ERROR: $message")
            pluginContext?.log(LogLevel.ERROR, message)
        }
        
        try {
            log("Loading native library: $libraryPath")
            log("Plugin ClassLoader (this.javaClass.classLoader): ${this.javaClass.classLoader?.javaClass?.name}")
            
            // Verify the library file exists
            val libraryFile = java.io.File(libraryPath)
            if (!libraryFile.exists()) {
                loadError = "Native library file does not exist: $libraryPath"
                logError(loadError!!)
                return false
            }
            log("Native library file exists, size: ${libraryFile.length()} bytes")
            
            // Load the native library using absolute path
            // This MUST be called before any J2V8 class is loaded, because J2V8 classes
            // have static initializers that try to load the native library using System.loadLibrary()
            log("Calling System.load($libraryPath)...")
            System.load(libraryPath)
            log("System.load() completed successfully!")
            
            // Now the native library is loaded. We can safely use J2V8 classes.
            // Use THIS class's ClassLoader to find V8 class
            val myClassLoader = this.javaClass.classLoader
            log("Attempting to load V8 class from ClassLoader: ${myClassLoader?.javaClass?.name}")
            
            // Load V8 class using the plugin's ClassLoader
            val v8Class = myClassLoader?.loadClass("com.eclipsesource.v8.V8")
            if (v8Class == null) {
                loadError = "V8 class not found in plugin ClassLoader"
                logError(loadError!!)
                return false
            }
            log("V8 class loaded: ${v8Class.name}")
            
            // Try to create a test runtime to verify everything works
            log("Creating test V8 runtime...")
            val createMethod = v8Class.getMethod("createV8Runtime")
            val runtime = createMethod.invoke(null)
            log("V8 runtime created successfully!")
            
            // Release the test runtime
            val releaseMethod = v8Class.getMethod("release")
            releaseMethod.invoke(runtime)
            log("V8 runtime released")
            
            nativeLibraryLoaded = true
            log("J2V8 native library loaded and verified successfully!")
            return true
            
        } catch (e: UnsatisfiedLinkError) {
            loadError = "Native library load failed: ${e.message}"
            logError(loadError!!)
            e.printStackTrace()
            return false
        } catch (e: Exception) {
            loadError = "Initialization failed: ${e.javaClass.simpleName}: ${e.message}"
            logError(loadError!!)
            e.printStackTrace()
            return false
        }
    }
}
