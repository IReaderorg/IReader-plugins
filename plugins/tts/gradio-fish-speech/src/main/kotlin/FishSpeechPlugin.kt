package io.github.ireaderorg.plugins.gradiofishspeech

import ireader.plugin.api.*

/**
 * Fish Speech Plugin - Fast multilingual TTS with natural prosody.
 */
class FishSpeechPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-fish-speech",
        name = "Fish Speech (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "Fast multilingual TTS with natural prosody.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://fishaudio-fish-speech-1.hf.space",
            "gradio.apiName" to "/predict",
            "gradio.apiType" to "GRADIO_API",
            "gradio.audioOutputIndex" to "0",
            "gradio.languages" to "en,zh,ja,ko",
            "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"language","choices":["en","zh","ja","ko"],"default":"en"}]"""
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Fish Speech plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
