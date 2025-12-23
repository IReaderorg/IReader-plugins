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
        graalvmVersion.convention("25.0.1")
    }
    
    // Complete JARs for GraalVM JS - all required dependencies
    private val requiredJars = listOf(
        // === Core Polyglot API ===
        "org/graalvm/polyglot/polyglot" to "polyglot",
        
        // === JavaScript Language ===
        "org/graalvm/js/js-language" to "js-language",
        
        // === Truffle Framework ===
        "org/graalvm/truffle/truffle-api" to "truffle-api",
        "org/graalvm/truffle/truffle-runtime" to "truffle-runtime",
        "org/graalvm/truffle/truffle-compiler" to "truffle-compiler",
        
        // === TRegex - REQUIRED for regex support in JS ===
        "org/graalvm/regex/regex" to "regex",
        
        // === SDK Components ===
        "org/graalvm/sdk/collections" to "collections",
        "org/graalvm/sdk/word" to "word",
        "org/graalvm/sdk/nativeimage" to "nativeimage",
        "org/graalvm/sdk/jniutils" to "jniutils",
        
        // === ICU4J (shadowed) - REQUIRED for Intl API and Date locale support ===
        // This is the GraalVM-shadowed version of ICU4J with package org.graalvm.shadowed.com.ibm.icu
        "org/graalvm/shadowed/icu4j" to "icu4j"
    )
    
    @TaskAction
    fun execute() {
        val version = graalvmVersion.get()
        val outDir = outputDir.get().asFile
        val cache = cacheDir.get().asFile
        
        cache.mkdirs()
        outDir.mkdirs()
        
        var totalSize = 0L
        
        // Essential JARs that must be present
        val essentialArtifacts = setOf("polyglot", "js-language", "truffle-api", "regex", "collections", "word", "icu4j")
        
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
                    if (artifactName in essentialArtifacts) {
                        throw RuntimeException("Failed to download essential JAR $jarName: ${e.message}", e)
                    }
                    logger.warn("Optional JAR not available: $jarName (${e.message})")
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
