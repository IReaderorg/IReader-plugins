package io.github.ireaderorg.plugins.coralreef

import ireader.plugin.api.*
import ireader.plugin.annotations.IReaderPlugin
import ireader.plugin.annotations.PluginMetadata

/**
 * Coral Reef Theme - Tropical ocean colors with warm coral and cool cyan accents.
 */
@IReaderPlugin
@PluginMetadata(
    id = "io.github.ireaderorg.plugins.coral-reef",
    name = "Coral Reef",
    version = "1.0.0",
    versionCode = 1,
    description = "Tropical ocean colors with warm coral and cool cyan accents",
    author = "IReader Team"
)
class CoralReefTheme : ThemePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.coral-reef",
        name = "Coral Reef",
        version = "1.0.0",
        versionCode = 1,
        description = "Tropical ocean colors with warm coral and cool cyan accents",
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
            bars = if (isDark) 0xFFBF360C else 0xFFFF7043,
            onBars = 0xFFFFFFFF,
            isBarLight = false
        )
    }

    private val lightColors = ThemeColorScheme(
        primary = 0xFFFF7043, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFFFCCBC, onPrimaryContainer = 0xFFBF360C,
        secondary = 0xFF26C6DA, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFB2EBF2, onSecondaryContainer = 0xFF006064,
        tertiary = 0xFFFF8A65, onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFFFCCBC, onTertiaryContainer = 0xFFD84315,
        error = 0xFFB00020, onError = 0xFFFFFFFF,
        errorContainer = 0xFFFCD8DF, onErrorContainer = 0xFF8B0000,
        background = 0xFFFFF3E0, onBackground = 0xFFBF360C,
        surface = 0xFFFFFFFF, onSurface = 0xFFBF360C,
        surfaceVariant = 0xFFFFE0B2, onSurfaceVariant = 0xFFE64A19,
        outline = 0xFFFFAB91, outlineVariant = 0xFFFFCCBC,
        scrim = 0xFF000000, inverseSurface = 0xFFBF360C,
        inverseOnSurface = 0xFFFFF3E0, inversePrimary = 0xFFFFAB91
    )
    
    private val darkColors = ThemeColorScheme(
        primary = 0xFFFFAB91, onPrimary = 0xFFBF360C,
        primaryContainer = 0xFFE64A19, onPrimaryContainer = 0xFFFFCCBC,
        secondary = 0xFF80DEEA, onSecondary = 0xFF006064,
        secondaryContainer = 0xFF00ACC1, onSecondaryContainer = 0xFFB2EBF2,
        tertiary = 0xFFFF8A65, onTertiary = 0xFFD84315,
        tertiaryContainer = 0xFFE64A19, onTertiaryContainer = 0xFFFFCCBC,
        error = 0xFFCF6679, onError = 0xFF000000,
        errorContainer = 0xFF8B0000, onErrorContainer = 0xFFFCD8DF,
        background = 0xFF1A100A, onBackground = 0xFFFFCCBC,
        surface = 0xFF2A1A14, onSurface = 0xFFFFCCBC,
        surfaceVariant = 0xFF3A241C, onSurfaceVariant = 0xFFFFAB91,
        outline = 0xFFE64A19, outlineVariant = 0xFFD84315,
        scrim = 0xFF000000, inverseSurface = 0xFFFFCCBC,
        inverseOnSurface = 0xFFBF360C, inversePrimary = 0xFFFF7043
    )
}
