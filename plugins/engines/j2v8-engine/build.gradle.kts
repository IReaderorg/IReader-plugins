plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.j2v8-engine")
    name.set("J2V8 JavaScript Engine")
    version.set("6.3.4")
    versionCode.set(1)
    description.set("V8 JavaScript engine for Android - enables LNReader-compatible sources with full ES6+ support")
    author.set("IReader Team")
    type.set(PluginType.JS_ENGINE)
    permissions.set(listOf(PluginPermission.STORAGE))
    mainClass.set("io.github.ireaderorg.plugins.j2v8engine.J2V8EnginePlugin")
    platforms.set(listOf(PluginPlatform.ANDROID)) // Android only
}

// J2V8 version
val j2v8Version = "6.2.1"

// Task to download J2V8 AAR and extract native libraries
val downloadJ2V8Natives = tasks.register<DownloadAndExtractAarTask>("downloadJ2V8Natives") {
    aarUrl.set("https://repo1.maven.org/maven2/com/eclipsesource/j2v8/j2v8/$j2v8Version/j2v8-$j2v8Version.aar")
    aarFileName.set("j2v8-$j2v8Version.aar")
    sourcePrefix.set("jni/")
    outputDir.set(layout.projectDirectory.dir("src/main/jniLibs"))
    cacheDir.set(layout.buildDirectory.dir("download-cache"))
}

// Make packagePlugin depend on downloading natives
tasks.named("packagePlugin") {
    dependsOn(downloadJ2V8Natives)
}
