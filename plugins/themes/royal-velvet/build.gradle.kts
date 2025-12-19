plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.royal-velvet")
    name.set("Royal Velvet")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Rich luxurious purple theme fit for royalty")
    author.set("IReader Team")
    authorWebsite.set("https://github.com/IReaderorg")
    type.set(PluginType.THEME)
    permissions.set(emptyList())
    mainClass.set("io.github.ireaderorg.plugins.royalvelvet.RoyalVelvetTheme")
}
