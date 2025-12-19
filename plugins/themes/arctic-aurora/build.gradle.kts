plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.arctic-aurora")
    name.set("Arctic Aurora")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Northern lights inspired with teal and purple gradients")
    author.set("IReader Team")
    authorWebsite.set("https://github.com/IReaderorg")
    type.set(PluginType.THEME)
    permissions.set(emptyList())
    mainClass.set("io.github.ireaderorg.plugins.arcticaurora.ArcticAuroraTheme")
}
