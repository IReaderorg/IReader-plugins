package io.github.ireaderorg.plugins.gradiotortoise

import ireader.plugin.api.*

/**
 * Tortoise TTS Plugin - High-quality TTS with many voice options (slower).
 */
class TortoiseTTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-tortoise-tts",
        name = "Tortoise TTS (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "High-quality TTS with many voice options. Slower but excellent results.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://jbetker-tortoise-tts.hf.space",
            "gradio.apiName" to "/predict",
            "gradio.apiType" to "GRADIO_API",
            "gradio.audioOutputIndex" to "0",
            "gradio.params" to """[
                {"type":"text","name":"text"},
                {"type":"choice","name":"voice","choices":["random","angie","deniro","freeman","halle","lj","myself","pat","snakes","tom","train_atkins","train_daws","train_dotrice","train_dreams","train_empire","train_grace","train_kennard","train_lescault","train_mouse","weaver","william"],"default":"random"},
                {"type":"choice","name":"preset","choices":["ultra_fast","fast","standard","high_quality"],"default":"fast"}
            ]""",
            "gradio.languages" to "en"
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Tortoise TTS Gradio plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
