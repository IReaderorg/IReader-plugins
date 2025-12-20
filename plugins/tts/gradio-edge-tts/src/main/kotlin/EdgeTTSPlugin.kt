package io.github.ireaderorg.plugins.gradioedgetts

import ireader.plugin.api.*

/**
 * Edge TTS Plugin - Microsoft Edge's neural TTS via Gradio.
 * Provides high-quality voices for multiple languages.
 */
class EdgeTTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-edge-tts",
        name = "Edge TTS (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "Microsoft Edge TTS with multiple voices via Gradio. High-quality neural voices.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://innoai-edge-tts-text-to-speech.hf.space",
            "gradio.apiName" to "/predict",
            "gradio.apiType" to "GRADIO_API",
            "gradio.audioOutputIndex" to "0",
            "gradio.languages" to "en,en-US,en-GB,en-AU",
            "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"voice","choices":["en-US-AriaNeural","en-US-GuyNeural","en-GB-SoniaNeural","en-AU-NatashaNeural"],"default":"en-US-AriaNeural"},{"type":"speed","name":"rate","default":1.0,"min":0.5,"max":2.0}]"""
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Edge TTS Gradio plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
