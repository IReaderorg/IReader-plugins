plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.example-translation")
    name.set("Example Translation")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Example translation plugin demonstrating the Translation plugin API")
    author.set("IReader Team")
    type.set(PluginType.TRANSLATION)
    permissions.set(listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES))
}
