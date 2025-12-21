plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.reading-tracker")
    name.set("Reading Progress Tracker")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Track and sync your reading progress across devices. Supports cloud backup, reading goals, and detailed analytics.")
    author.set("IReader Team")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(PluginPermission.NETWORK, PluginPermission.PREFERENCES, PluginPermission.LIBRARY_ACCESS))
    mainClass.set("io.github.ireaderorg.plugins.readingtracker.ReadingTrackerPlugin")
    tags.set(listOf("tracking", "progress", "sync", "backup", "analytics", "goals", "reading"))
}
