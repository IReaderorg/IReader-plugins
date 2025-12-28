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
 * Implements both CloudflareBypassPlugin for bypass functionality and
 * ExternalResourcePlugin for on-demand download management.
 */
@IReaderPlugin
class FlareSolverrBypassPlugin : FeaturePlugin, CloudflareBypassPlugin, ExternalResourcePlugin {
    
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
        // Check if already running
        if (isServerRunning()) return true
        
        // Check if executable exists
        val executable = findExecutable()
        if (executable == null) return false
        
        // Start server if not started
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
        if (!isResourceDownloaded()) {
            return BypassResponse.ServiceUnavailable(
                reason = "FlareSolverr not downloaded",
                setupInstructions = "Please download FlareSolverr from the Cloudflare Bypass settings"
            )
        }
        
        if (!isAvailable()) {
            return BypassResponse.ServiceUnavailable(
                reason = "FlareSolverr server not running",
                setupInstructions = "Please start the FlareSolverr server from settings"
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
            
            val response = json.decodeFromString(FlareSolverrResponse.serializer(), responseJson)
            
            if (response.status == "ok" && response.solution != null) {
                BypassResponse.Success(
                    content = response.solution.response ?: "",
                    cookies = response.solution.cookies?.map { cookie ->
                        BypassCookie(
                            name = cookie.name,
                            value = cookie.value,
                            domain = cookie.domain,
                            path = cookie.path,
                            expiresAt = cookie.expiry?.toLong() ?: 0L,
                            secure = cookie.secure,
                            httpOnly = cookie.httpOnly
                        )
                    } ?: emptyList(),
                    userAgent = response.solution.userAgent ?: "",
                    finalUrl = response.solution.url,
                    statusCode = response.solution.status ?: 200
                )
            } else {
                BypassResponse.Failed(
                    reason = response.message ?: "Unknown error",
                    canRetry = true
                )
            }
        } catch (e: Exception) {
            BypassResponse.Failed(
                reason = e.message ?: "Request failed",
                canRetry = true
            )
        }
    }
    
    override fun getStatusDescription(): String = when {
        _isDownloading -> "Downloading... ${(_currentProgress?.progressPercent ?: 0)}%"
        !isResourceDownloaded() -> "Not downloaded - click to download"
        isServerRunning() -> "Running on port $serverPort"
        serverProcess != null -> "Starting..."
        else -> "Downloaded - not running"
    }
    
    // ==================== ExternalResourcePlugin Implementation ====================
    
    override val resourceInfo: ExternalResourceInfo by lazy {
        val platformInfo = PLATFORM_INFO[platform]
        ExternalResourceInfo(
            id = "flaresolverr-binary-$platform",
            name = "FlareSolverr",
            description = "Browser automation for Cloudflare bypass. Runs a headless browser to solve challenges automatically.",
            downloadUrl = platformInfo?.downloadUrl ?: "",
            estimatedSize = platformInfo?.estimatedSize ?: 500_000_000L,
            requiredFor = "Cloudflare-protected sources",
            version = FLARESOLVERR_VERSION,
            source = "Official GitHub Release",
            platforms = listOf(platform)
        )
    }
    
    override fun isResourceDownloaded(): Boolean = findExecutable() != null
    
    @Volatile
    private var _isDownloading = false
    
    override fun isDownloading(): Boolean = _isDownloading
    
    @Volatile
    private var _currentProgress: ResourceDownloadProgress? = null
    
    override fun getDownloadProgress(): ResourceDownloadProgress? = _currentProgress
    
    @Volatile
    private var _downloadCancelled = false
    
