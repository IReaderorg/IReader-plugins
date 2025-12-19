plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-fish-speech")
    name.set("Fish Speech (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Fast multilingual TTS with natural prosody. Supports English, Chinese, Japanese, and Korean.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiofishspeech.FishSpeechPlugin")
    tags.set(listOf("tts", "gradio", "fish-speech", "multilingual", "fast"))
}
