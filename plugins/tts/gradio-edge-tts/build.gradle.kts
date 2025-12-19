plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-edge-tts")
    name.set("Edge TTS (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Microsoft Edge TTS with multiple voices via Gradio. High-quality neural voices for English and other languages.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradioedgetts.EdgeTTSPlugin")
    tags.set(listOf("tts", "gradio", "edge", "microsoft", "neural", "multilingual"))
}
