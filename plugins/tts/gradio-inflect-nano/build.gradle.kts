plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-inflect-nano")
    name.set("Inflect Nano v1 (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Inflect Nano v1 - Tiny on-device TTS model running entirely on CPU. Fast and lightweight.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradi.InflectNanoPlugin")
    tags.set(listOf("tts", "gradio", "inflect", "nano", "lightweight", "cpu"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://luigi-inflect-nano-v1-demo.hf.space",
        "gradio.apiName" to "/generate",
        "gradio.apiType" to "GRADIO_API_CALL",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "en",
        "gradio.params" to """[{"type":"text","name":"text"}]"""
    ))
}
