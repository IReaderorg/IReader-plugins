package io.github.ireaderorg.plugins.pipertts

import ireader.plugin.api.*

/**
 * Piper TTS Plugin for Desktop.
 * 
 * This plugin bundles the Piper TTS native libraries for all desktop platforms.
 * The host app extracts and loads these libraries to provide neural TTS support.
 * 
 * Bundled native libraries (in native/ folder):
 * 
 * Windows (native/windows-x64/):
 * - piper-jni.dll, piper_phonemize.dll, onnxruntime.dll, espeak-ng.dll
 * 
 * macOS x64 (native/macos-x64/):
 * - libpiper-jni.dylib, libpiper_phonemize.1.dylib, libonnxruntime.1.14.1.dylib, libespeak-ng.1.dylib
 * 
 * macOS ARM64 (native/macos-arm64/):
 * - libpiper-jni.dylib, libpiper_phonemize.1.dylib, libonnxruntime.1.14.1.dylib, libespeak-ng.1.dylib
 * 
 * Linux x64 (native/linux-x64/):
 * - libpiper-jni.so
 * 
 * Linux ARM64 (native/linux-arm64/):
 * - libpiper-jni.so
 * 
 * The host app should:
 * 1. Extract the appropriate native libraries for the current platform
 * 2. Load them via System.loadLibrary() or System.load()
 * 3. Use the Piper JNI API (io.github.givimad.piperjni.*)
 */
class PiperTTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.piper-tts",
        name = "Piper TTS",
        version = "1.2.0",
        versionCode = 1,
        description = "Piper neural TTS native libraries for Desktop. High-quality offline text-to-speech.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.TTS,
        permissions = listOf(PluginPermission.STORAGE, PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.DESKTOP),
        nativeLibraries = mapOf(
            "windows-x64" to listOf(
                "native/windows-x64/piper-jni.dll",
                "native/windows-x64/piper_phonemize.dll",
                "native/windows-x64/onnxruntime.dll",
                "native/windows-x64/espeak-ng.dll"
            ),
            "macos-x64" to listOf(
                "native/macos-x64/libpiper-jni.dylib",
                "native/macos-x64/libpiper_phonemize.1.dylib",
                "native/macos-x64/libonnxruntime.1.14.1.dylib",
                "native/macos-x64/libespeak-ng.1.dylib"
            ),
            "macos-arm64" to listOf(
                "native/macos-arm64/libpiper-jni.dylib",
                "native/macos-arm64/libpiper_phonemize.1.dylib",
                "native/macos-arm64/libonnxruntime.1.14.1.dylib",
                "native/macos-arm64/libespeak-ng.1.dylib"
            ),
            "linux-x64" to listOf("native/linux-x64/libpiper-jni.so"),
            "linux-arm64" to listOf("native/linux-arm64/libpiper-jni.so")
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Piper TTS native library plugin initialized")
        context.log(LogLevel.INFO, "Native libraries available at: native/<platform>/")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
