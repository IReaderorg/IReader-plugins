plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.reading-stats")
    name.set("Reading Statistics")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Comprehensive reading statistics and analytics. Track reading time, pages read, reading speed, daily/weekly/monthly goals, and reading streaks.")
    author.set("IReader Team")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT))
    mainClass.set("io.github.ireaderorg.plugins.readingstats.ReadingStatsPlugin")
    tags.set(listOf("statistics", "analytics", "reading", "goals", "streaks", "progress", "time"))
}
