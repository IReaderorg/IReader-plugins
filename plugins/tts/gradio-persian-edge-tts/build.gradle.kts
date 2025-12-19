plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-persian-edge-tts")
    name.set("Persian TTS (Edge - Premium)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Premium Persian TTS using Microsoft Edge neural voices - most natural and realistic Persian speech.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiopersianedgetts.PersianEdgeTTSPlugin")
    tags.set(listOf("tts", "gradio", "persian", "farsi", "edge", "microsoft", "neural"))
}
