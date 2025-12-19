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
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://parler-tts-parler-tts-mini.hf.space",
            "gradio.apiName" to "/predict",
            "gradio.apiType" to "GRADIO_API",
            "gradio.audioOutputIndex" to "0",
            "gradio.params" to """[
                {"type":"text","name":"text"},
                {"type":"string","name":"description","default":"A female speaker with a clear and pleasant voice"}
            ]""",
            "gradio.languages" to "en"
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Parler TTS Gradio plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
