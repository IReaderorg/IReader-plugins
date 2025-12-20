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
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://silero-silero-tts.hf.space",
        "gradio.apiName" to "/predict",
        "gradio.apiType" to "GRADIO_API",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "en,de,es,fr,ru,ua,uz,xal,indic",
        "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"language","choices":["en","de","es","fr","ru","ua","uz","xal","indic"],"default":"en"},{"type":"choice","name":"speaker","choices":["en_0","en_1","en_2","en_3","en_4"],"default":"en_0"}]"""
    ))
}
