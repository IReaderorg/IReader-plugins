plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-bark-tts")
    name.set("Bark TTS (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("High-quality generative TTS by Suno. Supports multiple speaker presets.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiobark.BarkTTSPlugin")
    tags.set(listOf("tts", "gradio", "bark", "suno", "generative"))
}
