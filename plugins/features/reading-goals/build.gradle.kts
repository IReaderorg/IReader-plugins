plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.reading-goals")
    name.set("Reading Goals")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Set and track your reading goals with daily, weekly, and monthly targets. Features streak tracking, progress visualization, and achievement badges.")
    author.set("IReader Team")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT))
    mainClass.set("io.github.ireaderorg.plugins.readinggoals.ReadingGoalsPlugin")
    tags.set(listOf("goals", "tracking", "streaks", "achievements", "reading", "progress", "habits"))
}
