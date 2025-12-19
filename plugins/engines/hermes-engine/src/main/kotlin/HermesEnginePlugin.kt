package io.github.ireaderorg.plugins.hermesengine

import ireader.plugin.api.*

/**
 * Hermes JavaScript Engine Plugin for Android.
 * 
 * This plugin uses the Player UI Hermes wrapper (com.intuit.playerui:hermes-android)
 * which provides a standalone Hermes runtime with a clean Kotlin API.
 * 
 * Benefits over J2V8/V8:
 * - Smaller binary size (~2-3MB vs ~8-10MB per ABI)
 * - Faster startup time (bytecode precompilation)
 * - Lower memory footprint
 * - Full ES6+ support
 * - Actively maintained
 * 
 * Trade-offs:
 * - No JIT compilation (interpreter-only), so raw execution is slower
 * - But for DOM parsing and API calls (typical plugin use), it's fast enough
 */
class HermesEnginePlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    private var nativeLibraryLoaded = false
    private var loadError: String? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.hermes-engine",
        name = "Hermes JavaScript Engine",
        version = "0.12.0",
        versionCode = 2,
        description = "Hermes JS engine for Android. Smaller size, faster startup, lower memory than V8. Full ES6+ support.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.JS_ENGINE,
        permissions = listOf(PluginPermission.STORAGE),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID),
        // All native libraries needed for Hermes
        nativeLibraries = mapOf(
            "android-arm64" to listOf(
                "native/android/arm64-v8a/libc++_shared.so",
                "native/android/arm64-v8a/libfbjni.so",
                "native/android/arm64-v8a/libjsi.so",
                "native/android/arm64-v8a/libhermes.so",
                "native/android/arm64-v8a/libhermes_jni.so"
            ),
            "android-arm32" to listOf(
                "native/android/armeabi-v7a/libc++_shared.so",
                "native/android/armeabi-v7a/libfbjni.so",
                "native/android/armeabi-v7a/libjsi.so",
                "native/android/armeabi-v7a/libhermes.so",
                "native/android/armeabi-v7a/libhermes_jni.so"
            ),
            "android-x64" to listOf(
                "native/android/x86_64/libc++_shared.so",
                "native/android/x86_64/libfbjni.so",
                "native/android/x86_64/libjsi.so",
                "native/android/x86_64/libhermes.so",
                "native/android/x86_64/libhermes_jni.so"
            ),
            "android-x86" to listOf(
                "native/android/x86/libc++_shared.so",
                "native/android/x86/libfbjni.so",
                "native/android/x86/libjsi.so",
                "native/android/x86/libhermes.so",
                "native/android/x86/libhermes_jni.so"
            )
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Hermes plugin initialized (native libraries will be loaded on demand)")
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
     * Load all native libraries from the given directory.
     * Libraries must be loaded in dependency order.
     * 
     * @param nativeDir Directory containing the native libraries
     * @return true if loaded successfully, false otherwise
     */
    fun loadNativeLibraries(nativeDir: String): Boolean {
        if (nativeLibraryLoaded) {
            return true
        }
        
        fun log(message: String) {
            println("[HermesPlugin] $message")
            pluginContext?.log(LogLevel.INFO, message)
        }
        
        fun logError(message: String) {
            println("[HermesPlugin] ERROR: $message")
            pluginContext?.log(LogLevel.ERROR, message)
        }
        
        try {
            log("Loading native libraries from: $nativeDir")
            log("Plugin ClassLoader: ${this.javaClass.classLoader?.javaClass?.name}")
            
            val dir = java.io.File(nativeDir)
            if (!dir.exists() || !dir.isDirectory) {
                loadError = "Native library directory does not exist: $nativeDir"
                logError(loadError!!)
                return false
            }
            
            // Load libraries in dependency order
            val libraryOrder = listOf(
                "libc++_shared.so",
                "libfbjni.so",
                "libjsi.so",
                "libhermes.so",
                "libhermes_jni.so"
            )
            
            for (libName in libraryOrder) {
                val libFile = java.io.File(dir, libName)
                if (!libFile.exists()) {
                    log("Library not found (may be optional): $libName")
                    continue
                }
                
                log("Loading: $libName (${libFile.length()} bytes)")
                try {
                    System.load(libFile.absolutePath)
                    log("Loaded: $libName")
                } catch (e: UnsatisfiedLinkError) {
                    // Some libraries might already be loaded or not needed
                    log("Could not load $libName: ${e.message}")
                }
            }
            
            // Verify HermesRuntime class is available
            val myClassLoader = this.javaClass.classLoader
            log("Verifying HermesRuntime class...")
            
            val hermesRuntimeClass = try {
                myClassLoader?.loadClass("com.intuit.playerui.hermes.bridge.runtime.HermesRuntime")
            } catch (e: ClassNotFoundException) {
                log("HermesRuntime class not found: ${e.message}")
                null
            }
            
            if (hermesRuntimeClass != null) {
                log("HermesRuntime class found: ${hermesRuntimeClass.name}")
                
                // Try to create a test runtime
                try {
                    val companionField = hermesRuntimeClass.getDeclaredField("Companion")
                    val companion = companionField.get(null)
                    
                    // Find the create method - it might have different signatures
                    val createMethods = companion.javaClass.methods.filter { it.name == "create" }
                    log("Found ${createMethods.size} create methods")
                    
                    for (method in createMethods) {
                        log("  - ${method.name}(${method.parameterTypes.joinToString { it.simpleName }})")
                    }
                    
                    // Try the no-arg version first
                    val createMethod = createMethods.find { it.parameterCount == 0 }
                        ?: createMethods.firstOrNull()
                    
                    if (createMethod != null) {
                        log("Creating test runtime...")
                        val runtime = if (createMethod.parameterCount == 0) {
                            createMethod.invoke(companion)
                        } else {
                            // Skip test if we can't figure out the parameters
                            log("Skipping test runtime creation - complex constructor")
                            null
                        }
                        
                        if (runtime != null) {
                            log("Test runtime created successfully!")
                            
                            // Try to close it
                            val closeMethod = hermesRuntimeClass.getMethod("close")
                            closeMethod.invoke(runtime)
                            log("Test runtime closed")
                        }
                    }
                } catch (e: Exception) {
                    log("Could not create test runtime (may still work): ${e.message}")
                    e.printStackTrace()
                }
            } else {
                loadError = "HermesRuntime class not found in plugin ClassLoader"
                logError(loadError!!)
                return false
            }
            
            nativeLibraryLoaded = true
            log("Hermes native libraries loaded successfully!")
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
    
    /**
     * Legacy method for compatibility - loads from a single library path.
     */
    fun loadNativeLibrary(libraryPath: String): Boolean {
        // Extract directory from path
        val libFile = java.io.File(libraryPath)
        val nativeDir = libFile.parentFile?.absolutePath ?: return false
        return loadNativeLibraries(nativeDir)
    }
}
