import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

/**
 * Task to generate plugin repository index.json
 * 
 * Supports remote icon URLs:
 * - If assets/icon.png exists in plugin source dir, copies it to repo/images/{pluginId}.png
 * - If iconUrl is set in plugin config, uses that URL directly
 * - If baseIconUrl is set, generates URL as: {baseIconUrl}/images/{pluginId}.png
 * - Otherwise, iconUrl is null (no icon)
 */
abstract class PluginRepoTask : DefaultTask() {

    @get:OutputDirectory
    abstract val repoDir: DirectoryProperty

    @get:Input
    abstract val pluginBuildDirs: ListProperty<String>
    
    /**
     * Plugin source directories (to find assets/icon.png)
     * Maps plugin ID to source directory path
     */
    @get:Input
    @get:Optional
    abstract val pluginSourceDirs: ListProperty<String>
    
    /**
     * Base URL for plugin icons (e.g., "https://raw.githubusercontent.com/IReaderorg/IReader-plugins/gh-pages/repo")
     * Icons will be served from {baseIconUrl}/images/{pluginId}.png
     */
    @get:Input
    @get:Optional
    abstract val baseIconUrl: Property<String>

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    @TaskAction
    fun generate() {
        val outputDir = repoDir.get().asFile
        outputDir.mkdirs()
        
        // Create images directory
        val imagesDir = File(outputDir, "images")
        imagesDir.mkdirs()

        val plugins = mutableListOf<PluginIndexEntry>()
        val baseUrl = baseIconUrl.orNull
        
        // Build a map of source directories for icon lookup
        val sourceDirMap = buildSourceDirMap()

        // Scan all plugin build outputs
        pluginBuildDirs.get().forEach { buildDirPath ->
            val buildDir = File(buildDirPath)
            val manifestFile = File(buildDir, "generated/plugin/plugin.json")
            if (manifestFile.exists()) {
                try {
                    val manifest = json.decodeFromString<PluginManifestData>(manifestFile.readText())

                    // Find the built plugin file
                    val pluginFile = findPluginFile(buildDir)
                    
                    // Try to copy icon from assets/icon.png if it exists
                    val iconCopied = copyPluginIcon(manifest.id, sourceDirMap, imagesDir)
                    
                    // Determine icon URL
                    val iconUrl = when {
                        // Use explicit iconUrl from manifest if set
                        !manifest.iconUrl.isNullOrBlank() -> manifest.iconUrl
                        // If icon was copied or baseUrl is set, generate URL
                        iconCopied && !baseUrl.isNullOrBlank() -> "$baseUrl/images/${manifest.id}.png"
                        !baseUrl.isNullOrBlank() && File(imagesDir, "${manifest.id}.png").exists() -> "$baseUrl/images/${manifest.id}.png"
                        // No icon
                        else -> null
                    }

                    plugins.add(PluginIndexEntry(
                        id = manifest.id,
                        name = manifest.name,
                        version = manifest.version,
                        versionCode = manifest.versionCode,
                        description = manifest.description,
                        author = manifest.author,
                        type = manifest.type,
                        permissions = manifest.permissions,
                        minIReaderVersion = manifest.minIReaderVersion,
                        platforms = manifest.platforms,
                        iconUrl = iconUrl,
                        monetization = manifest.monetization,
                        downloadUrl = "plugins/${manifest.id}-${manifest.version}.iplugin",
                        fileSize = pluginFile?.length() ?: 0,
                        checksum = pluginFile?.let { calculateChecksum(it) },
                        featured = manifest.featured,
                        tags = manifest.tags
                    ))

                    // Copy plugin file to repo
                    pluginFile?.let { file ->
                        val destFile = File(outputDir, "plugins/${manifest.id}-${manifest.version}.iplugin")
                        destFile.parentFile.mkdirs()
                        file.copyTo(destFile, overwrite = true)
                    }

                    val iconStatus = if (iconCopied) "✓ icon copied" else if (iconUrl != null) "icon: $iconUrl" else "no icon"
                    logger.lifecycle("Added plugin to index: ${manifest.name} ($iconStatus)")
                } catch (e: Exception) {
                    logger.warn("Failed to process plugin from $buildDirPath: ${e.message}")
                }
            }
        }

        val index = PluginIndex(
            version = 1,
            plugins = plugins
        )

        val indexFile = File(outputDir, "index.json")
        indexFile.writeText(json.encodeToString(index))

        logger.lifecycle("Generated plugin index with ${plugins.size} plugins: ${indexFile.absolutePath}")
        logger.lifecycle("Images directory: ${imagesDir.absolutePath}")
    }
    
    /**
     * Build a map from plugin source directories
     * Looks for assets/icon.png in each source directory
     */
    private fun buildSourceDirMap(): Map<String, File> {
        val map = mutableMapOf<String, File>()
        pluginSourceDirs.orNull?.forEach { sourceDirPath ->
            val sourceDir = File(sourceDirPath)
            if (sourceDir.exists() && sourceDir.isDirectory) {
                // The source dir path is the plugin project directory
                // We'll map it by directory name for now, actual ID matching happens in copyPluginIcon
                map[sourceDir.absolutePath] = sourceDir
            }
        }
        return map
    }
    
    /**
     * Copy plugin icon from assets/icon.png to repo images directory
     * Returns true if icon was successfully copied
     */
    private fun copyPluginIcon(pluginId: String, sourceDirMap: Map<String, File>, imagesDir: File): Boolean {
        // Look through all source directories for matching icon
        for ((_, sourceDir) in sourceDirMap) {
            val iconFile = File(sourceDir, "assets/icon.png")
            if (iconFile.exists()) {
                // Check if this source dir's build.gradle.kts contains this plugin ID
                val buildFile = File(sourceDir, "build.gradle.kts")
                if (buildFile.exists()) {
                    val buildContent = buildFile.readText()
                    if (buildContent.contains(pluginId) || buildContent.contains(pluginId.substringAfterLast("."))) {
                        val destFile = File(imagesDir, "$pluginId.png")
                        iconFile.copyTo(destFile, overwrite = true)
                        logger.lifecycle("  Copied icon: ${iconFile.absolutePath} -> ${destFile.name}")
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun findPluginFile(buildDir: File): File? {
        val outputsDir = File(buildDir, "outputs")
        return outputsDir.walkTopDown()
            .filter { it.isFile && it.extension == "iplugin" }
            .firstOrNull()
    }

    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return "sha256:" + digest.digest().joinToString("") { "%02x".format(it) }
    }
}

@Serializable
data class PluginIndex(
    val version: Int,
    val plugins: List<PluginIndexEntry>
)

@Serializable
data class PluginIndexEntry(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Int,
    val description: String,
    val author: AuthorData,
    val type: String,
    val permissions: List<String>,
    val minIReaderVersion: String,
    val platforms: List<String>,
    val iconUrl: String? = null,
    val monetization: MonetizationData? = null,
    val downloadUrl: String,
    val fileSize: Long,
    val checksum: String? = null,
    val featured: Boolean = false,
    val tags: List<String>? = null
)
