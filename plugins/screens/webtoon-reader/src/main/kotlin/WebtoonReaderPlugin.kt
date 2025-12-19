package io.github.ireaderorg.plugins.webtoonreader

import ireader.plugin.api.*
import ireader.plugin.api.screen.*
import ireader.plugin.api.source.ContentDeliveryType
import ireader.plugin.api.source.SourceChapterContent

/**
 * Webtoon Reader Plugin
 * 
 * Provides a vertical scrolling reader optimized for webtoons and long-strip manga.
 * Features:
 * - Smooth vertical scrolling
 * - Image preloading for seamless reading
 * - Pinch-to-zoom support
 * - Tap zones for navigation
 * - Reading progress tracking
 */
class WebtoonReaderPlugin : ReaderScreenPlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.webtoon-reader",
        name = "Webtoon Reader",
        version = "1.0.0",
        versionCode = 1,
        description = "Vertical scrolling reader for webtoons and long-strip manga",
        author = PluginAuthor(name = "IReader Team"),
        type = PluginType.READER_SCREEN,
        permissions = listOf(PluginPermission.STORAGE, PluginPermission.PREFERENCES),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.DESKTOP),
        mainClass = "io.github.ireaderorg.plugins.webtoonreader.WebtoonReaderPlugin"
    )
    
    override val screenInfo = ReaderScreenInfo(
        name = "Webtoon Reader",
        description = "Vertical scrolling reader optimized for webtoons and long-strip manga",
        targetContentTypes = setOf(ContentDeliveryType.IMAGE),
        isDefault = false,
        priority = 10,
        features = setOf(
            ReaderFeature.VERTICAL_SCROLL,
            ReaderFeature.ZOOM,
            ReaderFeature.PAN,
            ReaderFeature.PAGE_NAVIGATION,
            ReaderFeature.FULLSCREEN,
            ReaderFeature.BRIGHTNESS_CONTROL,
            ReaderFeature.KEEP_SCREEN_ON,
            ReaderFeature.GESTURE_CONTROLS
        )
    )
    
    private var context: PluginContext? = null
    private var settings = WebtoonSettings()
    
    override fun initialize(context: PluginContext) {
        this.context = context
        loadSettings()
    }
    
    override fun cleanup() {
        saveSettings()
        context = null
    }
    
    override fun createScreen(context: ReaderContext): ReaderScreen {
        return WebtoonReaderScreen(context, settings)
    }
    
    override fun canHandle(content: SourceChapterContent): Boolean {
        // Handle all image content, but prefer webtoon-style (many tall images)
        return content.type == ContentDeliveryType.IMAGE
    }
    
    override fun getSettings(): List<ReaderSetting> = listOf(
        ReaderSetting.Toggle(
            key = "preload_images",
            title = "Preload Images",
            description = "Load upcoming images in advance for smoother scrolling",
            category = "Performance",
            defaultValue = true,
            currentValue = settings.preloadImages
        ),
        ReaderSetting.Slider(
            key = "preload_count",
            title = "Preload Count",
            description = "Number of images to preload ahead",
            category = "Performance",
            min = 1f,
            max = 10f,
            step = 1f,
            defaultValue = 3f,
            currentValue = settings.preloadCount.toFloat()
        ),
        ReaderSetting.Toggle(
            key = "show_page_number",
            title = "Show Page Number",
            description = "Display current page number while reading",
            category = "Display",
            defaultValue = true,
            currentValue = settings.showPageNumber
        ),
        ReaderSetting.Toggle(
            key = "tap_to_scroll",
            title = "Tap to Scroll",
            description = "Tap screen edges to scroll up/down",
            category = "Controls",
            defaultValue = true,
            currentValue = settings.tapToScroll
        ),
        ReaderSetting.Slider(
            key = "scroll_sensitivity",
            title = "Scroll Sensitivity",
            description = "Adjust scroll speed",
            category = "Controls",
            min = 0.5f,
            max = 2f,
            step = 0.1f,
            defaultValue = 1f,
            currentValue = settings.scrollSensitivity
        ),
        ReaderSetting.Toggle(
            key = "keep_screen_on",
            title = "Keep Screen On",
            description = "Prevent screen from turning off while reading",
            category = "Display",
            defaultValue = true,
            currentValue = settings.keepScreenOn
        ),
        ReaderSetting.Selection(
            key = "image_quality",
            title = "Image Quality",
            description = "Balance between quality and loading speed",
            category = "Performance",
            options = listOf("Low", "Medium", "High", "Original"),
            optionValues = listOf("low", "medium", "high", "original"),
            defaultValue = "high",
            currentValue = settings.imageQuality
        )
    )
    
    override fun updateSetting(key: String, value: Any) {
        when (key) {
            "preload_images" -> settings = settings.copy(preloadImages = value as Boolean)
            "preload_count" -> settings = settings.copy(preloadCount = (value as Float).toInt())
            "show_page_number" -> settings = settings.copy(showPageNumber = value as Boolean)
            "tap_to_scroll" -> settings = settings.copy(tapToScroll = value as Boolean)
            "scroll_sensitivity" -> settings = settings.copy(scrollSensitivity = value as Float)
            "keep_screen_on" -> settings = settings.copy(keepScreenOn = value as Boolean)
            "image_quality" -> settings = settings.copy(imageQuality = value as String)
        }
        saveSettings()
    }
    
    private fun loadSettings() {
        val prefs = context?.preferences ?: return
        settings = WebtoonSettings(
            preloadImages = prefs.getBoolean("preload_images", true),
            preloadCount = prefs.getInt("preload_count", 3),
            showPageNumber = prefs.getBoolean("show_page_number", true),
            tapToScroll = prefs.getBoolean("tap_to_scroll", true),
            scrollSensitivity = prefs.getFloat("scroll_sensitivity", 1f),
            keepScreenOn = prefs.getBoolean("keep_screen_on", true),
            imageQuality = prefs.getString("image_quality", "high") ?: "high"
        )
    }
    
    private fun saveSettings() {
        val prefs = context?.preferences ?: return
        prefs.putBoolean("preload_images", settings.preloadImages)
        prefs.putInt("preload_count", settings.preloadCount)
        prefs.putBoolean("show_page_number", settings.showPageNumber)
        prefs.putBoolean("tap_to_scroll", settings.tapToScroll)
        prefs.putFloat("scroll_sensitivity", settings.scrollSensitivity)
        prefs.putBoolean("keep_screen_on", settings.keepScreenOn)
        prefs.putString("image_quality", settings.imageQuality)
    }
}

