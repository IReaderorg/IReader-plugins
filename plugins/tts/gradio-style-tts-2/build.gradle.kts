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
}
