package io.github.ireaderorg.plugins.gradiostyletts2

import ireader.plugin.api.*

/**
 * StyleTTS 2 Plugin - High-quality expressive TTS with style control.
 */
class StyleTTS2Plugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-style-tts-2",
        name = "StyleTTS 2 (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "High-quality expressive TTS with style control parameters.",
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
        context.log(LogLevel.INFO, "StyleTTS 2 plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
