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
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Tortoise TTS plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
