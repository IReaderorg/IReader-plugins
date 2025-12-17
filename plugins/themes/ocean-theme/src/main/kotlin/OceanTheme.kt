package io.github.ireaderorg.plugins.oceantheme

import ireader.plugin.api.*
import ireader.plugin.annotations.IReaderPlugin
import ireader.plugin.annotations.PluginMetadata

/**
 * Ocean Theme - A calming blue theme inspired by the ocean.
 */
@IReaderPlugin
@PluginMetadata(
    id = "io.github.ireaderorg.plugins.ocean-theme",
    name = "Ocean Theme",
    version = "1.0.0",
    versionCode = 1,
    description = "A calming ocean-inspired theme with blue tones",
    author = "IReader Team"
)
class OceanTheme : ThemePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.ocean-theme",
        name = "Ocean Theme",
        version = "1.0.0",
        versionCode = 1,
        description = "A calming ocean-inspired theme with blue tones",
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
            bars = if (isDark) 0xFF0D47A1 else 0xFF1976D2,
            onBars = 0xFFFFFFFF,
            isBarLight = false
        )
    }

    private val lightColors = ThemeColorScheme(
        primary = 0xFF1976D2, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFBBDEFB, onPrimaryContainer = 0xFF0D47A1,
        secondary = 0xFF00ACC1, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFB2EBF2, onSecondaryContainer = 0xFF006064,
        tertiary = 0xFF26A69A, onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFB2DFDB, onTertiaryContainer = 0xFF004D40,
        error = 0xFFB00020, onError = 0xFFFFFFFF,
        errorContainer = 0xFFFCD8DF, onErrorContainer = 0xFF8B0000,
        background = 0xFFE3F2FD, onBackground = 0xFF0D47A1,
        surface = 0xFFFFFFFF, onSurface = 0xFF1A237E,
        surfaceVariant = 0xFFE8EAF6, onSurfaceVariant = 0xFF3F51B5,
        outline = 0xFF90CAF9, outlineVariant = 0xFFBBDEFB,
        scrim = 0xFF000000, inverseSurface = 0xFF1A237E,
        inverseOnSurface = 0xFFE8EAF6, inversePrimary = 0xFF82B1FF
    )
    
    private val darkColors = ThemeColorScheme(
        primary = 0xFF82B1FF, onPrimary = 0xFF0D47A1,
        primaryContainer = 0xFF1565C0, onPrimaryContainer = 0xFFBBDEFB,
        secondary = 0xFF80DEEA, onSecondary = 0xFF006064,
        secondaryContainer = 0xFF00838F, onSecondaryContainer = 0xFFB2EBF2,
        tertiary = 0xFF80CBC4, onTertiary = 0xFF004D40,
        tertiaryContainer = 0xFF00695C, onTertiaryContainer = 0xFFB2DFDB,
        error = 0xFFCF6679, onError = 0xFF000000,
        errorContainer = 0xFF8B0000, onErrorContainer = 0xFFFCD8DF,
        background = 0xFF0D1B2A, onBackground = 0xFFE3F2FD,
        surface = 0xFF1B2838, onSurface = 0xFFE8EAF6,
        surfaceVariant = 0xFF263238, onSurfaceVariant = 0xFF90CAF9,
        outline = 0xFF546E7A, outlineVariant = 0xFF37474F,
        scrim = 0xFF000000, inverseSurface = 0xFFE8EAF6,
        inverseOnSurface = 0xFF1A237E, inversePrimary = 0xFF1976D2
    )
}
