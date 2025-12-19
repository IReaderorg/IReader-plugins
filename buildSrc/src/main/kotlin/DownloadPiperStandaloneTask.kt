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
 * Task to download standalone Piper TTS releases for all desktop platforms.
 * Downloads from rhasspy/piper GitHub releases which include piper executable and all dependencies.
 */
abstract class DownloadPiperStandaloneTask : DefaultTask() {
    
    @get:Input
    abstract val piperVersion: Property<String>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:OutputDirectory
    abstract val cacheDir: DirectoryProperty
    
    init {
        piperVersion.convention("2023.11.14-2")
    }
    
    @TaskAction
    fun execute() {
        val version = piperVersion.get()
        val outDir = outputDir.get().asFile
        val cache = cacheDir.get().asFile
        cache.mkdirs()
        
        // Platform downloads
        val platforms = mapOf(
            "windows-x64" to "piper_windows_amd64.zip",
            "linux-x64" to "piper_linux_x86_64.tar.gz",
            "macos-x64" to "piper_macos_x64.tar.gz",
            "macos-arm64" to "piper_macos_aarch64.tar.gz"
        )
        
        for ((platformDir, fileName) in platforms) {
            val url = "https://github.com/rhasspy/piper/releases/download/$version/$fileName"
            val archiveFile = File(cache, fileName)
            val platformOutDir = File(outDir, platformDir)
            
            // Download if not cached
            if (!archiveFile.exists()) {
                logger.lifecycle("Downloading Piper for $platformDir from $url")
                try {
                    URI(url).toURL().openStream().use { input ->
                        archiveFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    logger.lifecycle("Downloaded: $fileName (${archiveFile.length()} bytes)")
                } catch (e: Exception) {
                    logger.warn("Failed to download $fileName: ${e.message}")
                    continue
                }
            } else {
                logger.lifecycle("Using cached: $fileName")
            }
            
            // Extract
            platformOutDir.mkdirs()
            if (fileName.endsWith(".zip")) {
                extractZip(archiveFile, platformOutDir)
            } else {
                extractTarGz(archiveFile, platformOutDir)
            }
        }
    }
    
    private fun extractZip(zipFile: File, outDir: File) {
        logger.lifecycle("Extracting ${zipFile.name} to $outDir")
        
        ZipFile(zipFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory) continue
                
                // Remove the top-level "piper" directory from the path
                val entryName = entry.name.removePrefix("piper/")
                if (entryName.isEmpty()) continue
                
                val outputFile = File(outDir, entryName)
                outputFile.parentFile?.mkdirs()
                
                zip.getInputStream(entry).use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                logger.lifecycle("Extracted: $entryName (${outputFile.length()} bytes)")
            }
        }
    }
    
    private fun extractTarGz(tarGzFile: File, outDir: File) {
        logger.lifecycle("Extracting ${tarGzFile.name} to $outDir")
        
        // Use ProcessBuilder to extract tar.gz (works on macOS/Linux)
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        
        if (isWindows) {
            // On Windows, use tar command (available in Windows 10+)
            val process = ProcessBuilder("tar", "-xzf", tarGzFile.absolutePath, "-C", outDir.absolutePath, "--strip-components=1")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                logger.warn("tar extraction failed: $output")
            }
        } else {
            val process = ProcessBuilder("tar", "-xzf", tarGzFile.absolutePath, "-C", outDir.absolutePath, "--strip-components=1")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                logger.warn("tar extraction failed: $output")
            }
        }
    }
}
