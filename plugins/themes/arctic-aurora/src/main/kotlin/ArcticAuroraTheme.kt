package io.github.ireaderorg.plugins.arcticaurora

import ireader.plugin.api.*
import ireader.plugin.annotations.IReaderPlugin
import ireader.plugin.annotations.PluginMetadata

/**
 * Arctic Aurora Theme - Northern lights inspired with teal and purple gradients.
 */
@IReaderPlugin
@PluginMetadata(
    id = "io.github.ireaderorg.plugins.arctic-aurora",
    name = "Arctic Aurora",
    version = "1.0.0",
    versionCode = 1,
    description = "Northern lights inspired with teal and purple gradients",
    author = "IReader Team"
)
class ArcticAuroraTheme : ThemePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.arctic-aurora",
        name = "Arctic Aurora",
        version = "1.0.0",
        versionCode = 1,
        description = "Northern lights inspired with teal and purple gradients",
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
            bars = if (isDark) 0xFF00695C else 0xFF00BFA5,
            onBars = 0xFFFFFFFF,
            isBarLight = false
        )
    }

    private val lightColors = ThemeColorScheme(
        primary = 0xFF00BFA5, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFA7FFEB, onPrimaryContainer = 0xFF00695C,
        secondary = 0xFF7C4DFF, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFD1C4E9, onSecondaryContainer = 0xFF311B92,
        tertiary = 0xFF00E676, onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFB9F6CA, onTertiaryContainer = 0xFF1B5E20,
        error = 0xFFB00020, onError = 0xFFFFFFFF,
        errorContainer = 0xFFFCD8DF, onErrorContainer = 0xFF8B0000,
        background = 0xFFE0F7FA, onBackground = 0xFF004D40,
        surface = 0xFFFFFFFF, onSurface = 0xFF004D40,
        surfaceVariant = 0xFFE8F5E9, onSurfaceVariant = 0xFF00695C,
        outline = 0xFF80CBC4, outlineVariant = 0xFFB2DFDB,
        scrim = 0xFF000000, inverseSurface = 0xFF004D40,
        inverseOnSurface = 0xFFE0F7FA, inversePrimary = 0xFF64FFDA
    )
    
    private val darkColors = ThemeColorScheme(
        primary = 0xFF64FFDA, onPrimary = 0xFF00695C,
        primaryContainer = 0xFF00897B, onPrimaryContainer = 0xFFA7FFEB,
        secondary = 0xFFB388FF, onSecondary = 0xFF311B92,
        secondaryContainer = 0xFF651FFF, onSecondaryContainer = 0xFFD1C4E9,
        tertiary = 0xFF69F0AE, onTertiary = 0xFF1B5E20,
        tertiaryContainer = 0xFF2E7D32, onTertiaryContainer = 0xFFB9F6CA,
        error = 0xFFCF6679, onError = 0xFF000000,
        errorContainer = 0xFF8B0000, onErrorContainer = 0xFFFCD8DF,
        background = 0xFF0A1A1A, onBackground = 0xFFA7FFEB,
        surface = 0xFF142626, onSurface = 0xFFA7FFEB,
        surfaceVariant = 0xFF1E3333, onSurfaceVariant = 0xFF64FFDA,
        outline = 0xFF00897B, outlineVariant = 0xFF00695C,
        scrim = 0xFF000000, inverseSurface = 0xFFA7FFEB,
        inverseOnSurface = 0xFF00695C, inversePrimary = 0xFF00BFA5
    )
}
