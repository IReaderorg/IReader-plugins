package io.github.ireaderorg.plugins.sakuranight

import ireader.plugin.api.*
import ireader.plugin.annotations.IReaderPlugin
import ireader.plugin.annotations.PluginMetadata

/**
 * Sakura Night Theme - Japanese cherry blossom inspired with soft pinks and purples.
 */
@IReaderPlugin
@PluginMetadata(
    id = "io.github.ireaderorg.plugins.sakura-night",
    name = "Sakura Night",
    version = "1.0.0",
    versionCode = 1,
    description = "Japanese cherry blossom inspired theme with soft pinks and purples",
    author = "IReader Team"
)
class SakuraNightTheme : ThemePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.sakura-night",
        name = "Sakura Night",
        version = "1.0.0",
        versionCode = 1,
        description = "Japanese cherry blossom inspired theme with soft pinks and purples",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.THEME,
        permissions = emptyList(),
        minIReaderVersion = "1.0.0",
        platforms = listOf(Platform.ANDROID, Platform.IOS, Platform.DESKTOP)
    )
    
    override fun initialize(context: PluginContext) {}
    override fun cleanup() {}
    
    override fun getColorScheme(isDark: Boolean): ThemeColorScheme {
        return if (isDark) darkColors else lightColors
    }
    
    override fun getExtraColors(isDark: Boolean): ThemeExtraColors {
        return ThemeExtraColors(
            bars = if (isDark) 0xFF880E4F else 0xFFD81B60,
            onBars = 0xFFFFFFFF,
            isBarLight = false
        )
    }

    private val lightColors = ThemeColorScheme(
        primary = 0xFFD81B60, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFFCE4EC, onPrimaryContainer = 0xFF880E4F,
        secondary = 0xFF8E24AA, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFF3E5F5, onSecondaryContainer = 0xFF4A148C,
        tertiary = 0xFFAD1457, onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFF8BBD9, onTertiaryContainer = 0xFF880E4F,
        error = 0xFFB00020, onError = 0xFFFFFFFF,
        errorContainer = 0xFFFCD8DF, onErrorContainer = 0xFF8B0000,
        background = 0xFFFFF0F5, onBackground = 0xFF4A0E2A,
        surface = 0xFFFFFFFF, onSurface = 0xFF4A0E2A,
        surfaceVariant = 0xFFFCE4EC, onSurfaceVariant = 0xFF880E4F,
        outline = 0xFFF48FB1, outlineVariant = 0xFFF8BBD9,
        scrim = 0xFF000000, inverseSurface = 0xFF4A0E2A,
        inverseOnSurface = 0xFFFCE4EC, inversePrimary = 0xFFF8BBD9
    )
    
    private val darkColors = ThemeColorScheme(
        primary = 0xFFF8BBD9, onPrimary = 0xFF880E4F,
        primaryContainer = 0xFFAD1457, onPrimaryContainer = 0xFFFCE4EC,
        secondary = 0xFFCE93D8, onSecondary = 0xFF4A148C,
        secondaryContainer = 0xFF7B1FA2, onSecondaryContainer = 0xFFF3E5F5,
        tertiary = 0xFFF48FB1, onTertiary = 0xFF880E4F,
        tertiaryContainer = 0xFFC2185B, onTertiaryContainer = 0xFFF8BBD9,
        error = 0xFFCF6679, onError = 0xFF000000,
        errorContainer = 0xFF8B0000, onErrorContainer = 0xFFFCD8DF,
        background = 0xFF1A0A12, onBackground = 0xFFFCE4EC,
        surface = 0xFF2D1520, onSurface = 0xFFFCE4EC,
        surfaceVariant = 0xFF3D1F2D, onSurfaceVariant = 0xFFF48FB1,
        outline = 0xFFAD1457, outlineVariant = 0xFF880E4F,
        scrim = 0xFF000000, inverseSurface = 0xFFFCE4EC,
        inverseOnSurface = 0xFF880E4F, inversePrimary = 0xFFD81B60
    )
}
