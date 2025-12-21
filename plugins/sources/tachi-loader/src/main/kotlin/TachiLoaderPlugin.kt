package io.github.ireaderorg.plugins.tachiloader

import ireader.plugin.api.*
import ireader.plugin.api.source.*
import ireader.plugin.api.tachi.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

// Platform loaders are in separate files:
// - TachiAndroidLoader.kt
// - TachiDesktopLoader.kt
// - TachiPlatformLoader.kt (interface)
// - ReflectiveTachiSourceWrapper.kt

// ==================== Main Plugin ====================

/**
 * Tachiyomi/Mihon source loader plugin with declarative UI.
 * 
 * Loads Tachi extension APKs and provides manga sources to IReader.
 * Includes UI for repository management and extension browsing.
 */
class TachiLoaderPlugin : TachiSourceLoaderPlugin, FeaturePlugin, PluginUIProvider {
    
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
    private val repositories = mutableListOf<SourceRepository>()
    
    // Platform-specific loader
    private var platformLoader: TachiPlatformLoader? = null
    
    // UI State
    private var currentTab = 0
    private var availableExtensions = listOf<SourceExtensionMeta>()
    private var isLoading = false
    private var error: String? = null
    private var searchQuery = ""
    private var selectedLang = "all"
    private var pendingRepoUrl = ""
    private var pendingRepoName = ""
    
