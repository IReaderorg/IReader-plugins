plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.webtoon-reader")
    name.set("Webtoon Reader")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Vertical scrolling reader optimized for webtoons and long-strip manga. Features smooth scrolling, preloading, and gesture controls.")
    author.set("IReader Team")
    type.set(PluginType.READER_SCREEN)
    permissions.set(listOf(PluginPermission.STORAGE, PluginPermission.PREFERENCES))
    mainClass.set("io.github.ireaderorg.plugins.webtoonreader.WebtoonReaderPlugin")
    tags.set(listOf("reader", "webtoon", "manga", "vertical-scroll", "image"))
    platforms.set(listOf(Platform.ANDROID, Platform.DESKTOP))
    skipFromRepo.set(true)
    skipFromRepoReason.set("Internal reader screen - not for public distribution")
}
