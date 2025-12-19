plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-silero-tts")
    name.set("Silero TTS (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Fast and lightweight TTS with multiple languages including English, German, Spanish, French, Russian, and more.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiosilero.SileroTTSPlugin")
    tags.set(listOf("tts", "gradio", "silero", "fast", "lightweight", "multilingual"))
}
