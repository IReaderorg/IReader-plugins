plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.bookmark-manager")
    name.set("Bookmark Manager")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Advanced bookmark management with tags, notes, and smart organization. Features include bookmark search, export/import, and reading progress tracking.")
    author.set("IReader Team")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT))
    mainClass.set("io.github.ireaderorg.plugins.bookmarkmanager.BookmarkManagerPlugin")
    tags.set(listOf("bookmark", "notes", "tags", "organization", "reading", "progress"))
}
