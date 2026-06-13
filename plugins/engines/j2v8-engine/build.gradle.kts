plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.j2v8-engine")
    name.set("J2V8 JavaScript Engine")
    version.set("6.2.2")  // v6.2.2: Patched ELF for Android 15+ 16KB page alignment
    versionCode.set(3)  // v3: Patch ELF page alignment for Android 15+ (16KB pages)
    description.set("V8 JavaScript engine for Android - enables LNReader-compatible sources with full ES6+ support")
    author.set("IReader Team")
    type.set(PluginType.JS_ENGINE)
    permissions.set(listOf(PluginPermission.STORAGE))
    mainClass.set("io.github.ireaderorg.plugins.j2v8engine.J2V8EnginePlugin")
    platforms.set(listOf(PluginPlatform.ANDROID)) // Android only
    featured.set(true)
    tags.set(listOf("javascript", "js-engine", "android", "essential"))
}

// J2V8 version
val j2v8Version = "6.2.1"  // Maven artifact version (unchanged - we patch the .so post-download)

// Note: We don't add J2V8 as a Gradle dependency because:
// 1. It's an AAR which JVM plugin doesn't handle well
// 2. We extract classes manually via ExtractAarClassesTask
// 3. The extracted classes are added to the JAR which is then converted to DEX

// Task to download J2V8 AAR and extract native libraries
val downloadJ2V8Natives = tasks.register<DownloadAndExtractAarTask>("downloadJ2V8Natives") {
    aarUrl.set("https://repo1.maven.org/maven2/com/eclipsesource/j2v8/j2v8/$j2v8Version/j2v8-$j2v8Version.aar")
    aarFileName.set("j2v8-$j2v8Version.aar")
    sourcePrefix.set("jni/")
    outputDir.set(layout.projectDirectory.dir("src/main/jniLibs"))
    cacheDir.set(layout.buildDirectory.dir("download-cache"))
}

// Task to extract J2V8 Java classes from AAR for DEX generation
val extractJ2V8Classes = tasks.register<ExtractAarClassesTask>("extractJ2V8Classes") {
    aarUrl.set("https://repo1.maven.org/maven2/com/eclipsesource/j2v8/j2v8/$j2v8Version/j2v8-$j2v8Version.aar")
    aarFileName.set("j2v8-$j2v8Version.aar")
    outputDir.set(layout.buildDirectory.dir("j2v8-classes"))
    cacheDir.set(layout.buildDirectory.dir("download-cache"))
}

// Make JAR task include J2V8 classes
tasks.named<Jar>("jar") {
    dependsOn(extractJ2V8Classes)
    from(layout.buildDirectory.dir("j2v8-classes"))
}

tasks {
    register<PatchJ2V8PageSizeTask>("patchJ2V8PageSize") {
        jniLibsDir.set(layout.projectDirectory.dir("src/main/jniLibs"))
        cacheDir.set(layout.buildDirectory.dir("download-cache"))
        patcherScriptPath = "${rootProject.projectDir}/scripts/patch_j2v8_pagesize.py"
        dependsOn(downloadJ2V8Natives)
    }
}

// Make packagePlugin depend on downloading natives and patching page alignment
tasks.named("packagePlugin") {
    dependsOn(downloadJ2V8Natives)
    dependsOn("patchJ2V8PageSize")
}
