import java.net.URI

plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.hermes-engine")
    name.set("Hermes JavaScript Engine")
    version.set("0.12.0")  // Match Player UI Hermes library version
    versionCode.set(2)
    description.set("Hermes JavaScript engine for Android - smaller binary size, faster startup, lower memory usage. Uses Player UI's standalone Hermes wrapper.")
    author.set("IReader Team")
    type.set(PluginType.JS_ENGINE)
    permissions.set(listOf(PluginPermission.STORAGE))
    mainClass.set("io.github.ireaderorg.plugins.hermesengine.HermesEnginePlugin")
    platforms.set(listOf(PluginPlatform.ANDROID)) // Android only
}

// Player UI Hermes version - provides standalone Hermes wrapper for Android
val playerUiHermesVersion = "0.12.0-next.2"

// Download the Player UI Hermes Android AAR which includes:
// - Native libhermes.so for all ABIs
// - Java/Kotlin wrapper classes with simple eval API
val downloadHermesAar = tasks.register<DownloadAndExtractAarTask>("downloadHermesAar") {
    aarUrl.set("https://repo1.maven.org/maven2/com/intuit/playerui/hermes-android/$playerUiHermesVersion/hermes-android-$playerUiHermesVersion.aar")
    aarFileName.set("hermes-android-$playerUiHermesVersion.aar")
    sourcePrefix.set("jni/")
    outputDir.set(layout.projectDirectory.dir("src/main/jniLibs"))
    cacheDir.set(layout.buildDirectory.dir("download-cache"))
}

// Extract classes from the AAR for DEX generation
val extractHermesClasses = tasks.register<ExtractAarClassesTask>("extractHermesClasses") {
    aarUrl.set("https://repo1.maven.org/maven2/com/intuit/playerui/hermes-android/$playerUiHermesVersion/hermes-android-$playerUiHermesVersion.aar")
    aarFileName.set("hermes-android-$playerUiHermesVersion.aar")
    outputDir.set(layout.buildDirectory.dir("hermes-classes"))
    cacheDir.set(layout.buildDirectory.dir("download-cache"))
}

// Also download the base hermes JAR which contains the core wrapper classes
val downloadHermesJar = tasks.register("downloadHermesJar") {
    val jarUrl = "https://repo1.maven.org/maven2/com/intuit/playerui/hermes/$playerUiHermesVersion/hermes-$playerUiHermesVersion.jar"
    val outputDir = layout.buildDirectory.dir("hermes-jar")
    val jarFile = outputDir.map { it.file("hermes-$playerUiHermesVersion.jar") }
    
    outputs.file(jarFile)
    
    doLast {
        val outDir = outputDir.get().asFile
        outDir.mkdirs()
        val outFile = jarFile.get().asFile
        
        if (!outFile.exists()) {
            // Use ant.get for downloading
            ant.invokeMethod("get", mapOf(
                "src" to jarUrl,
                "dest" to outFile,
                "verbose" to true
            ))
            println("Downloaded: $jarUrl")
        }
        
        // Extract JAR contents to hermes-classes
        val classesDir = layout.buildDirectory.dir("hermes-classes").get().asFile
        ant.invokeMethod("unzip", mapOf(
            "src" to outFile,
            "dest" to classesDir
        ))
        println("Extracted classes to: $classesDir")
    }
}

// Make JAR task include Hermes classes
tasks.named<Jar>("jar") {
    dependsOn(extractHermesClasses, downloadHermesJar)
    from(layout.buildDirectory.dir("hermes-classes"))
    
    // Exclude Kotlin metadata that might conflict
    exclude("META-INF/*.kotlin_module")
}

// Make packagePlugin depend on downloading natives
tasks.named("packagePlugin") {
    dependsOn(downloadHermesAar)
}
