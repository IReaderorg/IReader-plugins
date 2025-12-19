plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.sakura-night")
    name.set("Sakura Night")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Japanese cherry blossom inspired theme with soft pinks and purples")
    author.set("IReader Team")
    authorWebsite.set("https://github.com/IReaderorg")
    type.set(PluginType.THEME)
    permissions.set(emptyList())
    mainClass.set("io.github.ireaderorg.plugins.sakuranight.SakuraNightTheme")
}
