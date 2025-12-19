package io.github.ireaderorg.plugins.gradiosilero

import ireader.plugin.api.*

/**
 * Silero TTS Plugin - Fast and lightweight TTS with multiple languages.
 */
class SileroTTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-silero-tts",
        name = "Silero TTS (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "Fast and lightweight TTS with multiple languages.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Silero TTS plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
