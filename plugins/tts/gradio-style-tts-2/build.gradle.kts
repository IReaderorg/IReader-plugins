plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-style-tts-2")
    name.set("StyleTTS 2 (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("High-quality expressive TTS with style control. Adjust alpha, beta, and diffusion steps.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiostyletts2.StyleTTS2Plugin")
    tags.set(listOf("tts", "gradio", "styletts", "expressive", "style-control"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://styletts2-styletts2.hf.space",
        "gradio.apiName" to "/predict",
        "gradio.apiType" to "GRADIO_API",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "en",
        "gradio.params" to """[{"type":"text","name":"text"},{"type":"float","name":"alpha","default":0.3,"min":0.0,"max":1.0},{"type":"float","name":"beta","default":0.7,"min":0.0,"max":1.0},{"type":"float","name":"diffusion_steps","default":5.0,"min":1.0,"max":20.0}]"""
    ))
}
