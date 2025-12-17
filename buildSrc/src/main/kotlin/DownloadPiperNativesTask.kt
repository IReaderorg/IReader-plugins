import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URI
import java.util.zip.ZipFile

/**
 * Task to download Piper TTS native libraries for all desktop platforms.
 * Downloads from piper-jni on Maven Central which bundles all platforms in one JAR.
 */
abstract class DownloadPiperNativesTask : DefaultTask() {
    
    @get:Input
    abstract val piperJniVersion: Property<String>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:OutputDirectory
    abstract val cacheDir: DirectoryProperty
    
    init {
        piperJniVersion.convention("1.2.0-c0670df")
    }
    
    @TaskAction
    fun execute() {
        val version = piperJniVersion.get()
        val outDir = outputDir.get().asFile
        val cache = cacheDir.get().asFile
        cache.mkdirs()
        
        val jarName = "piper-jni-$version.jar"
        val jarUrl = "https://repo1.maven.org/maven2/io/github/givimad/piper-jni/$version/$jarName"
        val jarFile = File(cache, jarName)
        
        // Download JAR if not cached
        if (!jarFile.exists()) {
            logger.lifecycle("Downloading Piper JNI from $jarUrl")
            try {
                URI(jarUrl).toURL().openStream().use { input ->
                    jarFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                logger.lifecycle("Downloaded: $jarName (${jarFile.length()} bytes)")
            } catch (e: Exception) {
                logger.error("Failed to download $jarName: ${e.message}")
                throw e
            }
        } else {
            logger.lifecycle("Using cached: $jarName")
        }
        
        // Extract native libraries organized by platform
        extractNativeLibraries(jarFile, outDir)
    }
    
    private fun extractNativeLibraries(jarFile: File, outDir: File) {
        logger.lifecycle("Extracting native libraries to $outDir")
        
        // Platform mappings: JAR path prefix -> output directory
        val platformMappings = mapOf(
            "win-amd64/" to "windows-x64",
            "debian-amd64/" to "linux-x64",
            "debian-arm64/" to "linux-arm64",
            "debian-armv7l/" to "linux-arm32",
            "macos-amd64/" to "macos-x64",
            "macos-arm64/" to "macos-arm64"
        )
        
        ZipFile(jarFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory) continue
                
                // Check if this is a native library
                for ((prefix, platformDir) in platformMappings) {
                    if (entry.name.startsWith(prefix)) {
                        val fileName = entry.name.removePrefix(prefix)
                        // Only extract actual library files
                        if (fileName.endsWith(".dll") || fileName.endsWith(".so") || fileName.endsWith(".dylib")) {
                            val platformOutDir = File(outDir, platformDir)
                            platformOutDir.mkdirs()
                            val outputFile = File(platformOutDir, fileName)
                            
                            zip.getInputStream(entry).use { input ->
                                outputFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            logger.lifecycle("Extracted: $platformDir/$fileName (${outputFile.length()} bytes)")
                        }
                        break
                    }
                }
            }
        }
    }
}
