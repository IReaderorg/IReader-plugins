package io.github.ireaderorg.plugins.gradi

import ireader.plugin.api.*

/**
 * Inflect Nano v1 Plugin - Tiny on-device TTS model.
 * 
 * Inflect Nano is a lightweight TTS model that runs entirely on CPU.
 * - Fast inference
 * - Low resource usage
 * - English only
 * 
 * Uses the Luigi/Inflect-Nano-v1-demo Hugging Face space.
 */
class InflectNanoPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-inflect-nano",
        name = "Inflect Nano v1 (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "Inflect Nano v1 - Tiny on-device TTS model running entirely on CPU. Fast and lightweight.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://luigi-inflect-nano-v1-demo.hf.space",
            "gradio.apiName" to "/generate",
            "gradio.apiType" to "GRADIO_API_CALL",
            "gradio.audioOutputIndex" to "0",
            "gradio.languages" to "en",
            "gradio.params" to """[{"type":"text","name":"text"}]"""
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Inflect Nano v1 plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
