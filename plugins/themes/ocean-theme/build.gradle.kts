plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.ocean-theme")
    name.set("Ocean Theme")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("A calming ocean-inspired theme with blue tones")
    author.set("IReader Team")
    authorWebsite.set("https://github.com/IReaderorg")
    type.set(PluginType.THEME)
    permissions.set(emptyList())
}
