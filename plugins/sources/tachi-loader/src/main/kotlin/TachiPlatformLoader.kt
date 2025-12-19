package io.github.ireaderorg.plugins.tachiloader

import ireader.plugin.api.PluginContext
import ireader.plugin.api.TachiValidationResult
import ireader.plugin.api.tachi.TachiSource

/**
 * Platform-specific extension loader interface.
 */
interface TachiPlatformLoader {
    /**
     * Load an extension from APK.
     */
    suspend fun loadExtension(apkPath: String): LoadedExtension
    
    /**
     * Unload an extension.
     */
    fun unloadExtension(pkgName: String)
    
    /**
     * Validate an APK file.
     */
    fun validateApk(apkPath: String): TachiValidationResult
}

/**
 * Result of loading an extension.
 */
data class LoadedExtension(
    val pkgName: String,
    val name: String,
    val versionName: String,
    val versionCode: Int,
    val lang: String,
    val isNsfw: Boolean,
    val sources: List<TachiSourceWrapper>,
    val iconUrl: String? = null
)

/**
 * Wrapper around loaded Tachi source.
 * Adapts the actual Tachi source to our interface.
 */
interface TachiSourceWrapper : ireader.plugin.api.tachi.TachiCatalogueSource {
    /** The underlying source object (platform-specific) */
    val underlyingSource: Any
}
