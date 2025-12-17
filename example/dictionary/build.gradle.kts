plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.dictionary")
    name.set("Dictionary Lookup")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Look up word definitions while reading")
    author.set("IReader Team")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(PluginPermission.READER_CONTEXT, PluginPermission.NETWORK))
}
