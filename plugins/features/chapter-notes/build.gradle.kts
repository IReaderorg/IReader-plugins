plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.chapter-notes")
    name.set("Chapter Notes")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Take notes and highlights while reading. Features tagging, search, and export capabilities for organizing your reading insights.")
    author.set("IReader Team")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT))
    mainClass.set("io.github.ireaderorg.plugins.chapternotes.ChapterNotesPlugin")
    tags.set(listOf("notes", "highlights", "annotations", "reading", "organization", "export"))
}
