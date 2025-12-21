plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.reading-timer")
    name.set("Reading Timer")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Track your reading time with session tracking, daily summaries, and focus mode. Includes Pomodoro technique support.")
    author.set("IReader Team")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(PluginPermission.PREFERENCES, PluginPermission.READER_CONTEXT))
    mainClass.set("io.github.ireaderorg.plugins.readingtimer.ReadingTimerPlugin")
    tags.set(listOf("timer", "tracking", "focus", "pomodoro", "reading", "productivity", "time"))
}
