package io.github.ireaderorg.plugins.gradiopersianxtts

import ireader.plugin.api.*

/**
 * Persian XTTS Plugin - Voice cloning capable TTS with Persian support.
 */
class PersianXTTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-persian-xtts",
        name = "Persian TTS (XTTS v2 - Voice Clone)",
        version = "1.0.0",
        versionCode = 1,
        description = "Persian TTS with XTTS v2 - supports voice cloning for personalized voices.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://coqui-xtts.hf.space",
            "gradio.apiName" to "/predict",
            "gradio.apiType" to "GRADIO_API",
            "gradio.audioOutputIndex" to "0",
            "gradio.supportsVoiceCloning" to "true",
            "gradio.languages" to "fa,fa-IR",
            "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"language","choices":["fa"],"default":"fa"}]"""
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Persian XTTS plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
