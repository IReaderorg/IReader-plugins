plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-openvoice")
    name.set("OpenVoice (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Voice cloning TTS with emotion control. Supports multiple styles like whispering, shouting, excited, and more.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradioopenvoice.OpenVoicePlugin")
    tags.set(listOf("tts", "gradio", "openvoice", "voice-cloning", "emotion", "style"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://myshell-ai-openvoice.hf.space",
        "gradio.apiName" to "/predict",
        "gradio.apiType" to "GRADIO_API",
        "gradio.audioOutputIndex" to "0",
        "gradio.supportsVoiceCloning" to "true",
        "gradio.languages" to "en,es,fr,zh,ja,ko",
        "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"style","choices":["default","whispering","shouting","excited","cheerful","terrified","angry","sad","friendly"],"default":"default"},{"type":"choice","name":"language","choices":["EN","ES","FR","ZH","JP","KR"],"default":"EN"}]"""
    ))
}
