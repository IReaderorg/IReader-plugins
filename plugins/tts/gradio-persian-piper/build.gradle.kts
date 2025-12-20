plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-persian-piper")
    name.set("Persian TTS (Piper - Basic)")
    version.set("1.1.0")
    versionCode.set(2)
    description.set("Persian language TTS using Piper voices via Gradio (basic quality). Updated with improved stability.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiopersianpiper.PersianPiperTTSPlugin")
    tags.set(listOf("tts", "gradio", "persian", "farsi", "piper"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://gyroing-persian-tts-piper.hf.space",
        "gradio.apiName" to "/synthesize_speech",
        "gradio.apiType" to "GRADIO_API_CALL",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "fa,fa-IR",
        "gradio.params" to """[{"type":"text","name":"text"}]"""
    ))
}
