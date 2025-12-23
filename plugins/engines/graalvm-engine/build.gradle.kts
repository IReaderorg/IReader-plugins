plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.graalvm-engine")
    name.set("GraalVM JavaScript Engine")
    version.set("25.0.1")  // Match GraalVM version
    versionCode.set(3)
    description.set("GraalVM Polyglot JavaScript engine for Desktop - high-performance JS execution with full ES2022+ support")
    author.set("IReader Team")
    type.set(PluginType.JS_ENGINE)
    permissions.set(listOf(PluginPermission.STORAGE))
    mainClass.set("io.github.ireaderorg.plugins.graalvmengine.GraalVMEnginePlugin")
    platforms.set(listOf(PluginPlatform.DESKTOP)) // Desktop only
    featured.set(true)
    tags.set(listOf("javascript", "js-engine", "desktop", "essential"))
}

// GraalVM version
val graalVersion = "25.0.1"

// Task to download GraalVM JARs
val downloadGraalVMJars = tasks.register<DownloadGraalVMJarsTask>("downloadGraalVMJars") {
    graalvmVersion.set(graalVersion)
    outputDir.set(layout.projectDirectory.dir("src/main/libs"))
    cacheDir.set(layout.buildDirectory.dir("download-cache"))
}

// Make packagePlugin depend on downloading JARs
tasks.named("packagePlugin") {
    dependsOn(downloadGraalVMJars)
}
