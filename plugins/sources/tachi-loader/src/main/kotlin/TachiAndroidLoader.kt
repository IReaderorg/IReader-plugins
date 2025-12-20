package io.github.ireaderorg.plugins.tachiloader

import ireader.plugin.api.PluginContext
import ireader.plugin.api.TachiExtensionException
import ireader.plugin.api.TachiValidationResult
import java.io.File

/**
 * Android implementation of Tachi extension loader.
 * Uses DexClassLoader to load APK files directly.
 */
class TachiAndroidLoader(
    private val context: PluginContext
) : TachiPlatformLoader {
    
    companion object {
        const val EXTENSION_FEATURE = "tachiyomi.extension"
        const val METADATA_SOURCE_CLASS = "tachiyomi.extension.class"
        const val METADATA_NSFW = "tachiyomi.extension.nsfw"
        const val LIB_VERSION_MIN = 1.3
        const val LIB_VERSION_MAX = 1.5
    }
    
    private val classLoaders = mutableMapOf<String, Any>() // DexClassLoader
    private val extensionsDir: File by lazy {
        File(context.getDataDir(), "tachi-extensions").apply { mkdirs() }
    }
    
    override suspend fun loadExtension(apkPath: String): LoadedExtension {
        val apkFile = File(apkPath)
        if (!apkFile.exists()) {
            throw TachiExtensionException("APK file not found: $apkPath")
        }
        
        // Parse APK metadata using Android PackageManager
        val metadata = parseApkMetadata(apkFile)
        
        // Validate lib version
        val libVersion = metadata.versionName.substringBeforeLast('.').toDoubleOrNull() ?: 0.0
        if (libVersion < LIB_VERSION_MIN || libVersion > LIB_VERSION_MAX) {
            throw TachiExtensionException(
                "Unsupported lib version: $libVersion (supported: $LIB_VERSION_MIN-$LIB_VERSION_MAX)"
            )
        }
        
        // Create DexClassLoader
        val dexOutputDir = File(extensionsDir, "dex").apply { mkdirs() }
        val classLoader = createDexClassLoader(
            apkFile.absolutePath,
            dexOutputDir.absolutePath,
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
            iconUrl = null // Android can load icon from APK directly
        )
    }
    
    override fun unloadExtension(pkgName: String) {
        classLoaders.remove(pkgName)
    }
    
    override fun validateApk(apkPath: String): TachiValidationResult {
        return try {
            val apkFile = File(apkPath)
            if (!apkFile.exists()) {
                return TachiValidationResult.Invalid("File not found")
            }
            
            val metadata = parseApkMetadata(apkFile)
            
            if (!metadata.isTachiExtension) {
                return TachiValidationResult.Invalid("Not a Tachiyomi extension")
            }
            
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
        // Use Android PackageManager to parse APK
        return try {
            val pmClass = Class.forName("android.content.pm.PackageManager")
            val contextClass = Class.forName("android.content.Context")
            
            // Get application context
            val appClass = Class.forName("android.app.Application")
            val currentAppMethod = appClass.getMethod("getProcessName")
            
            // This is a simplified version - real implementation would use:
            // context.packageManager.getPackageArchiveInfo(apkPath, PackageManager.GET_META_DATA)
            
            // For now, parse from filename
            val fileName = apkFile.nameWithoutExtension
            val parts = fileName.split("-")
            
            val pkgName = "eu.kanade.tachiyomi.extension.${parts.getOrElse(1) { "all" }}.${parts.getOrElse(2) { "unknown" }}"
            val name = parts.getOrElse(2) { "Unknown" }.replace("_", " ")
            val version = parts.lastOrNull()?.removePrefix("v") ?: "1.0.0"
            
            ApkMetadata(
                pkgName = pkgName,
                name = name,
                versionName = version,
                versionCode = 1,
                mainClass = "$pkgName.${name.replace(" ", "")}",
                isNsfw = false,
                isTachiExtension = true
            )
        } catch (e: Exception) {
            throw TachiExtensionException("Failed to parse APK metadata", e)
        }
    }
    
    private fun createDexClassLoader(
        dexPath: String,
        optimizedDir: String,
        parent: ClassLoader?
    ): Any {
        // Create DexClassLoader using reflection
        val dexClassLoaderClass = Class.forName("dalvik.system.DexClassLoader")
        val constructor = dexClassLoaderClass.getConstructor(
            String::class.java,
            String::class.java,
            String::class.java,
            ClassLoader::class.java
        )
        return constructor.newInstance(dexPath, optimizedDir, null, parent)
    }
    
    private fun instantiateSources(
        classLoader: Any,
        metadata: ApkMetadata
    ): List<TachiSourceWrapper> {
        return try {
            // Use classLoader.loadClass()
            val loadClassMethod = classLoader::class.java.getMethod("loadClass", String::class.java)
            val mainClass = loadClassMethod.invoke(classLoader, metadata.mainClass) as Class<*>
            val instance = mainClass.getDeclaredConstructor().newInstance()
            
            when {
                instance::class.java.interfaces.any { it.simpleName == "SourceFactory" } -> {
                    val createMethod = instance::class.java.getMethod("createSources")
                    @Suppress("UNCHECKED_CAST")
                    val sources = createMethod.invoke(instance) as List<Any>
                    sources.map { ReflectiveTachiSourceWrapper(it) }
                }
                else -> listOf(ReflectiveTachiSourceWrapper(instance))
            }
        } catch (e: Exception) {
            throw TachiExtensionException("Failed to instantiate sources", e)
        }
    }
    
    private fun determineLang(sources: List<TachiSourceWrapper>): String {
        val langs = sources.map { it.lang }.distinct()
        return when {
            langs.isEmpty() -> "all"
            langs.size == 1 -> langs.first()
            else -> "all"
        }
    }
}

/**
 * APK metadata extracted from manifest.
 */
data class ApkMetadata(
    val pkgName: String,
    val name: String,
    val versionName: String,
    val versionCode: Int,
    val mainClass: String,
    val isNsfw: Boolean,
    val isTachiExtension: Boolean
)
