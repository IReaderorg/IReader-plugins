plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.deepseek-translate")
    name.set("DeepSeek Translation")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("AI-powered translation using DeepSeek API. Supports context-aware translation with style preservation for literary content.")
    author.set("IReader Team")
    type.set(PluginType.TRANSLATION)
    permissions.set(listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES))
    mainClass.set("io.github.ireaderorg.plugins.deepseektranslate.DeepSeekTranslatePlugin")
    tags.set(listOf("translation", "ai", "deepseek", "literary", "context-aware"))
}
