package io.github.ireaderorg.plugins.gradiovoxcpm

import ireader.plugin.api.*

/**
 * VoxCPM TTS Plugin - High-quality text-to-speech with voice cloning and emotional control.
 * 
 * VoxCPM is a generative TTS model that supports:
 * - Voice cloning from reference audio
 * - Emotional/style control via instructions
 * - Chinese and English languages
 * - High-quality audio output
 * 
 * Uses the openbmb/VoxCPM-Demo Hugging Face space.
 */
class VoxCPMPlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.gradio-voxcpm-tts",
        name = "VoxCPM TTS (Gradio)",
        version = "1.0.0",
        versionCode = 1,
        description = "VoxCPM - High-quality TTS with voice cloning and emotional control. Supports Chinese and English with natural-sounding voices.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.GRADIO_TTS,
        permissions = listOf(PluginPermission.NETWORK),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP),
        metadata = mapOf(
            "gradio.spaceUrl" to "https://openbmb-voxcpm-demo.hf.space",
            "gradio.apiName" to "/generate",
            "gradio.apiType" to "GRADIO_API_CALL",
            "gradio.audioOutputIndex" to "0",
            "gradio.languages" to "zh,en",
            "gradio.params" to """[{"type":"text","name":"text_input"},{"type":"string","name":"control_instruction","default":"A young girl with a soft, sweet voice."},{"type":"audio","name":"reference_wav_path_input"},{"type":"boolean","name":"use_prompt_text","default":false},{"type":"string","name":"prompt_text_input","default":""},{"type":"number","name":"cfg_value_input","default":2.0,"min":1.0,"max":3.0},{"type":"boolean","name":"do_normalize","default":false},{"type":"boolean","name":"denoise","default":false}]"""
        )
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "VoxCPM TTS plugin initialized")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
