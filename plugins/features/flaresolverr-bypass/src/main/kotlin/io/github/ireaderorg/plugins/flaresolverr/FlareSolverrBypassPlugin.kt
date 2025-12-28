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
 * 
 * Implements CloudflareBypassPlugin for automatic integration with the bypass manager.
 */
@IReaderPlugin
class FlareSolverrBypassPlugin : CloudflareBypassPlugin {
    
    companion object {
        const val FLARESOLVERR_VERSION = "v3.3.21"
        const val GITHUB_RELEASE_URL = "https://github.com/FlareSolverr/FlareSolverr/releases/download"
        
        val PLATFORM_INFO = mapOf(
            "windows-x64" to PlatformInfo(
                downloadUrl = "$GITHUB_RELEASE_URL/$FLARESOLVERR_VERSION/flaresolverr_windows_x64.zip",
                executableName = "flaresolverr.exe",
                estimatedSize = 620_000_000L
            ),
            "linux-x64" to PlatformInfo(
                downloadUrl = "$GITHUB_RELEASE_URL/$FLARESOLVERR_VERSION/flaresolverr_linux_x64.tar.gz",
                executableName = "flaresolverr",
                estimatedSize = 450_000_000L
            ),
            "macos-x64" to PlatformInfo(
                downloadUrl = "$GITHUB_RELEASE_URL/$FLARESOLVERR_VERSION/flaresolverr_macos_x64.tar.gz",
                executableName = "flaresolverr",
                estimatedSize = 400_000_000L
            ),
            "macos-arm64" to PlatformInfo(
                downloadUrl = "$GITHUB_RELEASE_URL/$FLARESOLVERR_VERSION/flaresolverr_macos_arm64.tar.gz",
                executableName = "flaresolverr",
                estimatedSize = 400_000_000L
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
        type = PluginType.CLOUDFLARE_BYPASS,
        permissions = listOf(PluginPermission.NETWORK, PluginPermission.STORAGE),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.DESKTOP)
    )
    
    // ==================== CloudflareBypassPlugin Implementation ====================
    
    override val priority: Int = 100
    
    override suspend fun canHandle(challenge: CloudflareChallenge): Boolean {
        return when (challenge) {
            is CloudflareChallenge.JSChallenge,
            is CloudflareChallenge.ManagedChallenge,
            is CloudflareChallenge.TurnstileChallenge,
            is CloudflareChallenge.Unknown,
            CloudflareChallenge.None -> true
            is CloudflareChallenge.BlockedIP,
            is CloudflareChallenge.RateLimited,
            is CloudflareChallenge.CaptchaChallenge -> false
        }
    }
    
    override suspend fun isAvailable(): Boolean {
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
    
    override suspend fun bypass(request: BypassRequest): BypassResponse {
        if (!isDownloaded()) {
            return BypassResponse.ServiceUnavailable(
                reason = "FlareSolverr not downloaded",
                setupInstructions = "Please download FlareSolverr from Settings > Cloudflare Bypass"
            )
        }
        if (!isAvailable()) {
            return BypassResponse.ServiceUnavailable(
                reason = "FlareSolverr could not be started",
                setupInstructions = "Check if FlareSolverr is properly installed"
            )
        }
        
        return try {
            val flareSolverrRequest = FlareSolverrRequest(
                cmd = if (request.postData != null) "request.post" else "request.get",
                url = request.url,
                maxTimeout = request.timeoutMs.toInt().coerceAtMost(180000),
                postData = request.postData
            )
            val responseJson = makeHttpRequest(
                "http://localhost:$serverPort/v1",
                json.encodeToString(FlareSolverrRequest.serializer(), flareSolverrRequest)
            )
            parseBypassResponse(responseJson)
        } catch (e: Exception) {
            BypassResponse.Failed(reason = e.message ?: "Unknown error", canRetry = true)
        }
    }
    
    override fun getStatusDescription(): String = when {
        _isDownloading -> "Downloading... ${(_downloadProgress * 100).toInt()}%"
        !isDownloaded() -> "Not downloaded - click to download"
        isServerRunning() -> "Running on port $serverPort"
        serverProcess != null -> "Starting..."
        else -> "Downloaded - not running"
    }
    
    override fun getConfigurationScreen(): PluginScreen? = null
    
    // ==================== Plugin Lifecycle ====================
    
    private var pluginContext: PluginContext? = null
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        println("[FlareSolverr] Plugin initialized")
    }
    
    override fun cleanup() {
        stopServer()
        pluginContext = null
    }
    
    // ==================== Public API for UI ====================
    
    fun isDownloaded(): Boolean = findExecutable() != null
    fun isCurrentlyDownloading(): Boolean = _isDownloading
    fun getDownloadProgressFloat(): Float = _downloadProgress
    fun getDownloadStatusMessage(): String = _downloadStatus
    fun getDownloadedSize(): Long? {
        val executable = findExecutable() ?: return null
        return executable.parentFile?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() }
    }

    
    fun downloadFlareSolverr(): Boolean {
        if (_isDownloading) return false
        
        // Check if already downloaded
        if (isDownloaded()) {
            _downloadStatus = "Already downloaded"
            _downloadProgress = 1f
            onDownloadProgress?.invoke(1f, "Already downloaded")
            println("[FlareSolverr] Already downloaded, skipping download")
            return true
        }
        
        val platformInfo = PLATFORM_INFO[platform] ?: run {
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
                
                if (_downloadCancelled) { tempFile.delete(); return@Thread }
                
                _downloadStatus = "Extracting..."
                _downloadProgress = 0.8f
                onDownloadProgress?.invoke(_downloadProgress, _downloadStatus)
                
                if (platformInfo.downloadUrl.endsWith(".zip")) extractZip(tempFile, targetDir)
                else extractTarGz(tempFile, targetDir)
                
                if (platform != "windows-x64") {
                    File(targetDir, platformInfo.executableName).takeIf { it.exists() }?.setExecutable(true)
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
            } finally {
                _isDownloading = false
            }
        }.start()
        return true
    }
    
