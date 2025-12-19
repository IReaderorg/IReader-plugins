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
}
