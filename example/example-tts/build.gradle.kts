plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.example-tts")
    name.set("Example TTS")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Example TTS plugin demonstrating the TTS plugin API")
    author.set("IReader Team")
    type.set(PluginType.TTS)
    permissions.set(listOf(PluginPermission.NETWORK))
}
