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
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "XTTS v2 plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
