plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.piper-tts")
    name.set("Piper TTS")
    version.set("2.0.0")
    versionCode.set(3)
    description.set("High-performance neural text-to-speech with 30+ voices in 20+ languages. Uses standalone Piper executable.")
    author.set("IReader Team")
    type.set(PluginType.TTS)
    permissions.set(listOf(PluginPermission.STORAGE, PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.pipertts.PiperTTSPlugin")
    platforms.set(listOf(PluginPlatform.DESKTOP)) // Desktop only
    featured.set(true)
    tags.set(listOf("tts", "text-to-speech", "neural", "voices", "desktop"))
}

// Piper standalone release version (from GitHub)
val piperStandaloneVersion = "2023.11.14-2"

// Task to download standalone Piper releases for all desktop platforms
val downloadPiperStandalone = tasks.register<DownloadPiperStandaloneTask>("downloadPiperStandalone") {
    piperVersion.set(piperStandaloneVersion)
    outputDir.set(layout.projectDirectory.dir("src/main/native"))
    cacheDir.set(layout.buildDirectory.dir("download-cache"))
}

// Make packagePlugin depend on downloading standalone Piper
tasks.named("packagePlugin") {
    dependsOn(downloadPiperStandalone)
}
