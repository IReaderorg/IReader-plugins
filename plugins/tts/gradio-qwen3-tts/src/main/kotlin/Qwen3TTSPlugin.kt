package io.github.ireaderorg.plugins.gradioqwen3tts

import ireader.plugin.api.*

/**
 * Qwen3 TTS Plugin - High-quality text-to-speech with multiple modes.
 * 
 * Qwen3 TTS supports three modes:
 * 1. Voice Design - Generate speech from text with a voice description
 * 2. Voice Clone - Clone a voice from reference audio
 * 3. Custom Voice - Use predefined speaker voices
 * 
 * This plugin uses the Voice Design mode as the primary endpoint.
 * Uses the Qwen/Qwen3-TTS Hugging Face space.
 */
class Qwen3TTSPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-qwen3-tts",
        name = "Qwen3 TTS (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "Qwen3 TTS - High-quality TTS with voice design, voice clone, and custom voice modes.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://qwen-qwen3-tts.hf.space",
            "gradio.apiName" to "/generate_voice_design",
            "gradio.apiType" to "GRADIO_API_CALL",
            "gradio.audioOutputIndex" to "0",
            "gradio.languages" to "en,zh,ja,ko,fr,de,es,pt,ru",
            "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"language","choices":["Auto","Chinese","English","Japanese","Korean","French","German","Spanish","Portuguese","Russian"],"default":"Auto"},{"type":"string","name":"voice_description","default":"Speak clearly and naturally"}]"""
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "Qwen3 TTS plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
