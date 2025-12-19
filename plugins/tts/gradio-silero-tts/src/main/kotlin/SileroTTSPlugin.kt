package io.github.ireaderorg.plugins.gradiosilero

import ireader.plugin.api.*

/**
 * Silero TTS Plugin - Fast and lightweight TTS with multiple languages.
 */
class SileroTTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-silero-tts",
        name = "Silero TTS (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "Fast and lightweight TTS with multiple languages.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://silero-silero-tts.hf.space",
            "gradio.apiName" to "/predict",
            "gradio.apiType" to "GRADIO_API",
            "gradio.audioOutputIndex" to "0",
            "gradio.params" to """[
                {"type":"text","name":"text"},
                {"type":"choice","name":"language","choices":["en","de","es","fr","ru","ua","uz","xal","indic"],"default":"en"},
                {"type":"choice","name":"speaker","choices":["en_0","en_1","en_2","en_3","en_4"],"default":"en_0"}
            ]""",
            "gradio.languages" to "en,de,es,fr,ru,ua,uz,xal,indic"
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Silero TTS Gradio plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
