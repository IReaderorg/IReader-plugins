plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.ollama-translate")
    name.set("Ollama Translation")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Local AI translation using Ollama. Run LLMs locally for private, offline translation. Requires Ollama server running.")
    author.set("IReader Team")
    type.set(PluginType.TRANSLATION)
    permissions.set(listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES))
    mainClass.set("io.github.ireaderorg.plugins.ollamatranslate.OllamaTranslatePlugin")
    tags.set(listOf("translation", "ai", "ollama", "local", "offline", "llm", "privacy"))
}
