import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

/**
 * Task to generate plugin repository index.json
 */
abstract class PluginRepoTask : DefaultTask() {

    @get:OutputDirectory
    abstract val repoDir: DirectoryProperty

    @get:Input
    abstract val pluginBuildDirs: ListProperty<String>

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    @TaskAction
    fun generate() {
        val outputDir = repoDir.get().asFile
        outputDir.mkdirs()

        val plugins = mutableListOf<PluginIndexEntry>()

        // Scan all plugin build outputs
        pluginBuildDirs.get().forEach { buildDirPath ->
            val buildDir = File(buildDirPath)
            val manifestFile = File(buildDir, "generated/plugin/plugin.json")
            if (manifestFile.exists()) {
                try {
                    val manifest = json.decodeFromString<PluginManifestData>(manifestFile.readText())

                    // Find the built plugin file
                    val pluginFile = findPluginFile(buildDir)

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
                        iconUrl = manifest.iconUrl,
                        monetization = manifest.monetization,
                        downloadUrl = "plugins/${manifest.id}-${manifest.version}.iplugin",
                        fileSize = pluginFile?.length() ?: 0,
                        checksum = pluginFile?.let { calculateChecksum(it) }
                    ))

                    // Copy plugin file to repo
                    pluginFile?.let { file ->
                        val destFile = File(outputDir, "plugins/${manifest.id}-${manifest.version}.iplugin")
                        destFile.parentFile.mkdirs()
                        file.copyTo(destFile, overwrite = true)
                    }

                    logger.lifecycle("Added plugin to index: ${manifest.name}")
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
    val checksum: String? = null
)
