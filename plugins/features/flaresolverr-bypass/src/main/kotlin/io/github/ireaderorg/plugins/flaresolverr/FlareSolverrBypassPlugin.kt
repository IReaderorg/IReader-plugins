package io.github.ireaderorg.plugins.flaresolverr

import ireader.plugin.api.*
import ireader.plugin.annotations.IReaderPlugin
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import kotlin.concurrent.Volatile

/**
 * FlareSolverr Cloudflare Bypass Plugin
 * 
 * This plugin downloads FlareSolverr binaries on-demand from GitHub releases
 * instead of bundling them, keeping the plugin size small.
 */
@IReaderPlugin
class FlareSolverrBypassPlugin : FeaturePlugin {
    
    companion object {
        // FlareSolverr release info - update these when new versions are available
        const val FLARESOLVERR_VERSION = "v3.3.21"
        const val GITHUB_RELEASE_URL = "https://github.com/FlareSolverr/FlareSolverr/releases/download"
        
        // Platform-specific download URLs and sizes
        val PLATFORM_INFO = mapOf(
            "windows-x64" to PlatformInfo(
                downloadUrl = "$GITHUB_RELEASE_URL/$FLARESOLVERR_VERSION/flaresolverr_windows_x64.zip",
                executableName = "flaresolverr.exe",
                estimatedSize = 620_000_000L // ~620 MB
            ),
            "linux-x64" to PlatformInfo(
                downloadUrl = "$GITHUB_RELEASE_URL/$FLARESOLVERR_VERSION/flaresolverr_linux_x64.tar.gz",
                executableName = "flaresolverr",
                estimatedSize = 450_000_000L // ~450 MB
            ),
            "macos-x64" to PlatformInfo(
                downloadUrl = "$GITHUB_RELEASE_URL/$FLARESOLVERR_VERSION/flaresolverr_macos_x64.tar.gz",
                executableName = "flaresolverr",
                estimatedSize = 400_000_000L // ~400 MB
            ),
            "macos-arm64" to PlatformInfo(
                downloadUrl = "$GITHUB_RELEASE_URL/$FLARESOLVERR_VERSION/flaresolverr_macos_arm64.tar.gz",
                executableName = "flaresolverr",
                estimatedSize = 400_000_000L // ~400 MB
            )
        )
        
        data class PlatformInfo(
            val downloadUrl: String,
            val executableName: String,
            val estimatedSize: Long
        )
    }
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.flaresolverr-bypass",
        name = "FlareSolverr Bypass",
        version = "2.0.0",
        versionCode = 2,
        description = "Cloudflare bypass using FlareSolverr. Downloads automatically on first use.",
        author = PluginAuthor(name = "IReader Team"),
        type = PluginType.FEATURE,
        permissions = listOf(PluginPermission.NETWORK, PluginPermission.STORAGE),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.DESKTOP)
    )
    
    override fun getMenuItems(): List<PluginMenuItem> = emptyList()
    override fun getScreens(): List<PluginScreen> = emptyList()
    override fun onReaderContext(context: ReaderContext): PluginAction? = null
    
    private var pluginContext: PluginContext? = null
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        println("[FlareSolverr] Plugin initialized")
    }
    
    override fun cleanup() {
        stopServer()
        pluginContext = null
    }
    
    // ==================== Public API ====================
    
    /** Priority for bypass selection (higher = preferred) */
    val priority: Int = 100
    
    /** Check if this plugin can handle a challenge type */
    fun canHandle(challengeType: String): Boolean {
        return challengeType in listOf("JSChallenge", "ManagedChallenge", "TurnstileChallenge", "Unknown", "None")
    }
    
    /** Check if FlareSolverr is available and running */
    suspend fun isAvailable(): Boolean {
        if (isServerRunning()) return true
        
        val executable = findExecutable() ?: return false
        
        if (!isServerStarted) {
            startServer()
            repeat(30) {
                Thread.sleep(1000)
                if (isServerRunning()) return true
            }
        }
        return isServerRunning()
    }
    
    /** Check if FlareSolverr binaries are downloaded */
    fun isDownloaded(): Boolean = findExecutable() != null
    
    /** Check if currently downloading */
    fun isCurrentlyDownloading(): Boolean = _isDownloading
    
    /** Get download progress (0.0 to 1.0) */
    fun getDownloadProgressFloat(): Float = _downloadProgress
    
    /** Get download status message */
    fun getDownloadStatusMessage(): String = _downloadStatus
    
    /** Get human-readable status */
    fun getStatusDescription(): String = when {
        _isDownloading -> "Downloading... ${(_downloadProgress * 100).toInt()}%"
        !isDownloaded() -> "Not downloaded - click to download"
        isServerRunning() -> "Running on port $serverPort"
        serverProcess != null -> "Starting..."
        else -> "Downloaded - not running"
    }
    
    /** Get resource info for UI display */
    fun getResourceInfo(): ResourceInfo {
        val platformInfo = PLATFORM_INFO[platform]
        return ResourceInfo(
            id = "flaresolverr-binary-$platform",
            name = "FlareSolverr",
            description = "Browser automation for Cloudflare bypass",
            downloadUrl = platformInfo?.downloadUrl ?: "",
            estimatedSize = platformInfo?.estimatedSize ?: 500_000_000L,
            version = FLARESOLVERR_VERSION
        )
    }
    
    data class ResourceInfo(
        val id: String,
        val name: String,
        val description: String,
        val downloadUrl: String,
        val estimatedSize: Long,
        val version: String
    ) {
        val estimatedSizeFormatted: String
            get() {
                val mb = estimatedSize / 1024.0 / 1024.0
                return if (mb >= 1000) String.format("%.1f GB", mb / 1024.0) else String.format("%.0f MB", mb)
            }
    }
    
    // ==================== Download Management ====================
    
    @Volatile private var _isDownloading = false
    @Volatile private var _downloadProgress = 0f
    @Volatile private var _downloadStatus = ""
    @Volatile private var _downloadCancelled = false
    
    var onDownloadProgress: ((Float, String) -> Unit)? = null
    
    /** Start downloading FlareSolverr binaries */
    fun downloadFlareSolverr(): Boolean {
        if (_isDownloading) return false
        
        val platformInfo = PLATFORM_INFO[platform]
        if (platformInfo == null) {
            _downloadStatus = "Unsupported platform: $platform"
            return false
        }
        
        _isDownloading = true
        _downloadCancelled = false
        _downloadProgress = 0f
        _downloadStatus = "Starting download..."
        
        Thread {
            try {
                val dataDir = getPluginDataDir()
                val targetDir = File(dataDir, "native/$platform/flaresolverr")
                targetDir.mkdirs()
                
                _downloadStatus = "Downloading FlareSolverr $FLARESOLVERR_VERSION..."
                onDownloadProgress?.invoke(0f, _downloadStatus)
                
                val tempFile = File(dataDir, "flaresolverr_download.tmp")
                downloadFile(platformInfo.downloadUrl, tempFile) { progress ->
                    if (_downloadCancelled) throw InterruptedException("Download cancelled")
                    _downloadProgress = progress * 0.8f
                    onDownloadProgress?.invoke(_downloadProgress, "Downloading: ${(progress * 100).toInt()}%")
                }
                
                if (_downloadCancelled) {
                    tempFile.delete()
                    return@Thread
                }
                
                _downloadStatus = "Extracting..."
                _downloadProgress = 0.8f
                onDownloadProgress?.invoke(_downloadProgress, _downloadStatus)
                
                if (platformInfo.downloadUrl.endsWith(".zip")) {
                    extractZip(tempFile, targetDir)
                } else {
                    extractTarGz(tempFile, targetDir)
                }
                
                if (platform != "windows-x64") {
                    val exe = File(targetDir, platformInfo.executableName)
                    if (exe.exists()) exe.setExecutable(true)
                }
                
                tempFile.delete()
                
                _downloadProgress = 1f
                _downloadStatus = "Download complete!"
                onDownloadProgress?.invoke(_downloadProgress, _downloadStatus)
                
                println("[FlareSolverr] Download complete: ${targetDir.absolutePath}")
                
            } catch (e: InterruptedException) {
                _downloadStatus = "Download cancelled"
                _downloadProgress = 0f
                onDownloadProgress?.invoke(_downloadProgress, _downloadStatus)
            } catch (e: Exception) {
                _downloadStatus = "Download failed: ${e.message}"
                _downloadProgress = 0f
                onDownloadProgress?.invoke(_downloadProgress, _downloadStatus)
                println("[FlareSolverr] Download error: ${e.message}")
                e.printStackTrace()
            } finally {
                _isDownloading = false
            }
        }.start()
        
        return true
    }
    
    /** Cancel ongoing download */
    fun cancelDownload(): Boolean {
        if (_isDownloading) {
            _downloadCancelled = true
            return true
        }
        return false
    }
    
    /** Delete downloaded resources */
    fun deleteDownloadedResource(): Boolean {
        return try {
            stopServer()
            val dataDir = getPluginDataDir()
            val targetDir = File(dataDir, "native/$platform/flaresolverr")
            if (targetDir.exists()) targetDir.deleteRecursively()
            true
        } catch (e: Exception) {
            println("[FlareSolverr] Failed to delete: ${e.message}")
            false
        }
    }
    
    /** Get size of downloaded resource */
    fun getDownloadedSize(): Long? {
        val executable = findExecutable() ?: return null
        return executable.parentFile?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() }
    }
    
    // ==================== Bypass Functionality ====================
    
    /** Bypass Cloudflare protection for a URL */
    suspend fun bypass(url: String, postData: String? = null, timeoutMs: Long = 60000): String {
        if (!isDownloaded()) {
            return """{"status":"error","reason":"FlareSolverr not downloaded","needsDownload":true}"""
        }
        if (!isAvailable()) {
            return """{"status":"error","reason":"FlareSolverr could not be started","needsDownload":false}"""
        }
        return try {
            val request = FlareSolverrRequest(
                cmd = if (postData != null) "request.post" else "request.get",
                url = url,
                maxTimeout = timeoutMs.toInt().coerceAtMost(180000),
                postData = postData
            )
            makeHttpRequest("http://localhost:$serverPort/v1", json.encodeToString(FlareSolverrRequest.serializer(), request))
        } catch (e: Exception) {
            """{"status":"error","reason":"${e.message}"}"""
        }
    }
    
    // ==================== Server Management ====================
    
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var serverProcess: Process? = null
    private var serverPort: Int = 8191
    private var isServerStarted = false
    
    private val platform: String by lazy { detectPlatform() }
    private val executableName: String by lazy { PLATFORM_INFO[platform]?.executableName ?: "flaresolverr" }
    
    fun startServer() {
        if (serverProcess?.isAlive == true) return
        
        try {
            val executable = findExecutable() ?: return
            println("[FlareSolverr] Starting: ${executable.absolutePath}")
            
            val pb = ProcessBuilder(executable.absolutePath)
                .directory(executable.parentFile)
                .redirectErrorStream(true)
            
            pb.environment().apply {
                put("PORT", serverPort.toString())
                put("HOST", "0.0.0.0")
                put("LOG_LEVEL", "info")
                put("HEADLESS", "true")
            }
            
            serverProcess = pb.start()
            isServerStarted = true
            
            Thread { serverProcess?.inputStream?.bufferedReader()?.forEachLine { println("[FlareSolverr] $it") } }.start()
            Runtime.getRuntime().addShutdownHook(Thread { stopServer() })
        } catch (e: Exception) {
            println("[FlareSolverr] Failed to start: ${e.message}")
        }
    }
    
    fun stopServer() {
        serverProcess?.let {
            if (it.isAlive) {
                it.destroy()
                Thread.sleep(2000)
                if (it.isAlive) it.destroyForcibly()
            }
        }
        serverProcess = null
        isServerStarted = false
    }
    
    fun isServerRunning(): Boolean = try {
        val conn = URL("http://localhost:$serverPort/").openConnection() as HttpURLConnection
        conn.connectTimeout = 2000
        conn.readTimeout = 2000
        val code = conn.responseCode
        conn.disconnect()
        code in 200..299
    } catch (e: Exception) { false }
    
    // ==================== Internal Helpers ====================
    
    private fun findExecutable(): File? {
        val dataDir = getPluginDataDir()
        val exe = File(dataDir, "native/$platform/flaresolverr/$executableName")
        return if (exe.exists() && exe.canExecute()) exe else null
    }
    
    private fun detectPlatform(): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        return when {
            os.contains("win") -> "windows-x64"
            os.contains("mac") -> if (arch.contains("aarch64") || arch.contains("arm")) "macos-arm64" else "macos-x64"
            os.contains("linux") -> "linux-x64"
            else -> "linux-x64"
        }
    }
    
    private fun downloadFile(url: String, target: File, onProgress: (Float) -> Unit) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 60000
        connection.instanceFollowRedirects = true
        
        var finalConnection = connection
        var redirectCount = 0
        while (redirectCount < 5) {
            val responseCode = finalConnection.responseCode
            if (responseCode in 300..399) {
                val newUrl = finalConnection.getHeaderField("Location")
                finalConnection = URL(newUrl).openConnection() as HttpURLConnection
                finalConnection.connectTimeout = 30000
                finalConnection.readTimeout = 60000
                redirectCount++
            } else break
        }
        
        val totalSize = finalConnection.contentLengthLong
        var downloadedSize = 0L
        
        finalConnection.inputStream.use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedSize += bytesRead
                    if (totalSize > 0) onProgress(downloadedSize.toFloat() / totalSize)
                }
            }
        }
    }
    
    private fun extractZip(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                if (entry.isDirectory) file.mkdirs()
                else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
    
    private fun extractTarGz(tarGzFile: File, targetDir: File) {
        val pb = ProcessBuilder("tar", "-xzf", tarGzFile.absolutePath, "-C", targetDir.absolutePath)
        pb.redirectErrorStream(true)
        val process = pb.start()
        process.waitFor()
        if (process.exitValue() != 0) {
            throw RuntimeException("Failed to extract: ${process.inputStream.bufferedReader().readText()}")
        }
    }
    
    private fun makeHttpRequest(url: String, body: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 180000
        conn.readTimeout = 180000
        conn.outputStream.use { it.write(body.toByteArray()) }
        return conn.inputStream.bufferedReader().readText()
    }
    
    private fun getPluginDataDir(): File {
        val os = System.getProperty("os.name").lowercase()
        val baseDir = when {
            os.contains("win") -> File(System.getenv("LOCALAPPDATA") ?: System.getProperty("user.home"), "IReader")
            os.contains("mac") -> File(System.getProperty("user.home"), "Library/Application Support/IReader")
            else -> File(System.getProperty("user.home"), ".local/share/IReader")
        }
        val dir = File(baseDir, "plugins/flaresolverr")
        dir.mkdirs()
        return dir
    }
}
