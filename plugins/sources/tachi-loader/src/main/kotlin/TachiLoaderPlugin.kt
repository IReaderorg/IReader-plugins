package io.github.ireaderorg.plugins.tachiloader

import ireader.plugin.api.*
import ireader.plugin.api.source.*
import ireader.plugin.api.tachi.*
import kotlinx.serialization.json.Json

/**
 * Tachiyomi/Mihon source loader plugin.
 * 
 * Loads Tachi extension APKs and provides manga sources to IReader.
 * Implements [TachiSourceLoaderPlugin] which extends [SourceLoaderPlugin]
 * for seamless integration with IReader's unified source system.
 */
class TachiLoaderPlugin : TachiSourceLoaderPlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.tachi-loader",
        name = "Tachiyomi Source Loader",
        version = "1.0.0",
        versionCode = 1,
        description = "Load Tachiyomi/Mihon manga extensions in IReader",
        author = PluginAuthor(name = "IReader Team"),
        type = PluginType.TACHI_SOURCE_LOADER,
        permissions = listOf(
            PluginPermission.NETWORK,
            PluginPermission.STORAGE,
            PluginPermission.PREFERENCES
        ),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP),
        mainClass = "io.github.ireaderorg.plugins.tachiloader.TachiLoaderPlugin"
    )
    
    private var context: PluginContext? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    
    // Loaded extensions and sources
    private val loadedExtensions = mutableMapOf<String, TachiExtensionInfo>()
    private val loadedTachiSources = mutableMapOf<Long, TachiSourceWrapper>()
    private val unifiedSources = mutableMapOf<Long, UnifiedSource>()
    private val repositories = mutableListOf<SourceRepository>(SourceRepository.KEIYOUSHI)
    
    // Platform-specific loader
    private var platformLoader: TachiPlatformLoader? = null
    
    override fun initialize(context: PluginContext) {
        this.context = context
        platformLoader = createPlatformLoader(context)
        loadRepositories()
    }
    
    override fun cleanup() {
        loadedExtensions.keys.toList().forEach { pkgName ->
            try { platformLoader?.unloadExtension(pkgName) } catch (_: Exception) {}
        }
        loadedExtensions.clear()
        loadedTachiSources.clear()
        unifiedSources.clear()
        context = null
        platformLoader = null
    }
    
    // ==================== SourceLoaderPlugin Implementation ====================
    
    override fun getSources(): List<UnifiedSource> = unifiedSources.values.toList()
    
    override fun getSource(sourceId: Long): UnifiedSource? = unifiedSources[sourceId]
    
    override fun getSourcesByLanguage(lang: String): List<UnifiedSource> =
        unifiedSources.values.filter { it.lang == lang }
    
    override fun getAvailableLanguages(): List<String> =
        unifiedSources.values.map { it.lang }.distinct().sorted()
    
    override fun searchSources(query: String): List<UnifiedSource> {
        val lowerQuery = query.lowercase()
        return unifiedSources.values.filter { it.name.lowercase().contains(lowerQuery) }
    }
    
    override suspend fun refreshSources() {
        // Reload all extensions
        val extensions = loadedExtensions.values.toList()
        extensions.forEach { ext ->
            // Re-wrap sources as unified sources
            ext.sourceIds.forEach { sourceId ->
                loadedTachiSources[sourceId]?.let { tachiSource ->
                    if (tachiSource is TachiCatalogueSource) {
                        unifiedSources[sourceId] = TachiUnifiedSourceAdapter(
                            tachiSource = tachiSource,
                            extensionIconUrl = ext.iconUrl,
                            extensionIsNsfw = ext.isNsfw
                        )
                    }
                }
            }
        }
    }
    
    override fun supportsRepositories(): Boolean = true
    
    override fun getRepositories(): List<SourceRepository> = repositories.toList()
    
    override fun addRepository(repo: SourceRepository) {
        if (repositories.none { it.baseUrl == repo.baseUrl }) {
            repositories.add(repo)
            saveRepositories()
        }
    }
    
    override fun removeRepository(repoUrl: String) {
        repositories.removeAll { it.baseUrl == repoUrl }
        saveRepositories()
    }
    
    override suspend fun fetchAvailableExtensions(): List<SourceExtensionMeta> {
        val ctx = context ?: return emptyList()
        val allExtensions = mutableListOf<SourceExtensionMeta>()
        
        for (repo in repositories.filter { it.isEnabled }) {
            try {
                val extensions = fetchRepoExtensions(repo, ctx)
                allExtensions.addAll(extensions)
            } catch (_: Exception) {}
        }
        
        return allExtensions
    }
    
    override suspend fun installExtension(
        extension: SourceExtensionMeta,
        onProgress: (Float) -> Unit
    ): SourceExtensionInfo? {
        val ctx = context ?: return null
        
        val apkUrl = "${extension.repoUrl}/apk/${extension.fileName}"
        val downloadDir = getExtensionsDir(ctx)
        val apkPath = "$downloadDir/${extension.fileName}"
        
        downloadFile(ctx, apkUrl, apkPath, onProgress)
        
        val info = loadExtension(apkPath)
        return info.toSourceExtensionInfo()
    }
    
    override suspend fun uninstallExtension(extensionId: String): Boolean {
        return try {
            unloadExtension(extensionId)
            true
        } catch (_: Exception) {
            false
        }
    }
    
    override fun getInstalledExtensions(): List<SourceExtensionInfo> =
        loadedExtensions.values.map { it.toSourceExtensionInfo() }
    
    override suspend fun checkForUpdates(): List<SourceExtensionUpdate> {
        val available = fetchAvailableExtensions()
        val updates = mutableListOf<SourceExtensionUpdate>()
        
        for (installed in loadedExtensions.values) {
            val remote = available.find { it.pkgName == installed.pkgName }
            if (remote != null && remote.versionCode > installed.versionCode) {
                updates.add(SourceExtensionUpdate(
                    extensionId = installed.pkgName,
                    currentVersion = installed.versionName,
                    newVersion = remote.versionName,
                    newVersionCode = remote.versionCode
                ))
            }
        }
        
        return updates
    }
    
    // ==================== TachiSourceLoaderPlugin Implementation ====================
    
    override suspend fun loadExtension(apkPath: String): TachiExtensionInfo {
        val loader = platformLoader ?: throw TachiExtensionException("Plugin not initialized")
        
        val validation = validateExtension(apkPath)
        if (validation !is TachiValidationResult.Valid) {
            throw TachiExtensionException("Invalid extension: $validation")
        }
        
        val result = loader.loadExtension(apkPath)
        
        // Register Tachi sources
        result.sources.forEach { source ->
            loadedTachiSources[source.id] = source
            
            // Create unified source adapter
            if (source is TachiCatalogueSource) {
                unifiedSources[source.id] = TachiUnifiedSourceAdapter(
                    tachiSource = source,
                    extensionIconUrl = result.iconUrl,
                    extensionIsNsfw = result.isNsfw
                )
            }
        }
        
        val info = TachiExtensionInfo(
            pkgName = result.pkgName,
            name = result.name,
            versionName = result.versionName,
            versionCode = result.versionCode,
            lang = result.lang,
            isNsfw = result.isNsfw,
            sourceIds = result.sources.map { it.id },
            iconUrl = result.iconUrl
        )
        
        loadedExtensions[result.pkgName] = info
        return info
    }
    
    override suspend fun unloadExtension(pkgName: String) {
        val info = loadedExtensions[pkgName] 
            ?: throw TachiExtensionException("Extension not loaded: $pkgName")
        
        info.sourceIds.forEach { sourceId ->
            loadedTachiSources.remove(sourceId)
            unifiedSources.remove(sourceId)
        }
        
        platformLoader?.unloadExtension(pkgName)
        loadedExtensions.remove(pkgName)
    }
    
    override fun getTachiSource(sourceId: Long): TachiSource? = loadedTachiSources[sourceId]
    
    override fun getTachiCatalogueSources(): List<TachiCatalogueSource> =
        loadedTachiSources.values.filterIsInstance<TachiCatalogueSource>()
    
    override suspend fun validateExtension(apkPath: String): TachiValidationResult {
        val loader = platformLoader 
            ?: return TachiValidationResult.Invalid("Plugin not initialized")
        
        return try {
            loader.validateApk(apkPath)
        } catch (e: Exception) {
            TachiValidationResult.Invalid(e.message ?: "Validation failed")
        }
    }
    
    override fun getTachiExtensions(): List<TachiExtensionInfo> = loadedExtensions.values.toList()
    
    // ==================== Private Helpers ====================
    
    private fun createPlatformLoader(context: PluginContext): TachiPlatformLoader {
        return if (isAndroid()) TachiAndroidLoader(context) else TachiDesktopLoader(context)
    }
    
    private fun isAndroid(): Boolean = try {
        Class.forName("android.os.Build")
        true
    } catch (_: ClassNotFoundException) {
        false
    }
    
    private fun getExtensionsDir(context: PluginContext): String {
        val prefs = context.preferences
        return prefs.getString("extensions_dir", null) ?: "${context.dataDir}/tachi-extensions"
    }
    
    private suspend fun fetchRepoExtensions(
        repo: SourceRepository,
        context: PluginContext
    ): List<SourceExtensionMeta> {
        val indexUrl = "${repo.baseUrl}/index.min.json"
        val response = context.httpClient.get(indexUrl)
        
        if (!response.isSuccessful) {
            throw TachiExtensionException("Failed to fetch repo index: ${response.code}")
        }
        
        val body = response.body ?: throw TachiExtensionException("Empty response")
        return parseRepoIndex(body, repo)
    }
    
    private fun parseRepoIndex(jsonBody: String, repo: SourceRepository): List<SourceExtensionMeta> {
        val extensions = mutableListOf<SourceExtensionMeta>()
        
        try {
            val jsonArray = json.parseToJsonElement(jsonBody)
            if (jsonArray is kotlinx.serialization.json.JsonArray) {
                for (element in jsonArray) {
                    val obj = element as? kotlinx.serialization.json.JsonObject ?: continue
                    
                    val pkg = obj["pkg"]?.toString()?.trim('"') ?: continue
                    val name = obj["name"]?.toString()?.trim('"') ?: continue
                    val apk = obj["apk"]?.toString()?.trim('"') ?: continue
                    val lang = obj["lang"]?.toString()?.trim('"') ?: "all"
                    val code = obj["code"]?.toString()?.toIntOrNull() ?: 0
                    val version = obj["version"]?.toString()?.trim('"') ?: "1.0"
                    val nsfw = obj["nsfw"]?.toString() == "1"
                    val sources = obj["sources"]?.toString()?.toIntOrNull() ?: 1
                    
                    extensions.add(SourceExtensionMeta(
                        id = pkg,
                        pkgName = pkg,
                        name = name.removePrefix("Tachiyomi: "),
                        versionName = version,
                        versionCode = code,
                        lang = lang,
                        isNsfw = nsfw,
                        fileName = apk,
                        iconUrl = "${repo.baseUrl}/icon/${pkg}.png",
                        repoUrl = repo.baseUrl,
                        sourceCount = sources,
                        loaderType = SourceLoaderType.TACHIYOMI
                    ))
                }
            }
        } catch (e: Exception) {
            throw TachiExtensionException("Failed to parse repo index", e)
        }
        
        return extensions
    }
    
    private suspend fun downloadFile(
        context: PluginContext,
        url: String,
        destPath: String,
        onProgress: (Float) -> Unit
    ) {
        val response = context.httpClient.get(url)
        if (!response.isSuccessful) {
            throw TachiExtensionException("Download failed: ${response.code}")
        }
        
        val bytes = response.bodyBytes
        writeFile(destPath, bytes)
        onProgress(1f)
    }
    
    private fun writeFile(path: String, bytes: ByteArray) {
        java.io.File(path).apply {
            parentFile?.mkdirs()
            writeBytes(bytes)
        }
    }
    
    private fun saveRepositories() {
        val ctx = context ?: return
        val prefs = ctx.preferences
        val repoJson = json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(SourceRepository.serializer()),
            repositories
        )
        prefs.putString("repositories", repoJson)
    }
    
    private fun loadRepositories() {
        val ctx = context ?: return
        val prefs = ctx.preferences
        val repoJson = prefs.getString("repositories", null) ?: return
        
        try {
            val repos = json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(SourceRepository.serializer()),
                repoJson
            )
            repositories.clear()
            repositories.addAll(repos)
        } catch (_: Exception) {}
    }
}
