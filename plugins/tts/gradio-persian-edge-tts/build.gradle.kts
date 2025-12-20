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
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://innoai-edge-tts-text-to-speech.hf.space",
        "gradio.apiName" to "/predict",
        "gradio.apiType" to "GRADIO_API",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "fa,fa-IR",
        "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"voice","choices":["fa-IR-DilaraNeural","fa-IR-FaridNeural"],"default":"fa-IR-DilaraNeural"},{"type":"speed","name":"rate","default":1.0,"min":0.5,"max":2.0}]"""
    ))
}
