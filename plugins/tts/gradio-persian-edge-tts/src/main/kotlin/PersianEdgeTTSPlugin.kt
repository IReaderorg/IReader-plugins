package io.github.ireaderorg.plugins.gradiopersianedgetts

import ireader.plugin.api.*

/**
 * Persian Edge TTS Plugin - Microsoft's neural TTS with Persian voices.
 */
class PersianEdgeTTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-persian-edge-tts",
        name = "Persian TTS (Edge - Premium)",
        version = "1.0.0",
        versionCode = 1,
        description = "Premium Persian TTS using Microsoft Edge neural voices - most natural and realistic.",
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
            "gradio.languages" to "fa,fa-IR",
            "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"voice","choices":["fa-IR-DilaraNeural","fa-IR-FaridNeural"],"default":"fa-IR-DilaraNeural"},{"type":"speed","name":"rate","default":1.0,"min":0.5,"max":2.0}]"""
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Persian Edge TTS plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
