import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Task to generate plugin.json manifest file
 */
abstract class PluginManifestGenerator : DefaultTask() {
    
    @get:Input
    abstract val pluginId: Property<String>
    
    @get:Input
    abstract val pluginName: Property<String>
    
    @get:Input
    abstract val pluginVersion: Property<String>
    
    @get:Input
    abstract val pluginVersionCode: Property<Int>
    
    @get:Input
    abstract val pluginDescription: Property<String>
    
    @get:Input
    abstract val pluginAuthor: Property<String>
    
    @get:Input
    abstract val pluginAuthorEmail: Property<String>
    
    @get:Input
    abstract val pluginAuthorWebsite: Property<String>
    
    @get:Input
    abstract val pluginType: Property<String>
    
    @get:Input
    abstract val pluginPermissions: ListProperty<String>
    
    @get:Input
    abstract val minIReaderVersion: Property<String>
    
    @get:Input
    abstract val pluginIconUrl: Property<String>
    
    @get:Input
    abstract val monetizationType: Property<String>
    
    @get:Input
    abstract val price: Property<Double>
    
    @get:Input
    abstract val currency: Property<String>
    
    @get:Input
    abstract val trialDays: Property<Int>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    private val json = Json { 
        prettyPrint = true 
        encodeDefaults = true
    }
    
    @TaskAction
    fun generate() {
        val manifest = PluginManifestData(
            id = pluginId.get(),
            name = pluginName.get(),
            version = pluginVersion.get(),
            versionCode = pluginVersionCode.get(),
            description = pluginDescription.get(),
            author = AuthorData(
                name = pluginAuthor.get(),
                email = pluginAuthorEmail.orNull?.takeIf { it.isNotBlank() },
                website = pluginAuthorWebsite.orNull?.takeIf { it.isNotBlank() }
            ),
            type = pluginType.get(),
            permissions = pluginPermissions.get(),
            minIReaderVersion = minIReaderVersion.get(),
            platforms = listOf("ANDROID", "IOS", "DESKTOP"),
            iconUrl = pluginIconUrl.orNull?.takeIf { it.isNotBlank() },
            monetization = createMonetization()
        )
        
        val outputFile = File(outputDir.get().asFile, "plugin.json")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(json.encodeToString(manifest))
        
        logger.lifecycle("Generated plugin manifest: ${outputFile.absolutePath}")
    }
    
    private fun createMonetization(): MonetizationData? {
        return when (monetizationType.orNull) {
            "PREMIUM" -> MonetizationData(
                type = "PREMIUM",
                price = price.orNull,
                currency = currency.orNull,
                trialDays = trialDays.orNull
            )
            "FREEMIUM" -> MonetizationData(type = "FREEMIUM")
            "FREE" -> MonetizationData(type = "FREE")
            else -> null
        }
    }
}

@Serializable
data class PluginManifestData(
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
    val monetization: MonetizationData? = null
)

@Serializable
data class AuthorData(
    val name: String,
    val email: String? = null,
    val website: String? = null
)

@Serializable
data class MonetizationData(
    val type: String,
    val price: Double? = null,
    val currency: String? = null,
    val trialDays: Int? = null
)
