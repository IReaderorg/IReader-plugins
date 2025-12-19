plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.openai-translate")
    name.set("OpenAI Translation")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("AI-powered translation using OpenAI GPT models. High-quality context-aware translation with style preservation.")
    author.set("IReader Team")
    type.set(PluginType.TRANSLATION)
    permissions.set(listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES))
    mainClass.set("io.github.ireaderorg.plugins.openaitranslate.OpenAITranslatePlugin")
    tags.set(listOf("translation", "ai", "openai", "gpt", "literary", "context-aware"))
}
