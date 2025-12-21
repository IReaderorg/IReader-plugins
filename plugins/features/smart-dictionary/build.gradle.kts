plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.smart-dictionary")
    name.set("Smart Dictionary")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Instant word definitions and translations while reading. Features vocabulary building, spaced repetition review, and offline caching.")
    author.set("IReader Team")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT))
    mainClass.set("io.github.ireaderorg.plugins.smartdictionary.SmartDictionaryPlugin")
    tags.set(listOf("dictionary", "vocabulary", "translation", "definitions", "learning", "reading"))
}
