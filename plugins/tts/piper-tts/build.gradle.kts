plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.piper-tts")
    name.set("Piper TTS")
    version.set("1.2.0")
    versionCode.set(1)
    description.set("High-performance neural text-to-speech with 30+ voices in 20+ languages. Powered by Piper.")
    author.set("IReader Team")
    type.set(PluginType.TTS)
    permissions.set(listOf(PluginPermission.STORAGE, PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.pipertts.PiperTTSPlugin")
    platforms.set(listOf(PluginPlatform.DESKTOP)) // Desktop only
}

// Piper JNI version (from Maven Central)
val piperVersion = "1.2.0-c0670df"

// Task to download Piper native libraries for all desktop platforms
val downloadPiperNatives = tasks.register<DownloadPiperNativesTask>("downloadPiperNatives") {
    piperJniVersion.set(piperVersion)
    outputDir.set(layout.projectDirectory.dir("src/main/native"))
    cacheDir.set(layout.buildDirectory.dir("download-cache"))
}

// Make packagePlugin depend on downloading natives
tasks.named("packagePlugin") {
    dependsOn(downloadPiperNatives)
}
