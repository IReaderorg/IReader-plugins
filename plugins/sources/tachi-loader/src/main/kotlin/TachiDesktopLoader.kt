package io.github.ireaderorg.plugins.tachiloader

import ireader.plugin.api.PluginContext
import ireader.plugin.api.TachiExtensionException
import ireader.plugin.api.TachiValidationResult
import ireader.plugin.api.tachi.*
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.zip.ZipFile

/**
 * Desktop implementation of Tachi extension loader.
 * Uses dex2jar to convert APK DEX files to JAR, then loads via URLClassLoader.
 */
class TachiDesktopLoader(
    private val context: PluginContext
) : TachiPlatformLoader {
    
    companion object {
        const val EXTENSION_FEATURE = "tachiyomi.extension"
        const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"
        const val METADATA_NSFW = "tachiyomi.extension.nsfw"
        const val LIB_VERSION_MIN = 1.3
        const val LIB_VERSION_MAX = 1.5
    }
    
    private val classLoaders = mutableMapOf<String, URLClassLoader>()
    private val extensionsDir: File by lazy {
        File(context.dataDir, "tachi-extensions").apply { mkdirs() }
    }
    
    override suspend fun loadExtension(apkPath: String): LoadedExtension {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            throw TachiExtensionException("APK file not found: $apkPath")
        }
        
        // Parse APK metadata
        val metadata = parseApkMetadata(apkFile)
        
        // Validate lib version
        val libVersion = metadata.versionName.substringBeforeLast('.').toDoubleOrNull() ?: 0.0
        if (libVersion < LIB_VERSION_MIN || libVersion > LIB_VERSION_MAX) {
            throw TachiExtensionException(
                "Unsupported lib version: $libVersion (supported: $LIB_VERSION_MIN-$LIB_VERSION_MAX)"
            )
        }
        
        // Convert DEX to JAR
        val jarFile = convertDexToJar(apkFile, metadata.pkgName)
        
        // Load classes
        val classLoader = URLClassLoader(
            arrayOf(jarFile.toURI().toURL()),
            this::class.java.classLoader
        )
        classLoaders[metadata.pkgName] = classLoader
        
        // Instantiate sources
        val sources = instantiateSources(classLoader, metadata)
        
        return LoadedExtension(
            pkgName = metadata.pkgName,
            name = metadata.name,
            versionName = metadata.versionName,
            versionCode = metadata.versionCode,
            lang = determineLang(sources),
            isNsfw = metadata.isNsfw,
            sources = sources,
            iconUrl = extractIcon(apkFile, metadata.pkgName)
        )
    }
    
    override fun unloadExtension(pkgName: String) {
        classLoaders.remove(pkgName)?.close()
        
        // Clean up files
        val jarFile = File(extensionsDir, "$pkgName.jar")
        jarFile.delete()
    }
    
    override fun validateApk(apkPath: String): TachiValidationResult {
        return try {
            val apkFile = File(apkPath)
            if (!apkFile.exists()) {
                return TachiValidationResult.Invalid("File not found")
            }
            
            val metadata = parseApkMetadata(apkFile)
            
            // Check if it's a Tachi extension
            if (!metadata.isTachiExtension) {
                return TachiValidationResult.Invalid("Not a Tachiyomi extension")
            }
            
            // Check lib version
            val libVersion = metadata.versionName.substringBeforeLast('.').toDoubleOrNull() ?: 0.0
            if (libVersion < LIB_VERSION_MIN || libVersion > LIB_VERSION_MAX) {
                return TachiValidationResult.UnsupportedVersion(libVersion)
            }
            
            TachiValidationResult.Valid(
                pkgName = metadata.pkgName,
                name = metadata.name,
                libVersion = libVersion
            )
        } catch (e: Exception) {
            TachiValidationResult.Invalid(e.message ?: "Validation failed")
        }
    }
    
    private fun parseApkMetadata(apkFile: File): ApkMetadata {
        ZipFile(apkFile).use { zip ->
            // Read AndroidManifest.xml (binary XML, need to parse)
            val manifestEntry = zip.getEntry("AndroidManifest.xml")
                ?: throw TachiExtensionException("No AndroidManifest.xml found")
            
            // For now, use a simplified approach - extract from filename pattern
            // Real implementation would parse binary XML
            val fileName = apkFile.nameWithoutExtension
            val parts = fileName.split("-")
            
            // Try to find classes.dex to verify it's an APK
            val dexEntry = zip.getEntry("classes.dex")
                ?: throw TachiExtensionException("No classes.dex found")
            
            // Extract package name from APK structure
            // Tachi extensions follow pattern: tachiyomi-{lang}-{name}-v{version}
            val pkgName = "eu.kanade.tachiyomi.extension.${parts.getOrElse(1) { "all" }}.${parts.getOrElse(2) { "unknown" }}"
            val name = parts.getOrElse(2) { "Unknown" }.replace("_", " ")
            val version = parts.lastOrNull()?.removePrefix("v") ?: "1.0.0"
            
            return ApkMetadata(
                pkgName = pkgName,
                name = name,
                versionName = version,
                versionCode = 1,
                mainClass = "$pkgName.${name.replace(" ", "")}",
                isNsfw = false,
                isTachiExtension = true
            )
        }
    }
    
    private fun convertDexToJar(apkFile: File, pkgName: String): File {
        val jarFile = File(extensionsDir, "$pkgName.jar")
        
        // Extract classes.dex from APK
        val dexFile = File(extensionsDir, "$pkgName.dex")
        ZipFile(apkFile).use { zip ->
            val dexEntry = zip.getEntry("classes.dex")
                ?: throw TachiExtensionException("No classes.dex found")
            
            zip.getInputStream(dexEntry).use { input ->
                dexFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        
        // Convert DEX to JAR using dex2jar
        // Note: This requires dex2jar library to be available
        try {
            convertDex2Jar(dexFile, jarFile)
        } finally {
            dexFile.delete()
        }
        
        // Extract assets from APK and add to JAR
        extractAssetsToJar(apkFile, jarFile)
        
        return jarFile
    }
    
    private fun convertDex2Jar(dexFile: File, jarFile: File) {
        // Use dex2jar library
        // This is a simplified version - real implementation needs dex2jar dependency
        try {
            val dex2jarClass = Class.forName("com.googlecode.d2j.dex.Dex2jar")
            val fromMethod = dex2jarClass.getMethod("from", ByteArray::class.java)
            val toMethod = dex2jarClass.getMethod("to", java.nio.file.Path::class.java)
            
            val dexBytes = dexFile.readBytes()
            val dex2jar = fromMethod.invoke(null, dexBytes)
            toMethod.invoke(dex2jar, jarFile.toPath())
        } catch (e: ClassNotFoundException) {
            // Fallback: try to use command-line dex2jar if available
            val process = ProcessBuilder(
                "d2j-dex2jar.sh", "-f", "-o", jarFile.absolutePath, dexFile.absolutePath
            ).start()
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw TachiExtensionException("dex2jar conversion failed (exit code: $exitCode)")
            }
        }
    }
    
    private fun extractAssetsToJar(apkFile: File, jarFile: File) {
        // Extract assets from APK and add to JAR
        ZipFile(apkFile).use { apk ->
            val assetsEntries = apk.entries().asSequence()
                .filter { it.name.startsWith("assets/") && !it.isDirectory }
                .toList()
            
            if (assetsEntries.isEmpty()) return
            
            // Add assets to existing JAR
            val tempJar = File(jarFile.parent, "${jarFile.nameWithoutExtension}_temp.jar")
            
            java.util.zip.ZipOutputStream(tempJar.outputStream()).use { zipOut ->
                // Copy existing JAR contents
                ZipFile(jarFile).use { existingJar ->
                    existingJar.entries().asSequence()
                        .filter { !it.name.startsWith("META-INF/") }
                        .forEach { entry ->
                            zipOut.putNextEntry(java.util.zip.ZipEntry(entry.name))
                            existingJar.getInputStream(entry).copyTo(zipOut)
                            zipOut.closeEntry()
                        }
                }
                
                // Add assets
                assetsEntries.forEach { entry ->
                    zipOut.putNextEntry(java.util.zip.ZipEntry(entry.name))
                    apk.getInputStream(entry).copyTo(zipOut)
                    zipOut.closeEntry()
                }
            }
            
            jarFile.delete()
            tempJar.renameTo(jarFile)
        }
    }
    
    private fun instantiateSources(
        classLoader: URLClassLoader,
        metadata: ApkMetadata
    ): List<TachiSourceWrapper> {
        return try {
            val mainClass = classLoader.loadClass(metadata.mainClass)
            val instance = mainClass.getDeclaredConstructor().newInstance()
            
            when {
                // Check if it's a SourceFactory
                instance::class.java.interfaces.any { it.simpleName == "SourceFactory" } -> {
                    val createMethod = instance::class.java.getMethod("createSources")
                    @Suppress("UNCHECKED_CAST")
                    val sources = createMethod.invoke(instance) as List<Any>
                    sources.map { wrapSource(it) }
                }
                // Single source
                else -> listOf(wrapSource(instance))
            }
        } catch (e: Exception) {
            throw TachiExtensionException("Failed to instantiate sources", e)
        }
    }
    
    private fun wrapSource(source: Any): TachiSourceWrapper {
        return ReflectiveTachiSourceWrapper(source)
    }
    
    private fun determineLang(sources: List<TachiSourceWrapper>): String {
        val langs = sources.map { it.lang }.distinct()
        return when {
            langs.isEmpty() -> "all"
            langs.size == 1 -> langs.first()
            else -> "all"
        }
    }
    
    private fun extractIcon(apkFile: File, pkgName: String): String? {
        // Extract icon from APK and save locally
        return try {
            ZipFile(apkFile).use { zip ->
                val iconEntry = zip.entries().asSequence()
                    .filter { it.name.contains("ic_launcher") && it.name.endsWith(".png") }
                    .maxByOrNull { 
                        // Prefer larger icons
                        when {
                            it.name.contains("xxxhdpi") -> 4
                            it.name.contains("xxhdpi") -> 3
                            it.name.contains("xhdpi") -> 2
                            it.name.contains("hdpi") -> 1
                            else -> 0
                        }
                    }
                
                if (iconEntry != null) {
                    val iconFile = File(extensionsDir, "$pkgName.png")
                    zip.getInputStream(iconEntry).use { input ->
                        iconFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    iconFile.absolutePath
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * APK metadata extracted from manifest.
 */
private data class ApkMetadata(
    val pkgName: String,
    val name: String,
    val versionName: String,
    val versionCode: Int,
    val mainClass: String,
    val isNsfw: Boolean,
    val isTachiExtension: Boolean
)
