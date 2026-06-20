plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-zonos2")
    name.set("Zonos TTS v2 (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Zonos TTS v2 by Zyphra - High-quality TTS with voice cloning. Supports multiple languages.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiozonos2.Zonos2Plugin")
    tags.set(listOf("tts", "gradio", "zonos", "voice-cloning", "multilingual"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://multimodalart-zonos2.hf.space",
        "gradio.apiName" to "/generate",
        "gradio.apiType" to "GRADIO_API_CALL",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "en,fr,de,es,it,pt,ja,zh,ko",
        "gradio.params" to """[{"type":"audio","name":"speaker_audio"},{"type":"boolean","name":"accurate_mode","default":true},{"type":"boolean","name":"clean_background","default":false},{"type":"choice","name":"speaking_rate","choices":["Auto","0-8","8-11","11-14","14-17","17-21","21-28","28-40","40+"],"default":"Auto"},{"type":"number","name":"max_seconds","default":30,"min":1,"max":120},{"type":"number","name":"seed","default":42},{"type":"boolean","name":"randomize_seed","default":true},{"type":"float","name":"temperature","default":1.15,"min":0.1,"max":2.0},{"type":"float","name":"top_k","default":106,"min":1,"max":200},{"type":"float","name":"min_p","default":0.18,"min":0.0,"max":1.0},{"type":"float","name":"repetition_penalty","default":1.2,"min":1.0,"max":2.0}]"""
    ))
}