/**
 * Settings for the webtoon reader.
 */
data class WebtoonSettings(
    val preloadImages: Boolean = true,
    val preloadCount: Int = 3,
    val showPageNumber: Boolean = true,
    val tapToScroll: Boolean = true,
    val scrollSensitivity: Float = 1f,
    val keepScreenOn: Boolean = true,
    val imageQuality: String = "high"
)

/**
 * The actual reader screen implementation.
 * This would contain Compose UI code in a real implementation.
 */
class WebtoonReaderScreen(
    private val readerContext: ReaderContext,
    private val settings: WebtoonSettings
) : ReaderScreen {
    
    private var currentPageIndex = 0
    private var totalPages = 0
    
    override fun onCreate() {
        totalPages = readerContext.content.pages.size
        
        // Restore progress if available
        readerContext.getProgress()?.let { progress ->
            restoreProgress(progress)
        }
        
        // Apply settings
        readerContext.setKeepScreenOn(settings.keepScreenOn)
    }
    
    override fun onDestroy() {
        // Save progress before closing
        readerContext.updateProgress(getCurrentProgress())
    }
    
    override fun onContentChanged(content: SourceChapterContent) {
        totalPages = content.pages.size
        currentPageIndex = 0
    }
    
    override fun getCurrentProgress(): ReaderProgress {
        return ReaderProgress(
            position = currentPageIndex.toLong(),
            total = totalPages.toLong(),
            extras = mapOf(
                "scroll_offset" to "0" // Would store actual scroll offset
            )
        )
    }
    
    override fun restoreProgress(progress: ReaderProgress) {
        currentPageIndex = progress.position.toInt().coerceIn(0, totalPages - 1)
        // Would also restore scroll offset from extras
    }
    
    override fun getComposeContent(): Any? {
        // In a real implementation, this would return a Compose lambda:
        // return @Composable { WebtoonReaderContent(readerContext, settings) }
        return null
    }
}
