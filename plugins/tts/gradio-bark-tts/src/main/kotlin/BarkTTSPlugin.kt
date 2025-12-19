package io.github.ireaderorg.plugins.gradiobark

import ireader.plugin.api.*

/**
 * Bark TTS Plugin - Suno's text-to-audio model via Gradio.
 */
class BarkTTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-bark-tts",
        name = "Bark TTS (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "High-quality generative TTS by Suno. Multiple speaker presets available.",
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
        context.log(LogLevel.INFO, "Bark TTS plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
