plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-xtts-v2")
    name.set("XTTS v2 (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Coqui's latest multilingual TTS with voice cloning support. 15+ languages including English, Spanish, French, German, and more.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradioxttsv2.XTTSv2Plugin")
    tags.set(listOf("tts", "gradio", "xtts", "coqui", "multilingual", "voice-cloning"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://coqui-xtts.hf.space",
        "gradio.apiName" to "/predict",
        "gradio.apiType" to "GRADIO_API",
        "gradio.audioOutputIndex" to "0",
        "gradio.supportsVoiceCloning" to "true",
        "gradio.languages" to "en,es,fr,de,it,pt,pl,tr,ru,nl,cs,ar,zh-cn,ja,ko",
        "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"language","choices":["en","es","fr","de","it","pt","pl","tr","ru","nl","cs","ar","zh-cn","ja","ko"],"default":"en"}]"""
    ))
}
