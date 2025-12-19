plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.piper-tts")
    name.set("Piper TTS")
    version.set("1.2.1")
    versionCode.set(2)
    description.set("High-performance neural text-to-speech with 30+ voices in 20+ languages. Powered by Piper.")
    author.set("IReader Team")
    type.set(PluginType.TTS)
    permissions.set(listOf(PluginPermission.STORAGE, PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.pipertts.PiperTTSPlugin")
    platforms.set(listOf(PluginPlatform.DESKTOP)) // Desktop only
}

// Piper JNI version (from Maven Central)
val piperVersion = "1.2.0-c0670df"

// Configuration for piper-jni JAR (classes only, not native libs)
val piperJniJar by configurations.creating {
    isTransitive = false
}

dependencies {
    // Piper JNI for compilation
    compileOnly("io.github.givimad:piper-jni:$piperVersion")
    // Also add to piperJniJar configuration for packaging
    piperJniJar("io.github.givimad:piper-jni:$piperVersion")
}

// Task to copy piper-jni JAR to libs folder for packaging
val copyPiperJniJar = tasks.register<Copy>("copyPiperJniJar") {
    from(piperJniJar)
    into(layout.projectDirectory.dir("src/main/libs"))
    rename { "piper-jni.jar" }
}

// Task to download Piper native libraries for all desktop platforms
val downloadPiperNatives = tasks.register<DownloadPiperNativesTask>("downloadPiperNatives") {
    piperJniVersion.set(piperVersion)
    outputDir.set(layout.projectDirectory.dir("src/main/native"))
    cacheDir.set(layout.buildDirectory.dir("download-cache"))
}

// Make packagePlugin depend on downloading natives and copying JAR
tasks.named("packagePlugin") {
    dependsOn(downloadPiperNatives, copyPiperJniJar)
}
