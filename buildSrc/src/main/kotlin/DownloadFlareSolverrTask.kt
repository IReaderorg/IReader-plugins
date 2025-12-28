import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Gradle task to download FlareSolverr precompiled binaries from GitHub releases.
 * 
 * FlareSolverr provides standalone binaries for Windows and Linux x64.
 * These binaries include Chrome/Chromium and all dependencies needed to run.
 */
abstract class DownloadFlareSolverrTask : DefaultTask() {
    
    companion object {
        const val FLARESOLVERR_VERSION = "v3.4.6"
        const val GITHUB_RELEASE_URL = "https://github.com/FlareSolverr/FlareSolverr/releases/download"
        
        // Platform-specific download URLs
        val PLATFORM_DOWNLOADS = mapOf(
            "windows-x64" to "$GITHUB_RELEASE_URL/$FLARESOLVERR_VERSION/flaresolverr_windows_x64.zip",
            "linux-x64" to "$GITHUB_RELEASE_URL/$FLARESOLVERR_VERSION/flaresolverr_linux_x64.tar.gz"
        )
    }
    
    @get:Input
    abstract val platforms: ListProperty<String>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    init {
        platforms.convention(listOf("windows-x64"))
    }
    
    @TaskAction
    fun download() {
        val outDir = outputDir.get().asFile
        outDir.mkdirs()
        
        platforms.get().forEach { platform ->
            val url = PLATFORM_DOWNLOADS[platform]
            if (url == null) {
                logger.warn("No FlareSolverr binary available for platform: $platform")
                return@forEach
            }
            
            val platformDir = File(outDir, platform)
            val markerFile = File(platformDir, ".downloaded_$FLARESOLVERR_VERSION")
            
            // Skip if already downloaded
            if (markerFile.exists()) {
                logger.lifecycle("FlareSolverr $FLARESOLVERR_VERSION for $platform already downloaded")
                return@forEach
            }
            
            logger.lifecycle("Downloading FlareSolverr $FLARESOLVERR_VERSION for $platform...")
            logger.lifecycle("URL: $url")
            
            try {
                platformDir.mkdirs()
                
                val tempFile = File.createTempFile("flaresolverr_$platform", if (url.endsWith(".zip")) ".zip" else ".tar.gz")
                tempFile.deleteOnExit()
                
                // Download with progress
                downloadWithProgress(url, tempFile)
                
                // Extract
                logger.lifecycle("Extracting to ${platformDir.absolutePath}...")
                if (url.endsWith(".zip")) {
                    extractZip(tempFile, platformDir)
                } else {
                    extractTarGz(tempFile, platformDir)
                }
                
                // Create marker file
                markerFile.writeText("Downloaded: $FLARESOLVERR_VERSION\nDate: ${java.time.LocalDateTime.now()}")
                
                // Cleanup
                tempFile.delete()
                
                logger.lifecycle("FlareSolverr $FLARESOLVERR_VERSION for $platform downloaded successfully")
                
            } catch (e: Exception) {
                logger.error("Failed to download FlareSolverr for $platform: ${e.message}")
                throw e
            }
        }
    }
    
    private fun downloadWithProgress(urlString: String, destFile: File) {
        val url = URL(urlString)
        val connection = url.openConnection()
        connection.setRequestProperty("User-Agent", "IReader-Plugin-Builder")
        
        val totalSize = connection.contentLengthLong
        var downloadedSize = 0L
        var lastProgress = 0
        
        connection.getInputStream().use { input ->
            destFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedSize += bytesRead
                    
                    if (totalSize > 0) {
                        val progress = ((downloadedSize * 100) / totalSize).toInt()
                        if (progress >= lastProgress + 10) {
                            logger.lifecycle("  Downloaded: $progress% (${downloadedSize / 1024 / 1024}MB / ${totalSize / 1024 / 1024}MB)")
                            lastProgress = progress
                        }
                    }
                }
            }
        }
    }
    
    private fun extractZip(zipFile: File, destDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val destFile = File(destDir, entry.name)
                
                if (entry.isDirectory) {
                    destFile.mkdirs()
                } else {
                    destFile.parentFile?.mkdirs()
                    destFile.outputStream().use { output ->
                        zis.copyTo(output)
                    }
                }
                
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
    
    private fun extractTarGz(tarGzFile: File, destDir: File) {
        // Use system tar command for .tar.gz extraction
        val os = System.getProperty("os.name").lowercase()
        
        if (os.contains("windows")) {
            // Windows 10+ has tar built-in
            val process = ProcessBuilder("tar", "-xzf", tarGzFile.absolutePath, "-C", destDir.absolutePath)
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val output = process.inputStream.bufferedReader().readText()
                throw RuntimeException("tar extraction failed with exit code $exitCode: $output")
            }
        } else {
            // Unix-like systems
            val process = ProcessBuilder("tar", "-xzf", tarGzFile.absolutePath, "-C", destDir.absolutePath)
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val output = process.inputStream.bufferedReader().readText()
                throw RuntimeException("tar extraction failed with exit code $exitCode: $output")
            }
        }
    }
}
