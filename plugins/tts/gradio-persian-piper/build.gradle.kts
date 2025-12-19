plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-persian-piper")
    name.set("Persian TTS (Piper - Basic)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Persian language TTS using Piper voices via Gradio (basic quality).")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiopersianpiper.PersianPiperTTSPlugin")
    tags.set(listOf("tts", "gradio", "persian", "farsi", "piper"))
}
