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
 * Task to download an AAR file and extract native libraries from it.
 * Configuration cache compatible.
 */
abstract class DownloadAndExtractAarTask : DefaultTask() {
    
    @get:Input
    abstract val aarUrl: Property<String>
    
    @get:Input
    abstract val aarFileName: Property<String>
    
    @get:Input
    abstract val sourcePrefix: Property<String>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:OutputDirectory
    abstract val cacheDir: DirectoryProperty
    
    init {
        sourcePrefix.convention("jni/")
    }
    
    @TaskAction
    fun execute() {
        val url = aarUrl.get()
        val fileName = aarFileName.get()
        val prefix = sourcePrefix.get()
        val outDir = outputDir.get().asFile
        
        // Download AAR to cache directory
        val tmpDir = cacheDir.get().asFile
        tmpDir.mkdirs()
        val aarFile = File(tmpDir, fileName)
        
        if (!aarFile.exists()) {
            logger.lifecycle("Downloading AAR from $url")
            URI(url).toURL().openStream().use { input ->
                aarFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            logger.lifecycle("Downloaded: ${aarFile.name} (${aarFile.length()} bytes)")
        } else {
            logger.lifecycle("Using cached AAR: ${aarFile.name}")
        }
        
        // Extract native libraries
        logger.lifecycle("Extracting native libraries to $outDir")
        outDir.mkdirs()
        
        ZipFile(aarFile).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.name.startsWith(prefix) && entry.name.endsWith(".so")) {
                    val relativePath = entry.name.removePrefix(prefix)
                    val outputFile = File(outDir, relativePath)
                    outputFile.parentFile.mkdirs()
                    
                    zip.getInputStream(entry).use { input ->
                        outputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    logger.lifecycle("Extracted: $relativePath (${outputFile.length()} bytes)")
                }
            }
        }
    }
}
