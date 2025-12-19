plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.desert-sand")
    name.set("Desert Sand")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Warm earthy tones inspired by desert landscapes")
    author.set("IReader Team")
    authorWebsite.set("https://github.com/IReaderorg")
    type.set(PluginType.THEME)
    permissions.set(emptyList())
    mainClass.set("io.github.ireaderorg.plugins.desertsand.DesertSandTheme")
}
