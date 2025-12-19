plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.mint-fresh")
    name.set("Mint Fresh")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Cool refreshing mint green theme for a clean reading experience")
    author.set("IReader Team")
    authorWebsite.set("https://github.com/IReaderorg")
    type.set(PluginType.THEME)
    permissions.set(emptyList())
}