    override fun initialize(context: PluginContext) {
        this.context = context
        platformLoader = createPlatformLoader(context)
        loadRepositories()
        // Add default repos if empty
        if (repositories.isEmpty()) {
            repositories.add(SourceRepository.KEIYOUSHI)
        }
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
    
    // ==================== FeaturePlugin Implementation ====================
    
    override fun getMenuItems(): List<PluginMenuItem> = listOf(
        PluginMenuItem(id = "browse_extensions", label = "Browse Extensions", icon = "extension", order = 0),
        PluginMenuItem(id = "manage_repos", label = "Manage Repositories", icon = "settings", order = 1),
        PluginMenuItem(id = "installed", label = "Installed Extensions", icon = "download_done", order = 2)
    )
    
    override fun getScreens(): List<PluginScreen> = listOf(
        PluginScreen(
            route = "plugin/tachi-loader/main",
            title = "Tachi Extensions",
            content = {}
        )
    )
    
    override fun onReaderContext(context: ReaderContext): PluginAction? = null
    
    // ==================== PluginUIProvider Implementation ====================
    
    override fun getScreen(screenId: String, context: PluginScreenContext): PluginUIScreen? {
        return buildMainScreen()
    }
    
    override suspend fun handleEvent(
        screenId: String,
        event: PluginUIEvent,
        context: PluginScreenContext
    ): PluginUIScreen? {
        when (event.eventType) {
            UIEventType.TAB_SELECTED -> {
                currentTab = event.data["index"]?.toIntOrNull() ?: 0
            }
            UIEventType.TEXT_CHANGED -> {
                when (event.componentId) {
                    "search" -> searchQuery = event.data["value"] ?: ""
                    "repo_url" -> pendingRepoUrl = event.data["value"] ?: ""
                    "repo_name" -> pendingRepoName = event.data["value"] ?: ""
                }
            }
            UIEventType.CHIP_SELECTED -> {
                if (event.componentId == "lang_filter") {
                    selectedLang = event.data["value"] ?: "all"
                }
            }
            UIEventType.CLICK -> {
                when {
                    event.componentId == "refresh" -> {
                        isLoading = true
                        error = null
                        try {
                            availableExtensions = fetchAvailableExtensions()
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to fetch extensions"
                        }
                        isLoading = false
                    }
                    event.componentId == "add_repo" -> {
                        if (pendingRepoUrl.isNotBlank()) {
                            addRepository(SourceRepository(
                                name = pendingRepoName.ifBlank { "Custom Repo" },
                                baseUrl = pendingRepoUrl.trimEnd('/'),
                                isEnabled = true
                            ))
                            pendingRepoUrl = ""
                            pendingRepoName = ""
                        }
                    }
                    event.componentId.startsWith("install_") -> {
                        val pkgName = event.componentId.removePrefix("install_")
                        val ext = availableExtensions.find { it.pkgName == pkgName }
                        if (ext != null) {
                            isLoading = true
                            try {
                                installExtension(ext) { }
                            } catch (e: Exception) {
                                error = "Install failed: ${e.message}"
                            }
                            isLoading = false
                        }
                    }
                    event.componentId.startsWith("uninstall_") -> {
                        val pkgName = event.componentId.removePrefix("uninstall_")
                        try {
                            uninstallExtension(pkgName)
                        } catch (e: Exception) {
                            error = "Uninstall failed: ${e.message}"
                        }
                    }
                    event.componentId.startsWith("remove_repo_") -> {
                        val repoUrl = event.componentId.removePrefix("remove_repo_")
                        removeRepository(repoUrl)
                    }
                    event.componentId.startsWith("toggle_repo_") -> {
                        val repoUrl = event.componentId.removePrefix("toggle_repo_")
                        val repo = repositories.find { it.baseUrl == repoUrl }
                        if (repo != null) {
                            val index = repositories.indexOf(repo)
                            repositories[index] = repo.copy(isEnabled = !repo.isEnabled)
                            saveRepositories()
                        }
                    }
                }
            }
            else -> {}
        }
        return buildMainScreen()
    }
    
    private fun buildMainScreen(): PluginUIScreen {
        val tabs = listOf(
            Tab(
                id = "browse",
                title = "Browse",
                icon = "extension",
                content = buildBrowseTab()
            ),
            Tab(
                id = "installed",
                title = "Installed",
                icon = "download_done",
                content = buildInstalledTab()
            ),
            Tab(
                id = "repos",
                title = "Repos",
                icon = "settings",
                content = buildReposTab()
            )
        )
        
        return PluginUIScreen(
            id = "main",
            title = "Tachi Extensions",
            components = listOf(PluginUIComponent.Tabs(tabs))
        )
    }
    
    private fun buildBrowseTab(): kotlin.collections.List<PluginUIComponent> {
        val components = mutableListOf<PluginUIComponent>()
        
        // Search and filter
        components.add(PluginUIComponent.Row(
            children = listOf(
                PluginUIComponent.TextField(
                    id = "search",
                    label = "Search extensions",
                    value = searchQuery
                ),
                PluginUIComponent.Button(
                    id = "refresh",
                    label = "",
                    style = ButtonStyle.TEXT,
                    icon = "refresh"
                )
            ),
            spacing = 8
        ))
        
        // Language filter
        val languages = listOf("all", "en", "ja", "ko", "zh", "multi")
        components.add(PluginUIComponent.ChipGroup(
            id = "lang_filter",
            chips = languages.map { lang ->
                PluginUIComponent.Chip(
                    id = lang,
                    label = if (lang == "all") "All" else lang.uppercase(),
                    selected = selectedLang == lang
                )
            },
            singleSelection = true
        ))
        
        components.add(PluginUIComponent.Spacer(16))
        
        if (isLoading) {
            components.add(PluginUIComponent.Loading("Loading extensions..."))
        } else if (error != null) {
            components.add(PluginUIComponent.Error(error!!))
        } else if (availableExtensions.isEmpty()) {
            components.add(PluginUIComponent.Empty(
                icon = "extension",
                message = "No extensions loaded",
                description = "Tap refresh to load extensions from repositories"
            ))
        } else {
            // Filter extensions
            val filtered = availableExtensions.filter { ext ->
                (selectedLang == "all" || ext.lang == selectedLang) &&
                (searchQuery.isBlank() || ext.name.lowercase().contains(searchQuery.lowercase()))
            }
            
            filtered.take(50).forEach { ext ->
                val isInstalled = loadedExtensions.containsKey(ext.pkgName)
                components.add(PluginUIComponent.Card(
                    children = listOf(
                        PluginUIComponent.Row(
                            children = listOf(
                                PluginUIComponent.Column(
                                    children = listOf(
                                        PluginUIComponent.Text(ext.name, TextStyle.TITLE_SMALL),
                                        PluginUIComponent.Text(
                                            "${ext.lang.uppercase()} • v${ext.versionName}${if (ext.isNsfw) " • 18+" else ""}",
                                            TextStyle.BODY_SMALL
                                        )
                                    ),
                                    spacing = 4
                                ),
                                PluginUIComponent.Button(
                                    id = if (isInstalled) "uninstall_${ext.pkgName}" else "install_${ext.pkgName}",
                                    label = if (isInstalled) "Uninstall" else "Install",
                                    style = if (isInstalled) ButtonStyle.OUTLINED else ButtonStyle.PRIMARY
                                )
                            ),
                            spacing = 8
                        )
                    )
                ))
            }
            
            if (filtered.size > 50) {
                components.add(PluginUIComponent.Text(
                    "Showing 50 of ${filtered.size} extensions. Use search to find more.",
                    TextStyle.BODY_SMALL
                ))
            }
        }
        
        return components
    }
    
    private fun buildInstalledTab(): kotlin.collections.List<PluginUIComponent> {
        val components = mutableListOf<PluginUIComponent>()
        
        if (loadedExtensions.isEmpty()) {
            components.add(PluginUIComponent.Empty(
                icon = "download_done",
                message = "No extensions installed",
                description = "Browse and install extensions from the Browse tab"
            ))
        } else {
            loadedExtensions.values.forEach { ext ->
                components.add(PluginUIComponent.Card(
                    children = listOf(
                        PluginUIComponent.Row(
                            children = listOf(
                                PluginUIComponent.Column(
                                    children = listOf(
                                        PluginUIComponent.Text(ext.name, TextStyle.TITLE_SMALL),
                                        PluginUIComponent.Text(
                                            "${ext.lang.uppercase()} • v${ext.versionName} • ${ext.sourceIds.size} sources",
                                            TextStyle.BODY_SMALL
                                        )
                                    ),
                                    spacing = 4
                                ),
                                PluginUIComponent.Button(
                                    id = "uninstall_${ext.pkgName}",
                                    label = "Uninstall",
                                    style = ButtonStyle.OUTLINED,
                                    icon = "delete"
                                )
                            ),
                            spacing = 8
                        )
                    )
                ))
            }
        }
        
        return components
    }
    
    private fun buildReposTab(): kotlin.collections.List<PluginUIComponent> {
        val components = mutableListOf<PluginUIComponent>()
        
        // Add repo form
        components.add(PluginUIComponent.Card(
            children = listOf(
                PluginUIComponent.Text("Add Repository", TextStyle.TITLE_SMALL),
                PluginUIComponent.Spacer(8),
                PluginUIComponent.TextField(
                    id = "repo_name",
                    label = "Repository name (optional)",
                    value = pendingRepoName
                ),
                PluginUIComponent.Spacer(8),
                PluginUIComponent.TextField(
                    id = "repo_url",
                    label = "Repository URL",
                    value = pendingRepoUrl
                ),
                PluginUIComponent.Spacer(8),
                PluginUIComponent.Button(
                    id = "add_repo",
                    label = "Add Repository",
                    style = ButtonStyle.PRIMARY,
                    icon = "add"
                )
            )
        ))
        
        components.add(PluginUIComponent.Spacer(16))
        components.add(PluginUIComponent.Text("Repositories", TextStyle.TITLE_SMALL))
        components.add(PluginUIComponent.Spacer(8))
        
        if (repositories.isEmpty()) {
            components.add(PluginUIComponent.Empty(
                icon = "settings",
                message = "No repositories",
                description = "Add a repository to browse extensions"
            ))
        } else {
            repositories.forEach { repo ->
                components.add(PluginUIComponent.Card(
                    children = listOf(
                        PluginUIComponent.Row(
                            children = listOf(
                                PluginUIComponent.Column(
                                    children = listOf(
                                        PluginUIComponent.Text(repo.name, TextStyle.TITLE_SMALL),
                                        PluginUIComponent.Text(repo.baseUrl, TextStyle.BODY_SMALL)
                                    ),
                                    spacing = 4
                                ),
                                PluginUIComponent.Switch(
                                    id = "toggle_repo_${repo.baseUrl}",
                                    label = "",
                                    checked = repo.isEnabled
                                ),
                                PluginUIComponent.Button(
                                    id = "remove_repo_${repo.baseUrl}",
                                    label = "",
                                    style = ButtonStyle.TEXT,
                                    icon = "delete"
                                )
                            ),
                            spacing = 8
                        )
                    )
                ))
            }
        }
        
        return components
    }
    
    // ==================== SourceLoaderPlugin Implementation ====================
    
    override fun getSources(): kotlin.collections.List<UnifiedSource> = unifiedSources.values.toList()
    
    override fun getSource(sourceId: Long): UnifiedSource? = unifiedSources[sourceId]
    
    override fun getSourcesByLanguage(lang: String): kotlin.collections.List<UnifiedSource> =
        unifiedSources.values.filter { it.lang == lang }
    
    override fun getAvailableLanguages(): kotlin.collections.List<String> =
        unifiedSources.values.map { it.lang }.distinct().sorted()
    
    override fun searchSources(query: String): kotlin.collections.List<UnifiedSource> {
        val lowerQuery = query.lowercase()
        return unifiedSources.values.filter { it.name.lowercase().contains(lowerQuery) }
    }
    
    override suspend fun refreshSources() {
        val extensions = loadedExtensions.values.toList()
        extensions.forEach { ext ->
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
    
    override fun getRepositories(): kotlin.collections.List<SourceRepository> = repositories.toList()
    
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
    
    override suspend fun fetchAvailableExtensions(): kotlin.collections.List<SourceExtensionMeta> {
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
        
        val info = loadTachiExtension(apkPath)
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
    
    override fun getInstalledExtensions(): kotlin.collections.List<SourceExtensionInfo> =
        loadedExtensions.values.map { it.toSourceExtensionInfo() }
    
    override suspend fun checkForUpdates(): kotlin.collections.List<SourceExtensionUpdate> {
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
    
    override suspend fun loadTachiExtension(apkPath: String): TachiExtensionInfo {
        val loader = platformLoader ?: throw TachiExtensionException("Plugin not initialized")
        
        val validation = validateTachiExtension(apkPath)
        if (validation !is TachiValidationResult.Valid) {
            throw TachiExtensionException("Invalid extension: $validation")
        }
        
        val result = loader.loadExtension(apkPath)
        
        result.sources.forEach { source ->
            loadedTachiSources[source.id] = source
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
    
    override fun unloadTachiExtension(pkgName: String) {
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
    
    override fun getTachiCatalogueSources(): kotlin.collections.List<TachiCatalogueSource> =
        loadedTachiSources.values.filterIsInstance<TachiCatalogueSource>()
    
    override suspend fun validateTachiExtension(apkPath: String): TachiValidationResult {
        val loader = platformLoader 
            ?: return TachiValidationResult.Invalid("Plugin not initialized")
        
        return try {
            loader.validateApk(apkPath)
        } catch (e: Exception) {
            TachiValidationResult.Invalid(e.message ?: "Validation failed")
        }
    }
    
    override fun getTachiExtensions(): kotlin.collections.List<TachiExtensionInfo> = loadedExtensions.values.toList()
    
    // ==================== ExtensionLoader Implementation ====================
    
    override val supportedFormats: List<ExtensionFormat>
        get() = listOf(ExtensionFormat.APK)
    
    override suspend fun loadExtension(path: String): ExtensionLoadResult {
        return try {
            val info = loadTachiExtension(path)
            val sources = info.sourceIds.mapNotNull { unifiedSources[it] }
            ExtensionLoadResult.Success(LoadedExtension(
                id = info.pkgName,
                name = info.name,
                versionName = info.versionName,
                versionCode = info.versionCode,
                lang = info.lang,
                isNsfw = info.isNsfw,
                sources = sources,
                iconUrl = info.iconUrl,
                format = ExtensionFormat.APK,
                loaderType = SourceLoaderType.TACHIYOMI
            ))
        } catch (e: Exception) {
            ExtensionLoadResult.Failure(ExtensionLoadError.Unknown(e.message ?: "Unknown error"))
        }
    }
    
    override suspend fun loadExtensionFromBytes(bytes: ByteArray, format: ExtensionFormat): ExtensionLoadResult {
        // Save bytes to temp file and load
        val ctx = context ?: return ExtensionLoadResult.Failure(ExtensionLoadError.Unknown("Plugin not initialized"))
        val tempPath = "${getExtensionsDir(ctx)}/temp_${System.currentTimeMillis()}.apk"
        writeFile(tempPath, bytes)
        return loadExtension(tempPath)
    }
    
    override fun unloadExtension(extensionId: String) {
        try {
            unloadTachiExtension(extensionId)
        } catch (_: Exception) {}
    }
    
    override suspend fun validateExtension(path: String): ExtensionValidationResult {
        return when (val result = validateTachiExtension(path)) {
            is TachiValidationResult.Valid -> ExtensionValidationResult.Valid(
                id = result.pkgName,
                name = result.name,
                version = result.libVersion.toString(),
                format = ExtensionFormat.APK
            )
            is TachiValidationResult.Invalid -> ExtensionValidationResult.Invalid(result.reason)
            is TachiValidationResult.UnsupportedVersion -> ExtensionValidationResult.UnsupportedVersion(
                version = result.version.toString(),
                minSupported = "1.2",
                maxSupported = "1.5"
            )
        }
    }
    
    override suspend fun validateExtensionFromBytes(bytes: ByteArray, format: ExtensionFormat): ExtensionValidationResult {
        val ctx = context ?: return ExtensionValidationResult.Invalid("Plugin not initialized")
        val tempPath = "${getExtensionsDir(ctx)}/temp_validate_${System.currentTimeMillis()}.apk"
        writeFile(tempPath, bytes)
        val result = validateExtension(tempPath)
        java.io.File(tempPath).delete()
        return result
    }
    
    override suspend fun getExtensionMetadata(path: String): ExtensionMetadataResult {
        return when (val result = validateTachiExtension(path)) {
            is TachiValidationResult.Valid -> ExtensionMetadataResult.Success(
                ireader.plugin.api.source.ExtensionMetadata(
                    id = result.pkgName,
                    name = result.name,
                    versionName = "1.0",
                    versionCode = 1,
                    lang = "all",
                    isNsfw = false,
                    format = ExtensionFormat.APK,
                    libVersion = result.libVersion.toString()
                )
            )
            is TachiValidationResult.Invalid -> ExtensionMetadataResult.Failure(
                ExtensionLoadError.InvalidFormat(result.reason)
            )
            is TachiValidationResult.UnsupportedVersion -> ExtensionMetadataResult.Failure(
                ExtensionLoadError.UnsupportedVersion(result.version.toString(), "1.2", "1.5")
            )
        }
    }
    
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
        return prefs.getString("extensions_dir", "") 
            .takeIf { it.isNotEmpty() } 
            ?: "${context.getDataDir()}/tachi-extensions"
    }
    
    private suspend fun fetchRepoExtensions(
        repo: SourceRepository,
        context: PluginContext
    ): kotlin.collections.List<SourceExtensionMeta> {
        val indexUrl = "${repo.baseUrl}/index.min.json"
        val httpClient = context.httpClient 
            ?: throw TachiExtensionException("HTTP client not available")
        val response = httpClient.get(indexUrl)
        
        if (response.statusCode !in 200..299) {
            throw TachiExtensionException("Failed to fetch repo index: ${response.statusCode}")
        }
        
        val body = response.body
        if (body.isEmpty()) throw TachiExtensionException("Empty response")
        return parseRepoIndex(body, repo)
    }
    
    private fun parseRepoIndex(jsonBody: String, repo: SourceRepository): kotlin.collections.List<SourceExtensionMeta> {
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
        val httpClient = context.httpClient 
            ?: throw TachiExtensionException("HTTP client not available")
        val bytes = httpClient.download(url)
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
            ListSerializer(SourceRepository.serializer()),
            repositories
        )
        prefs.putString("repositories", repoJson)
    }
    
    private fun loadRepositories() {
        val ctx = context ?: return
        val prefs = ctx.preferences
        val repoJson = prefs.getString("repositories", "")
        if (repoJson.isEmpty()) return
        
        try {
            val repos = json.decodeFromString(
                ListSerializer(SourceRepository.serializer()),
                repoJson
            )
            repositories.clear()
            repositories.addAll(repos)
        } catch (_: Exception) {}
    }
}
