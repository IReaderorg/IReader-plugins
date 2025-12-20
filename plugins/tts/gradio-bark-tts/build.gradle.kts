plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-bark-tts")
    name.set("Bark TTS (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("High-quality generative TTS by Suno. Supports multiple speaker presets.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiobark.BarkTTSPlugin")
    tags.set(listOf("tts", "gradio", "bark", "suno", "generative"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://suno-bark.hf.space",
        "gradio.apiName" to "/predict",
        "gradio.apiType" to "GRADIO_API",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "en",
        "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"voice_preset","choices":["v2/en_speaker_0","v2/en_speaker_1","v2/en_speaker_2","v2/en_speaker_3"],"default":"v2/en_speaker_0"}]"""
    ))
}
