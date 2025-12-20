plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-fish-speech")
    name.set("Fish Speech (Gradio)")
    version.set("1.1.0")
    versionCode.set(2)
    description.set("Fast multilingual TTS with natural prosody. Supports English, Chinese, Japanese, and Korean. Updated with better voice quality.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiofishspeech.FishSpeechPlugin")
    tags.set(listOf("tts", "gradio", "fish-speech", "multilingual", "fast"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://fishaudio-fish-speech-1.hf.space",
        "gradio.apiName" to "/predict",
        "gradio.apiType" to "GRADIO_API",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "en,zh,ja,ko",
        "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"language","choices":["en","zh","ja","ko"],"default":"en"}]"""
    ))
}
