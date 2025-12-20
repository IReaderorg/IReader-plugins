plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-parler-tts")
    name.set("Parler TTS (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Describe the voice you want! Natural language voice description for TTS.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradioparler.ParlerTTSPlugin")
    tags.set(listOf("tts", "gradio", "parler", "voice-description"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://parler-tts-parler-tts-mini.hf.space",
        "gradio.apiName" to "/predict",
        "gradio.apiType" to "GRADIO_API",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "en",
        "gradio.params" to """[{"type":"text","name":"text"},{"type":"string","name":"description","default":"A female speaker with a clear and pleasant voice"}]"""
    ))
}
