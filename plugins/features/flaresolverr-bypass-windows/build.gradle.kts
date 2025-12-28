plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.flaresolverr-bypass-windows")
    name.set("FlareSolverr Bypass (Windows)")
    version.set("1.0.0")
    versionCode.set(1)
    description.set("Cloudflare bypass using bundled FlareSolverr for Windows. Auto-starts when needed, no external setup required.")
    author.set("IReader Team")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(PluginPermission.NETWORK, PluginPermission.STORAGE))
    mainClass.set("io.github.ireaderorg.plugins.flaresolverr.FlareSolverrBypassPlugin")
    platforms.set(listOf(PluginPlatform.DESKTOP))
    tags.set(listOf("cloudflare", "bypass", "flaresolverr", "windows"))
    metadata.set(mapOf(
        "flaresolverr.version" to "v3.4.6",
        "flaresolverr.platform" to "windows-x64",
        "isCloudflareBypass" to "true"
    ))
}

val downloadFlareSolverr by tasks.registering(DownloadFlareSolverrTask::class) {
    platforms.set(listOf("windows-x64"))
    outputDir.set(file("src/main/native"))
}

tasks.named("processResources") {
    dependsOn(downloadFlareSolverr)
}

sourceSets {
    main {
        resources {
            srcDir("src/main/native")
        }
    }
}
