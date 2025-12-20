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
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://styletts2-styletts2.hf.space",
            "gradio.apiName" to "/predict",
            "gradio.apiType" to "GRADIO_API",
            "gradio.audioOutputIndex" to "0",
            "gradio.languages" to "en",
            "gradio.params" to """[{"type":"text","name":"text"},{"type":"float","name":"alpha","default":0.3,"min":0.0,"max":1.0},{"type":"float","name":"beta","default":0.7,"min":0.0,"max":1.0},{"type":"float","name":"diffusion_steps","default":5.0,"min":1.0,"max":20.0}]"""
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "StyleTTS 2 plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
