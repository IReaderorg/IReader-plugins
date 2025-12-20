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
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://innoai-edge-tts-text-to-speech.hf.space",
        "gradio.apiName" to "/predict",
        "gradio.apiType" to "GRADIO_API",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "en,en-US,en-GB,en-AU",
        "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"voice","choices":["en-US-AriaNeural","en-US-GuyNeural","en-GB-SoniaNeural","en-AU-NatashaNeural"],"default":"en-US-AriaNeural"},{"type":"speed","name":"rate","default":1.0,"min":0.5,"max":2.0}]"""
    ))
}