    override suspend fun downloadResource(onProgress: (ResourceDownloadProgress) -> Unit): ResourceDownloadResult {
        if (_isDownloading) {
            return ResourceDownloadResult.Failed("Download already in progress", canRetry = false)
        }
        
        val platformInfo = PLATFORM_INFO[platform]
        if (platformInfo == null) {
            return ResourceDownloadResult.PlatformNotSupported(
                currentPlatform = platform,
                supportedPlatforms = PLATFORM_INFO.keys.toList()
            )
        }
        
        _isDownloading = true
        _downloadCancelled = false
        _currentProgress = ResourceDownloadProgress(phase = ResourceDownloadPhase.PREPARING, statusMessage = "Starting download...")
        onProgress(_currentProgress!!)
        
        return try {
            val dataDir = getPluginDataDir()
            val targetDir = File(dataDir, "native/$platform/flaresolverr")
            targetDir.mkdirs()
            
            _currentProgress = ResourceDownloadProgress(phase = ResourceDownloadPhase.DOWNLOADING, statusMessage = "Downloading FlareSolverr $FLARESOLVERR_VERSION...")
            onProgress(_currentProgress!!)
            
            val tempFile = File(dataDir, "flaresolverr_download.tmp")
            
            downloadFile(platformInfo.downloadUrl, tempFile) { downloaded, total, speed ->
                if (_downloadCancelled) throw InterruptedException("Download cancelled")
                _currentProgress = ResourceDownloadProgress(
                    downloadedBytes = downloaded,
                    totalBytes = total,
                    phase = ResourceDownloadPhase.DOWNLOADING,
                    statusMessage = "Downloading...",
                    speedBytesPerSecond = speed
                )
                onProgress(_currentProgress!!)
            }
            
            if (_downloadCancelled) {
                tempFile.delete()
                return ResourceDownloadResult.Cancelled
            }
            
            _currentProgress = ResourceDownloadProgress(phase = ResourceDownloadPhase.EXTRACTING, statusMessage = "Extracting files...")
            onProgress(_currentProgress!!)
            
            // Extract based on file type
            if (platformInfo.downloadUrl.endsWith(".zip")) {
                extractZip(tempFile, targetDir)
            } else {
                extractTarGz(tempFile, targetDir)
            }
            
            // Make executable on Unix systems
            if (platform != "windows-x64") {
                val exe = File(targetDir, platformInfo.executableName)
                if (exe.exists()) {
                    exe.setExecutable(true)
                }
            }
            
            tempFile.delete()
            
            _currentProgress = ResourceDownloadProgress(phase = ResourceDownloadPhase.COMPLETE, statusMessage = "Download complete!")
            onProgress(_currentProgress!!)
            
            println("[FlareSolverr] Download complete: ${targetDir.absolutePath}")
            
            ResourceDownloadResult.Success(
                installedPath = targetDir.absolutePath,
                sizeBytes = getDownloadedSize() ?: 0L
            )
        } catch (e: InterruptedException) {
            _currentProgress = ResourceDownloadProgress(phase = ResourceDownloadPhase.CANCELLED, statusMessage = "Download cancelled")
            onProgress(_currentProgress!!)
            ResourceDownloadResult.Cancelled
        } catch (e: Exception) {
            _currentProgress = ResourceDownloadProgress(
                phase = ResourceDownloadPhase.ERROR,
                statusMessage = "Download failed: ${e.message}"
            )
            onProgress(_currentProgress!!)
            println("[FlareSolverr] Download error: ${e.message}")
            e.printStackTrace()
            ResourceDownloadResult.Failed(
                reason = e.message ?: "Unknown error",
                canRetry = true,
                exceptionMessage = e.toString()
            )
        } finally {
            _isDownloading = false
        }
    }
    
    override fun cancelDownload(): Boolean {
        if (_isDownloading) {
            _downloadCancelled = true
            return true
        }
        return false
    }
    
    override suspend fun deleteResource(): Boolean {
        return try {
            stopServer()
            val dataDir = getPluginDataDir()
            val targetDir = File(dataDir, "native/$platform/flaresolverr")
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            true
        } catch (e: Exception) {
            println("[FlareSolverr] Failed to delete resource: ${e.message}")
            false
        }
    }
    
    override fun getDownloadedSize(): Long? {
        val executable = findExecutable() ?: return null
        return executable.parentFile?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() }
    }
    
    // ==================== Internal Implementation ====================
    
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var serverProcess: Process? = null
    private var serverPort: Int = 8191
    private var isServerStarted = false
    
    private val platform: String by lazy { detectPlatform() }
    private val executableName: String by lazy { PLATFORM_INFO[platform]?.executableName ?: "flaresolverr" }
    
    // Legacy callback for backward compatibility
    var onDownloadProgress: ((Float, String) -> Unit)? = null
    
    /**
     * Legacy method - use downloadResource() instead
     */
    @Deprecated("Use downloadResource() instead", ReplaceWith("downloadResource(onProgress)"))
    fun downloadFlareSolverr(): Boolean {
        if (_isDownloading) return false
        
        val platformInfo = PLATFORM_INFO[platform]
        if (platformInfo == null) {
            return false
        }
        
        Thread {
            kotlinx.coroutines.runBlocking {
                downloadResource { progress ->
                    val progressFloat = progress.progress
                    onDownloadProgress?.invoke(progressFloat, progress.statusMessage)
                }
            }
        }.start()
        
        return true
    }
    
    /**
     * Legacy bypass method - use bypass(BypassRequest) instead
     */
    suspend fun bypassUrl(url: String, postData: String? = null, timeoutMs: Long = 60000): String {
        if (!isResourceDownloaded()) {
            return """{"status":"error","reason":"FlareSolverr not downloaded. Please download first.","needsDownload":true}"""
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
    
    private fun downloadFile(url: String, target: File, onProgress: (downloaded: Long, total: Long, speed: Long) -> Unit) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 60000
        connection.instanceFollowRedirects = true
        
        // Handle redirects manually for GitHub releases
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
            } else {
                break
            }
        }
        
        val totalSize = finalConnection.contentLengthLong
        var downloadedSize = 0L
        var lastTime = System.currentTimeMillis()
        var lastDownloaded = 0L
        var currentSpeed = 0L
        
        finalConnection.inputStream.use { input ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloadedSize += bytesRead
                    
                    // Calculate speed every second
                    val now = System.currentTimeMillis()
                    if (now - lastTime >= 1000) {
                        currentSpeed = ((downloadedSize - lastDownloaded) * 1000) / (now - lastTime)
                        lastTime = now
                        lastDownloaded = downloadedSize
                    }
                    
                    onProgress(downloadedSize, totalSize, currentSpeed)
                }
            }
        }
    }
    
    private fun extractZip(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
    
    private fun extractTarGz(tarGzFile: File, targetDir: File) {
        // Use system tar command for tar.gz extraction
        val pb = ProcessBuilder("tar", "-xzf", tarGzFile.absolutePath, "-C", targetDir.absolutePath)
        pb.redirectErrorStream(true)
        val process = pb.start()
        process.waitFor()
        
        if (process.exitValue() != 0) {
            throw RuntimeException("Failed to extract tar.gz: ${process.inputStream.bufferedReader().readText()}")
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
