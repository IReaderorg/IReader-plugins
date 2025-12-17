import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI

/**
 * Task to download GraalVM Polyglot JS JARs for bundling in the plugin.
 * GraalVM is pure Java/JVM - no native libraries needed.
 */
abstract class DownloadGraalVMJarsTask : DefaultTask() {
    
    @get:Input
    abstract val graalvmVersion: Property<String>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:OutputDirectory
    abstract val cacheDir: DirectoryProperty
    
    init {
        graalvmVersion.convention("24.1.1")
    }
    
    // Required JARs for GraalVM JS
    private val requiredJars = listOf(
        // Core polyglot API
        "org/graalvm/polyglot/polyglot" to "polyglot",
        // JS language implementation
        "org/graalvm/js/js-language" to "js-language",
        // Truffle API (required by js-language)
        "org/graalvm/truffle/truffle-api" to "truffle-api",
        // Collections (required dependency)
        "org/graalvm/sdk/collections" to "collections",
        // Word (required dependency)
        "org/graalvm/sdk/word" to "word",
        // Nativeimage (required dependency)
        "org/graalvm/sdk/nativeimage" to "nativeimage"
    )
    
    @TaskAction
    fun execute() {
        val version = graalvmVersion.get()
        val outDir = outputDir.get().asFile
        val cache = cacheDir.get().asFile
        
        cache.mkdirs()
        outDir.mkdirs()
        
        var totalSize = 0L
        
        for ((mavenPath, artifactName) in requiredJars) {
            val jarName = "$artifactName-$version.jar"
            val jarUrl = "https://repo1.maven.org/maven2/$mavenPath/$version/$jarName"
            val cachedJar = File(cache, jarName)
            val outputJar = File(outDir, jarName)
            
            // Download if not cached
            if (!cachedJar.exists()) {
                logger.lifecycle("Downloading $jarName from $jarUrl")
                try {
                    URI(jarUrl).toURL().openStream().use { input ->
                        cachedJar.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    logger.lifecycle("Downloaded: $jarName (${cachedJar.length()} bytes)")
                } catch (e: Exception) {
                    logger.warn("Failed to download $jarName: ${e.message}")
                    continue
                }
            } else {
                logger.lifecycle("Using cached: $jarName")
            }
            
            // Copy to output directory
            cachedJar.copyTo(outputJar, overwrite = true)
            totalSize += outputJar.length()
        }
        
        logger.lifecycle("Total GraalVM JARs size: ${totalSize / 1024 / 1024} MB")
    }
}
