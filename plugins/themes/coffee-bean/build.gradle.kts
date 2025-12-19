plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.coffee-bean")
    name.set("Coffee Bean")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Rich coffee brown theme perfect for cozy reading sessions")
    author.set("IReader Team")
    authorWebsite.set("https://github.com/IReaderorg")
    type.set(PluginType.THEME)
    permissions.set(emptyList())
    mainClass.set("io.github.ireaderorg.plugins.coffeebean.CoffeeBeanTheme")
}
