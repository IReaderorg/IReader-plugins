package io.github.ireaderorg.plugins.gradiopersianchatterbox

import ireader.plugin.api.*

/**
 * Persian Chatterbox TTS Plugin - High quality neural TTS for Persian.
 */
class PersianChatterboxTTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-persian-chatterbox",
        name = "Persian TTS (Chatterbox - Natural)",
        version = "1.0.0",
        versionCode = 1,
        description = "High-quality Persian TTS using Chatterbox neural model - natural and expressive.",
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
        context.log(LogLevel.INFO, "Persian Chatterbox TTS plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
