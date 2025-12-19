package io.github.ireaderorg.plugins.gradioxttsv2

import ireader.plugin.api.*

/**
 * XTTS v2 Plugin - Coqui's latest multilingual TTS with voice cloning.
 */
class XTTSv2Plugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-xtts-v2",
        name = "XTTS v2 (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "Coqui's latest multilingual TTS with voice cloning. 15+ languages supported.",
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
            "gradio.params" to """[
                {"type":"text","name":"text"},
                {"type":"choice","name":"language","choices":["en","es","fr","de","it","pt","pl","tr","ru","nl","cs","ar","zh-cn","ja","ko"],"default":"en"}
            ]""",
            "gradio.languages" to "en,es,fr,de,it,pt,pl,tr,ru,nl,cs,ar,zh-cn,ja,ko"
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "XTTS v2 Gradio plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
