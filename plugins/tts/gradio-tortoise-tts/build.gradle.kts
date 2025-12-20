plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.gradio-tortoise-tts")
    name.set("Tortoise TTS (Gradio)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("High-quality TTS with many voice options. Slower but produces excellent results.")
    author.set("IReader Team")
    type.set(PluginType.GRADIO_TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.gradiotortoise.TortoiseTTSPlugin")
    tags.set(listOf("tts", "gradio", "tortoise", "high-quality", "voices"))
    metadata.set(mapOf(
        "gradio.spaceUrl" to "https://jbetker-tortoise-tts.hf.space",
        "gradio.apiName" to "/predict",
        "gradio.apiType" to "GRADIO_API",
        "gradio.audioOutputIndex" to "0",
        "gradio.languages" to "en",
        "gradio.params" to """[{"type":"text","name":"text"},{"type":"choice","name":"voice","choices":["random","angie","deniro","freeman","halle","lj","myself","pat","snakes","tom","train_atkins","train_daws","train_dotrice","train_dreams","train_empire","train_grace","train_kennard","train_lescault","train_mouse","weaver","william"],"default":"random"},{"type":"choice","name":"preset","choices":["ultra_fast","fast","standard","high_quality"],"default":"fast"}]"""
    ))
}
