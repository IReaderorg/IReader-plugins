plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-persian-chatterbox")
    name.set("Persian TTS (Chatterbox - Natural)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("High-quality Persian TTS using Chatterbox neural model - natural and expressive speech.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiopersianchatterbox.PersianChatterboxTTSPlugin")
    tags.set(listOf("tts", "gradio", "persian", "farsi", "chatterbox", "neural"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://gyroing-chatterbox-tts-persian-farsi.hf.space",
        "gradio.apiName" to "/generate_audio",
        "gradio.apiType" to "GRADIO_API_CALL",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "fa,fa-IR",
        "gradio.params" to """[{"type":"text","name":"text"}]"""
    ))
}
