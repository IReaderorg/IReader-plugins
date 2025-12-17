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
 * Task to download an AAR file and extract Java classes (classes.jar) from it.
 * The extracted classes can then be included in the plugin's DEX.
 * Configuration cache compatible.
 */
abstract class ExtractAarClassesTask : DefaultTask() {
    
    @get:Input
    abstract val aarUrl: Property<String>
    
    @get:Input
    abstract val aarFileName: Property<String>
    
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty
    
    @get:OutputDirectory
    abstract val cacheDir: DirectoryProperty
    
    @TaskAction
    fun execute() {
        val url = aarUrl.get()
        val fileName = aarFileName.get()
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
        
        // Extract classes.jar from AAR
        logger.lifecycle("Extracting classes from AAR to $outDir")
        outDir.mkdirs()
        
        ZipFile(aarFile).use { aarZip ->
            // Find classes.jar in the AAR
            val classesEntry = aarZip.getEntry("classes.jar")
            if (classesEntry == null) {
                logger.warn("No classes.jar found in AAR")
                return
            }
            
            // Extract classes.jar to temp file
            val tempClassesJar = File(tmpDir, "classes-temp.jar")
            aarZip.getInputStream(classesEntry).use { input ->
                tempClassesJar.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            logger.lifecycle("Extracted classes.jar (${tempClassesJar.length()} bytes)")
            
            // Extract .class files from classes.jar
            ZipFile(tempClassesJar).use { classesZip ->
                val entries = classesZip.entries()
                var extractedCount = 0
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.endsWith(".class") && !entry.isDirectory) {
                        val outputFile = File(outDir, entry.name)
                        outputFile.parentFile.mkdirs()
                        
                        classesZip.getInputStream(entry).use { input ->
                            outputFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        extractedCount++
                    }
                }
                logger.lifecycle("Extracted $extractedCount class files")
            }
            
            // Clean up temp file
            tempClassesJar.delete()
        }
    }
}
