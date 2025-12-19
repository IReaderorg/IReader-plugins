package io.github.ireaderorg.plugins.pipertts

import ireader.plugin.api.*

/**
 * Piper TTS Plugin for Desktop.
 * 
 * This plugin bundles the standalone Piper TTS executable for all desktop platforms.
 * The host app extracts and runs piper as a subprocess to provide neural TTS support.
 * 
 * Bundled files (in native/ folder):
 * 
 * Windows (native/windows-x64/):
 * - piper.exe (main executable)
 * - espeak-ng-data/ (phoneme data)
 * - *.dll (runtime libraries)
 * 
 * macOS x64 (native/macos-x64/):
 * - piper (main executable)
 * - espeak-ng-data/ (phoneme data)
 * - *.dylib (runtime libraries)
 * 
 * macOS ARM64 (native/macos-arm64/):
 * - piper (main executable)
 * - espeak-ng-data/ (phoneme data)
 * - *.dylib (runtime libraries)
 * 
 * Linux x64 (native/linux-x64/):
 * - piper (main executable)
 * - espeak-ng-data/ (phoneme data)
 * - *.so (runtime libraries)
 * 
 * The host app should:
 * 1. Extract the appropriate files for the current platform
 * 2. Run piper as a subprocess with model and text input
 * 3. Capture the audio output
 */
class PiperTTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.piper-tts",
        name = "Piper TTS",
        version = "2.0.0",
        versionCode = 3,
        description = "Piper neural TTS standalone executable for Desktop. High-quality offline text-to-speech.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.TTS,
        permissions = listOf(PluginPermission.STORAGE, PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.DESKTOP),
        nativeLibraries = mapOf(
            "windows-x64" to listOf("native/windows-x64/piper.exe"),
            "macos-x64" to listOf("native/macos-x64/piper"),
            "macos-arm64" to listOf("native/macos-arm64/piper"),
            "linux-x64" to listOf("native/linux-x64/piper")
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Piper TTS standalone plugin initialized")
        context.log(LogLevel.INFO, "Piper executable available at: native/<platform>/piper")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
