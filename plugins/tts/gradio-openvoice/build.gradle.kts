plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-openvoice")
    name.set("OpenVoice (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Voice cloning TTS with emotion control. Supports multiple styles like whispering, shouting, excited, and more.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradioopenvoice.OpenVoicePlugin")
    tags.set(listOf("tts", "gradio", "openvoice", "voice-cloning", "emotion", "style"))
}
