package io.github.ireaderorg.plugins.flaresolverr

import ireader.plugin.api.*
import ireader.plugin.annotations.IReaderPlugin
import kotlinx.serialization.json.Json

/**
 * FlareSolverr Cloudflare Bypass Plugin (Windows)
 */
@IReaderPlugin
class FlareSolverrBypassPlugin : FeaturePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.flaresolverr-bypass-windows",
        name = "FlareSolverr Bypass (Windows)",
        version = "1.0.0",
        versionCode = 1,
        description = "Cloudflare bypass using bundled FlareSolverr for Windows.",
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
        println("[FlareSolverr-Win] Plugin initialized")
    }
    
    override fun cleanup() {
        stopServer()
        pluginContext = null
    }
    
    val priority: Int = 100
    
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var serverProcess: Process? = null
    private var serverPort: Int = 8191
    private var isServerStarted = false
    
    private val platformDir = "windows-x64"
    private val executableName = "flaresolverr.exe"
    
    fun canHandleChallenge(challengeType: String): Boolean {
        return challengeType in listOf("JSChallenge", "ManagedChallenge", "TurnstileChallenge", "Unknown", "None")
    }
    
    suspend fun isAvailable(): Boolean {
        if (isServerRunning()) return true
        if (!isServerStarted) {
            startServer()
            repeat(30) {
                Thread.sleep(1000)
                if (isServerRunning()) return true
            }
        }
        return isServerRunning()
    }
    
    suspend fun bypass(url: String, postData: String? = null, timeoutMs: Long = 60000): String {
        if (!isAvailable()) {
            return """{"status":"error","reason":"FlareSolverr could not be started"}"""
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
    
    fun getStatusDescription(): String = when {
        isServerRunning() -> "Running on port $serverPort"
        serverProcess != null -> "Starting..."
        else -> "Not running"
    }
    
    private fun startServer() {
        if (serverProcess?.isAlive == true) return
        
        try {
            val executable = findExecutable() ?: return
            println("[FlareSolverr-Win] Starting: ${executable.absolutePath}")
            
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
            println("[FlareSolverr-Win] Failed to start: ${e.message}")
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
    
    private fun findExecutable(): java.io.File? {
        val locations = listOf(
            getPluginDataDir()?.let { java.io.File(it, "native/$platformDir/flaresolverr/$executableName") },
            getPluginDirectory()?.let { java.io.File(it, "native/$platformDir/flaresolverr/$executableName") }
        )
        
        for (loc in locations) {
            if (loc?.exists() == true) return loc
        }
        return extractNativeFiles()
    }
    
    private fun extractNativeFiles(): java.io.File? {
        val dataDir = getPluginDataDir() ?: return null
        val nativeDir = java.io.File(dataDir, "native/$platformDir/flaresolverr")
        val targetExe = java.io.File(nativeDir, executableName)
        
        if (targetExe.exists()) return targetExe
        
        val pluginDir = getPluginDirectory() ?: return null
        val sourceDir = java.io.File(pluginDir, "native/$platformDir/flaresolverr")
        
        if (sourceDir.exists()) {
            copyDirectory(sourceDir, nativeDir)
            if (targetExe.exists()) return targetExe
        }
        return null
    }
    
    private fun copyDirectory(source: java.io.File, target: java.io.File) {
        target.mkdirs()
        source.listFiles()?.forEach { file ->
            val targetFile = java.io.File(target, file.name)
            if (file.isDirectory) copyDirectory(file, targetFile)
            else file.copyTo(targetFile, overwrite = true)
        }
    }
    
    private fun isServerRunning(): Boolean = try {
        val conn = java.net.URL("http://localhost:$serverPort/").openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 2000
        conn.readTimeout = 2000
        val code = conn.responseCode
        conn.disconnect()
        code in 200..299
    } catch (e: Exception) { false }
    
    private fun makeHttpRequest(url: String, body: String): String {
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 180000
        conn.readTimeout = 180000
        conn.outputStream.use { it.write(body.toByteArray()) }
        return conn.inputStream.bufferedReader().readText()
    }
    
    private fun getPluginDirectory(): java.io.File? = try {
        java.io.File(this::class.java.protectionDomain.codeSource.location.toURI()).parentFile
    } catch (e: Exception) { null }
    
    private fun getPluginDataDir(): java.io.File? {
        val dir = java.io.File(System.getProperty("user.home"), "AppData/Local/IReader/plugins/flaresolverr")
        dir.mkdirs()
        return dir
    }
}
