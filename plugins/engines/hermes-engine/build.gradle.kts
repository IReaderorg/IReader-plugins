import java.io.File
import java.net.URI
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

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

// Task to download and extract the base hermes JAR
abstract class DownloadHermesJarTask : DefaultTask() {
    @get:Input
    abstract val jarUrl: Property<String>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:OutputDirectory
    abstract val classesDir: DirectoryProperty
    
    @TaskAction
    fun execute() {
        val outDir = outputDir.get().asFile
        outDir.mkdirs()
        
        val jarFileName = jarUrl.get().substringAfterLast("/")
        val outFile = File(outDir, jarFileName)
        
        if (!outFile.exists()) {
            logger.lifecycle("Downloading: ${jarUrl.get()}")
            URL(jarUrl.get()).openStream().use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            logger.lifecycle("Downloaded: $jarFileName")
        }
        
        // Extract JAR contents to classes directory
        val classesOutput = classesDir.get().asFile
        classesOutput.mkdirs()
        
        val zipFile = ZipFile(outFile)
        try {
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry: ZipEntry = entries.nextElement()
                val destFile = File(classesOutput, entry.name)
                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    val inputStream = zipFile.getInputStream(entry)
                    inputStream.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } finally {
            zipFile.close()
        }
        logger.lifecycle("Extracted classes to: $classesOutput")
    }
}

val downloadHermesJar = tasks.register<DownloadHermesJarTask>("downloadHermesJar") {
    jarUrl.set("https://repo1.maven.org/maven2/com/intuit/playerui/hermes/$playerUiHermesVersion/hermes-$playerUiHermesVersion.jar")
    outputDir.set(layout.buildDirectory.dir("hermes-jar"))
    classesDir.set(layout.buildDirectory.dir("hermes-classes"))
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
