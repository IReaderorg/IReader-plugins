plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-voxcpm-tts")
    name.set("VoxCPM TTS (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("VoxCPM - High-quality TTS with voice cloning and emotional control via Gradio. Supports Chinese and English.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiovoxcpm.VoxCPMPlugin")
    tags.set(listOf("tts", "gradio", "voxcpm", "voice-cloning", "emotional", "chinese", "english"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://openbmb-voxcpm-demo.hf.space",
        "gradio.apiName" to "/generate",
        "gradio.apiType" to "GRADIO_API_CALL",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "zh,en",
        "gradio.params" to """[{"type":"text","name":"text_input"},{"type":"string","name":"control_instruction","default":"A young girl with a soft, sweet voice."},{"type":"audio","name":"reference_wav_path_input"},{"type":"boolean","name":"use_prompt_text","default":false},{"type":"string","name":"prompt_text_input","default":""},{"type":"number","name":"cfg_value_input","default":2.0,"min":1.0,"max":3.0},{"type":"boolean","name":"do_normalize","default":false},{"type":"boolean","name":"denoise","default":false}]"""
    ))
}
