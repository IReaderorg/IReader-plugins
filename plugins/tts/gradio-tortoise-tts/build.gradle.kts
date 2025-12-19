plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-tortoise-tts")
    name.set("Tortoise TTS (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("High-quality TTS with many voice options. Slower but produces excellent results.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiotortoise.TortoiseTTSPlugin")
    tags.set(listOf("tts", "gradio", "tortoise", "high-quality", "voices"))
}
