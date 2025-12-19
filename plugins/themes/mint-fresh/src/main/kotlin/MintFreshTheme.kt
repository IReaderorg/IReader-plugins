package io.github.ireaderorg.plugins.mintfresh

import ireader.plugin.api.*
import ireader.plugin.annotations.IReaderPlugin
import ireader.plugin.annotations.PluginMetadata

/**
 * Mint Fresh Theme - Cool refreshing mint green for a clean reading experience.
 */
@IReaderPlugin
@PluginMetadata(
    id = "io.github.ireaderorg.plugins.mint-fresh",
    name = "Mint Fresh",
    version = "1.0.0",
    versionCode = 1,
    description = "Cool refreshing mint green theme for a clean reading experience",
    author = "IReader Team"
)
class MintFreshTheme : ThemePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.mint-fresh",
        name = "Mint Fresh",
        version = "1.0.0",
        versionCode = 1,
        description = "Cool refreshing mint green theme for a clean reading experience",
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
            bars = if (isDark) 0xFF00695C else 0xFF26A69A,
            onBars = 0xFFFFFFFF,
            isBarLight = false
        )
    }

    private val lightColors = ThemeColorScheme(
        primary = 0xFF26A69A, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFB2DFDB, onPrimaryContainer = 0xFF00695C,
        secondary = 0xFF66BB6A, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFC8E6C9, onSecondaryContainer = 0xFF2E7D32,
        tertiary = 0xFF4DB6AC, onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFB2DFDB, onTertiaryContainer = 0xFF00695C,
        error = 0xFFB00020, onError = 0xFFFFFFFF,
        errorContainer = 0xFFFCD8DF, onErrorContainer = 0xFF8B0000,
        background = 0xFFE8F5E9, onBackground = 0xFF1B5E20,
        surface = 0xFFFFFFFF, onSurface = 0xFF1B5E20,
        surfaceVariant = 0xFFE0F2F1, onSurfaceVariant = 0xFF00695C,
        outline = 0xFF80CBC4, outlineVariant = 0xFFB2DFDB,
        scrim = 0xFF000000, inverseSurface = 0xFF1B5E20,
        inverseOnSurface = 0xFFE8F5E9, inversePrimary = 0xFF80CBC4
    )
    
    private val darkColors = ThemeColorScheme(
        primary = 0xFF80CBC4, onPrimary = 0xFF00695C,
        primaryContainer = 0xFF00897B, onPrimaryContainer = 0xFFB2DFDB,
        secondary = 0xFFA5D6A7, onSecondary = 0xFF2E7D32,
        secondaryContainer = 0xFF43A047, onSecondaryContainer = 0xFFC8E6C9,
        tertiary = 0xFF4DB6AC, onTertiary = 0xFF00695C,
        tertiaryContainer = 0xFF00897B, onTertiaryContainer = 0xFFB2DFDB,
        error = 0xFFCF6679, onError = 0xFF000000,
        errorContainer = 0xFF8B0000, onErrorContainer = 0xFFFCD8DF,
        background = 0xFF0A1A18, onBackground = 0xFFB2DFDB,
        surface = 0xFF142A26, onSurface = 0xFFB2DFDB,
        surfaceVariant = 0xFF1E3A34, onSurfaceVariant = 0xFF80CBC4,
        outline = 0xFF00897B, outlineVariant = 0xFF00695C,
        scrim = 0xFF000000, inverseSurface = 0xFFB2DFDB,
        inverseOnSurface = 0xFF00695C, inversePrimary = 0xFF26A69A
    )
}
