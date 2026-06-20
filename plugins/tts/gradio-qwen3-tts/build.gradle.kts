plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-qwen3-tts")
    name.set("Qwen3 TTS (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Qwen3 TTS - High-quality TTS with voice design, voice clone, and custom voice modes.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradioqwen3tts.Qwen3TTSPlugin")
    tags.set(listOf("tts", "gradio", "qwen", "voice-design", "voice-clone", "multilingual"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://qwen-qwen3-tts.hf.space",
        "gradio.apiName" to "/generate_voice_design",
        "gradio.apiType" to "GRADIO_API_CALL",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "en,zh,ja,ko,fr,de,es,pt,ru",
        "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"language","choices":["Auto","Chinese","English","Japanese","Korean","French","German","Spanish","Portuguese","Russian"],"default":"Auto"},{"type":"string","name":"voice_description","default":"Speak clearly and naturally"}]"""
    ))
}
