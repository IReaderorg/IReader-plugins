plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-persian-xtts")
    name.set("Persian TTS (XTTS v2 - Voice Clone)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Persian TTS with XTTS v2 - supports voice cloning for personalized voices.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiopersianxtts.PersianXTTSPlugin")
    tags.set(listOf("tts", "gradio", "persian", "farsi", "xtts", "voice-cloning"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://coqui-xtts.hf.space",
        "gradio.apiName" to "/predict",
        "gradio.apiType" to "GRADIO_API",
        "gradio.audioOutputIndex" to "0",
        "gradio.supportsVoiceCloning" to "true",
        "gradio.languages" to "fa,fa-IR",
        "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"language","choices":["fa"],"default":"fa"}]"""
    ))
}