    fun cancelDownload(): Boolean {
        if (_isDownloading) { _downloadCancelled = true; return true }
        return false
    }
    
    fun deleteDownloadedResource(): Boolean = try {
        stopServer()
        File(getPluginDataDir(), "native/$platform/flaresolverr").takeIf { it.exists() }?.deleteRecursively()
        true
    } catch (e: Exception) { println("[FlareSolverr] Failed to delete: ${e.message}"); false }
    
    // ==================== Download State ====================
    
    @Volatile private var _isDownloading = false
    @Volatile private var _downloadProgress = 0f
    @Volatile private var _downloadStatus = ""
    @Volatile private var _downloadCancelled = false
    var onDownloadProgress: ((Float, String) -> Unit)? = null
    
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
            val pb = ProcessBuilder(executable.absolutePath).directory(executable.parentFile).redirectErrorStream(true)
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
        } catch (e: Exception) { println("[FlareSolverr] Failed to start: ${e.message}") }
    }
    
    fun stopServer() {
        serverProcess?.let {
            if (it.isAlive) { it.destroy(); Thread.sleep(2000); if (it.isAlive) it.destroyForcibly() }
        }
        serverProcess = null
        isServerStarted = false
    }
    
    fun isServerRunning(): Boolean = try {
        val conn = URL("http://localhost:$serverPort/").openConnection() as HttpURLConnection
        conn.connectTimeout = 2000; conn.readTimeout = 2000
        val code = conn.responseCode; conn.disconnect()
        code in 200..299
    } catch (e: Exception) { false }
    
    // ==================== Internal Helpers ====================
    
    private fun findExecutable(): File? {
        val baseDir = File(getPluginDataDir(), "native/$platform/flaresolverr")
        println("[FlareSolverr] Looking for executable '$executableName' in: ${baseDir.absolutePath}")
        
        if (!baseDir.exists()) {
            println("[FlareSolverr] Base directory does not exist")
            return null
        }
        
        // Direct path (if extracted without root folder)
        val directExe = File(baseDir, executableName)
        if (directExe.exists() && directExe.isFile) {
            println("[FlareSolverr] Found executable at direct path: ${directExe.absolutePath}")
            return directExe
        }
        
        // Search in subdirectories (ZIP might have nested folders)
        // Common patterns: flaresolverr/flaresolverr.exe or flaresolverr/flaresolverr/flaresolverr.exe
        val searchPaths = listOf(
            File(baseDir, "flaresolverr/$executableName"),
            File(baseDir, "flaresolverr/flaresolverr/$executableName"),
            File(baseDir, "FlareSolverr/$executableName"),
            File(baseDir, "FlareSolverr/FlareSolverr/$executableName")
        )
        
        for (path in searchPaths) {
            if (path.exists() && path.isFile) {
                println("[FlareSolverr] Found executable at: ${path.absolutePath}")
                return path
            }
        }
        
        // Fallback: recursive search
        try {
            baseDir.walkTopDown()
                .maxDepth(4)
                .filter { it.isFile && it.name.equals(executableName, ignoreCase = true) }
                .firstOrNull()
                ?.let { 
                    println("[FlareSolverr] Found executable via recursive search: ${it.absolutePath}")
                    return it 
                }
        } catch (e: Exception) {
            println("[FlareSolverr] Error during recursive search: ${e.message}")
        }
        
        println("[FlareSolverr] Executable not found")
        return null
    }
    
    private fun detectPlatform(): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        return when {
            os.contains("win") -> "windows-x64"
            os.contains("mac") -> if (arch.contains("aarch64") || arch.contains("arm")) "macos-arm64" else "macos-x64"
            else -> "linux-x64"
        }
    }
    
    private fun downloadFile(url: String, target: File, onProgress: (Float) -> Unit) {
        var conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 30000; conn.readTimeout = 60000; conn.instanceFollowRedirects = true
        var redirectCount = 0
        while (redirectCount < 5) {
            if (conn.responseCode in 300..399) {
                conn = URL(conn.getHeaderField("Location")).openConnection() as HttpURLConnection
                conn.connectTimeout = 30000; conn.readTimeout = 60000
                redirectCount++
            } else break
        }
        val totalSize = conn.contentLengthLong
        var downloadedSize = 0L
        conn.inputStream.use { input ->
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
                else { file.parentFile?.mkdirs(); FileOutputStream(file).use { zis.copyTo(it) } }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
    
    private fun extractTarGz(tarGzFile: File, targetDir: File) {
        val pb = ProcessBuilder("tar", "-xzf", tarGzFile.absolutePath, "-C", targetDir.absolutePath).redirectErrorStream(true)
        val process = pb.start()
        process.waitFor()
        if (process.exitValue() != 0) throw RuntimeException("Failed to extract: ${process.inputStream.bufferedReader().readText()}")
    }
    
    private fun makeHttpRequest(url: String, body: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true; conn.connectTimeout = 180000; conn.readTimeout = 180000
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
        return File(baseDir, "plugins/flaresolverr").also { it.mkdirs() }
    }
    
    private fun parseBypassResponse(responseJson: String): BypassResponse = try {
        val response = json.decodeFromString<FlareSolverrResponse>(responseJson)
        when (response.status) {
            "ok" -> response.solution?.let { solution ->
                BypassResponse.Success(
                    content = solution.response ?: "",
                    cookies = solution.cookies?.map { BypassCookie(it.name, it.value, it.domain, it.path, it.expiry?.toLong() ?: 0L, it.secure, it.httpOnly) } ?: emptyList(),
                    userAgent = solution.userAgent ?: "",
                    finalUrl = solution.url,
                    statusCode = solution.status ?: 200
                )
            } ?: BypassResponse.Failed("No solution in response", canRetry = true)
            "error" -> if (responseJson.contains("needsDownload\":true"))
                BypassResponse.ServiceUnavailable(response.message ?: "FlareSolverr not downloaded", "Please download FlareSolverr from Settings > Cloudflare Bypass")
            else BypassResponse.Failed(response.message ?: "Unknown error", canRetry = true)
            else -> BypassResponse.Failed("Unknown status: ${response.status}", canRetry = true)
        }
    } catch (e: Exception) {
        BypassResponse.Failed(Regex(""""reason"\s*:\s*"([^"]+)"""").find(responseJson)?.groupValues?.get(1) ?: e.message ?: "Failed to parse response", canRetry = true)
    }
}
