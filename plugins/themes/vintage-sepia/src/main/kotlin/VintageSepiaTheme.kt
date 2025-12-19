package io.github.ireaderorg.plugins.vintagesepia

import ireader.plugin.api.*
import ireader.plugin.annotations.IReaderPlugin
import ireader.plugin.annotations.PluginMetadata

/**
 * Vintage Sepia Theme - Classic old book feel with warm brown tones.
 */
@IReaderPlugin
@PluginMetadata(
    id = "io.github.ireaderorg.plugins.vintage-sepia",
    name = "Vintage Sepia",
    version = "1.0.0",
    versionCode = 1,
    description = "Classic old book feel with warm brown tones",
    author = "IReader Team"
)
class VintageSepiaTheme : ThemePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.vintage-sepia",
        name = "Vintage Sepia",
        version = "1.0.0",
        versionCode = 1,
        description = "Classic old book feel with warm brown tones",
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
            bars = if (isDark) 0xFF3E2723 else 0xFF795548,
            onBars = 0xFFFFFFFF,
            isBarLight = false
        )
    }

    private val lightColors = ThemeColorScheme(
        primary = 0xFF795548, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFD7CCC8, onPrimaryContainer = 0xFF3E2723,
        secondary = 0xFF8D6E63, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFBCAAA4, onSecondaryContainer = 0xFF4E342E,
        tertiary = 0xFF6D4C41, onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFD7CCC8, onTertiaryContainer = 0xFF3E2723,
        error = 0xFFB00020, onError = 0xFFFFFFFF,
        errorContainer = 0xFFFCD8DF, onErrorContainer = 0xFF8B0000,
        background = 0xFFFAF3E0, onBackground = 0xFF3E2723,
        surface = 0xFFFFF8E7, onSurface = 0xFF3E2723,
        surfaceVariant = 0xFFF5EBD7, onSurfaceVariant = 0xFF5D4037,
        outline = 0xFFA1887F, outlineVariant = 0xFFBCAAA4,
        scrim = 0xFF000000, inverseSurface = 0xFF3E2723,
        inverseOnSurface = 0xFFFAF3E0, inversePrimary = 0xFFBCAAA4
    )
    
    private val darkColors = ThemeColorScheme(
        primary = 0xFFBCAAA4, onPrimary = 0xFF3E2723,
        primaryContainer = 0xFF5D4037, onPrimaryContainer = 0xFFD7CCC8,
        secondary = 0xFFA1887F, onSecondary = 0xFF4E342E,
        secondaryContainer = 0xFF6D4C41, onSecondaryContainer = 0xFFBCAAA4,
        tertiary = 0xFF8D6E63, onTertiary = 0xFF3E2723,
        tertiaryContainer = 0xFF5D4037, onTertiaryContainer = 0xFFD7CCC8,
        error = 0xFFCF6679, onError = 0xFF000000,
        errorContainer = 0xFF8B0000, onErrorContainer = 0xFFFCD8DF,
        background = 0xFF1C1410, onBackground = 0xFFD7CCC8,
        surface = 0xFF2A201A, onSurface = 0xFFD7CCC8,
        surfaceVariant = 0xFF3A2E26, onSurfaceVariant = 0xFFBCAAA4,
        outline = 0xFF6D4C41, outlineVariant = 0xFF5D4037,
        scrim = 0xFF000000, inverseSurface = 0xFFD7CCC8,
        inverseOnSurface = 0xFF3E2723, inversePrimary = 0xFF795548
    )
}
