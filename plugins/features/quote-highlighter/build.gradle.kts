plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.quote-highlighter")
    name.set("Quote Highlighter")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Highlight and save memorable quotes while reading. Features include color-coded highlights, quote collections, and easy sharing to social media.")
    author.set("IReader Team")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT))
    mainClass.set("io.github.ireaderorg.plugins.quotehighlighter.QuoteHighlighterPlugin")
    tags.set(listOf("quotes", "highlights", "annotations", "sharing", "collections", "reading"))
}
