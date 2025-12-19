package io.github.ireaderorg.plugins.gradioopenvoice

import ireader.plugin.api.*

/**
 * OpenVoice Plugin - Voice cloning TTS with emotion control.
 */
class OpenVoicePlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-openvoice",
        name = "OpenVoice (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "Voice cloning TTS with emotion control. Multiple styles available.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://myshell-ai-openvoice.hf.space",
            "gradio.apiName" to "/predict",
            "gradio.apiType" to "GRADIO_API",
            "gradio.audioOutputIndex" to "0",
            "gradio.supportsVoiceCloning" to "true",
            "gradio.params" to """[
                {"type":"text","name":"text"},
                {"type":"choice","name":"style","choices":["default","whispering","shouting","excited","cheerful","terrified","angry","sad","friendly"],"default":"default"},
                {"type":"choice","name":"language","choices":["EN","ES","FR","ZH","JP","KR"],"default":"EN"}
            ]""",
            "gradio.languages" to "en,es,fr,zh,ja,ko"
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "OpenVoice Gradio plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
