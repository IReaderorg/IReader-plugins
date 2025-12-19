plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.libretranslate")
    name.set("LibreTranslate")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Free and open-source translation using LibreTranslate API. No API key required.")
    author.set("IReader Team")
    type.set(PluginType.TRANSLATION)
    permissions.set(listOf(PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.libretranslate.LibreTranslatePlugin")
    tags.set(listOf("translation", "free", "open-source", "libretranslate"))
}
