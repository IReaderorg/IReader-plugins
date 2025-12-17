package io.github.ireaderorg.plugins.j2v8engine

import ireader.plugin.api.*

/**
 * J2V8 JavaScript Engine Plugin for Android.
 * 
 * This plugin bundles the J2V8 native libraries (~120MB total for all ABIs).
 * The host app extracts and loads these libraries to provide V8 JS engine support.
 * 
 * Bundled native libraries:
 * - native/android/arm64-v8a/libj2v8.so (~32MB)
 * - native/android/armeabi-v7a/libj2v8.so (~24MB)
 * - native/android/x86_64/libj2v8.so (~33MB)
 * - native/android/x86/libj2v8.so (~30MB)
 * 
 * The host app should:
 * 1. Extract the appropriate .so file for the device ABI
 * 2. Load it via System.loadLibrary() or System.load()
 * 3. Use the J2V8 Java API (com.eclipsesource.v8.*)
 */
class J2V8EnginePlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.j2v8-engine",
        name = "J2V8 JavaScript Engine",
        version = "6.2.1",
        versionCode = 1,
        description = "V8 JavaScript engine native libraries for Android. Enables LNReader-compatible JS sources.",
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
        context.log(LogLevel.INFO, "J2V8 native library plugin initialized")
        context.log(LogLevel.INFO, "Native libraries available at: native/android/<abi>/libj2v8.so")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
