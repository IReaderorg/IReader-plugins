package io.github.ireaderorg.plugins.gradioparler

import ireader.plugin.api.*

/**
 * Parler TTS Plugin - Describe the voice you want via Gradio.
 */
class ParlerTTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-parler-tts",
        name = "Parler TTS (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "Describe the voice style you want using natural language.",
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
        context.log(LogLevel.INFO, "Parler TTS plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
