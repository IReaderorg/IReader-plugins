plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.tachi-loader")
    name.set("Tachiyomi Source Loader")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Load Tachiyomi/Mihon manga extensions in IReader. Supports browsing, searching, and reading manga from hundreds of sources.")
    author.set("IReader Team")
    type.set(PluginType.TACHI_SOURCE_LOADER)
    permissions.set(listOf(
        PluginPermission.NETWORK,
        PluginPermission.STORAGE,
        PluginPermission.PREFERENCES
    ))
    mainClass.set("io.github.ireaderorg.plugins.tachiloader.TachiLoaderPlugin")
    tags.set(listOf("tachiyomi", "mihon", "manga", "sources", "extensions"))
    platforms.set(listOf(Platform.ANDROID, Platform.DESKTOP))
    skipFromRepo.set(true)
    skipFromRepoReason.set("Internal source loader - not for public distribution")
}

dependencies {
    // OkHttp for network requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
