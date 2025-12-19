plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.huggingface-translate")
    name.set("Hugging Face Translation")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Free AI translation using Hugging Face Inference API with Helsinki-NLP models. No API key required.")
    author.set("IReader Team")
    type.set(PluginType.TRANSLATION)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.huggingfacetranslate.HuggingFaceTranslatePlugin")
    tags.set(listOf("translation", "ai", "free", "huggingface", "helsinki-nlp"))
}
