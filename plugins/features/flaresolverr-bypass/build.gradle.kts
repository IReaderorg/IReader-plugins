plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.flaresolverr-bypass")
    name.set("FlareSolverr Bypass")
    version.set("2.0.0")
    versionCode.set(2)
    description.set("Cloudflare bypass using FlareSolverr. Downloads the required files automatically on first use.")
    author.set("IReader Team")
    type.set(PluginType.FEATURE)
    permissions.set(listOf(PluginPermission.NETWORK, PluginPermission.STORAGE))
    mainClass.set("io.github.ireaderorg.plugins.flaresolverr.FlareSolverrBypassPlugin")
    platforms.set(listOf(PluginPlatform.DESKTOP))
    tags.set(listOf("cloudflare", "bypass", "flaresolverr", "windows", "linux", "macos"))
    metadata.set(mapOf(
        "flaresolverr.version" to "v3.3.21",
        "isCloudflareBypass" to "true",
        "downloadOnDemand" to "true"
    ))
}
