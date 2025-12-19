package io.github.ireaderorg.plugins.gradiopersianpiper

import ireader.plugin.api.*

/**
 * Persian Piper TTS Plugin - Basic quality Persian TTS via Gradio.
 */
class PersianPiperTTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-persian-piper",
        name = "Persian TTS (Piper - Basic)",
        version = "1.0.0",
        versionCode = 1,
        description = "Persian language TTS using Piper voices (basic quality).",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://gyroing-persian-tts-piper.hf.space",
            "gradio.apiName" to "/synthesize_speech",
            "gradio.apiType" to "GRADIO_API_CALL",
            "gradio.audioOutputIndex" to "0",
            "gradio.params" to """[
                {"type":"text","name":"text"}
            ]""",
            "gradio.languages" to "fa,fa-IR"
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Persian Piper TTS Gradio plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
