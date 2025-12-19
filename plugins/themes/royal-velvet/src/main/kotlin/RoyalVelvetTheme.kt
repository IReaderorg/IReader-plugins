package io.github.ireaderorg.plugins.royalvelvet

import ireader.plugin.api.*
import ireader.plugin.annotations.IReaderPlugin
import ireader.plugin.annotations.PluginMetadata

/**
 * Royal Velvet Theme - Rich luxurious purple theme fit for royalty.
 */
@IReaderPlugin
@PluginMetadata(
    id = "io.github.ireaderorg.plugins.royal-velvet",
    name = "Royal Velvet",
    version = "1.0.0",
    versionCode = 1,
    description = "Rich luxurious purple theme fit for royalty",
    author = "IReader Team"
)
class RoyalVelvetTheme : ThemePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.royal-velvet",
        name = "Royal Velvet",
        version = "1.0.0",
        versionCode = 1,
        description = "Rich luxurious purple theme fit for royalty",
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
            bars = if (isDark) 0xFF38006B else 0xFF6A1B9A,
            onBars = 0xFFFFFFFF,
            isBarLight = false
        )
    }

    private val lightColors = ThemeColorScheme(
        primary = 0xFF6A1B9A, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFE1BEE7, onPrimaryContainer = 0xFF38006B,
        secondary = 0xFFAB47BC, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFF3E5F5, onSecondaryContainer = 0xFF7B1FA2,
        tertiary = 0xFF9C27B0, onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFE1BEE7, onTertiaryContainer = 0xFF6A1B9A,
        error = 0xFFB00020, onError = 0xFFFFFFFF,
        errorContainer = 0xFFFCD8DF, onErrorContainer = 0xFF8B0000,
        background = 0xFFF3E5F5, onBackground = 0xFF38006B,
        surface = 0xFFFFFFFF, onSurface = 0xFF38006B,
        surfaceVariant = 0xFFEDE7F6, onSurfaceVariant = 0xFF6A1B9A,
        outline = 0xFFCE93D8, outlineVariant = 0xFFE1BEE7,
        scrim = 0xFF000000, inverseSurface = 0xFF38006B,
        inverseOnSurface = 0xFFF3E5F5, inversePrimary = 0xFFCE93D8
    )
    
    private val darkColors = ThemeColorScheme(
        primary = 0xFFCE93D8, onPrimary = 0xFF38006B,
        primaryContainer = 0xFF8E24AA, onPrimaryContainer = 0xFFE1BEE7,
        secondary = 0xFFBA68C8, onSecondary = 0xFF7B1FA2,
        secondaryContainer = 0xFF9C27B0, onSecondaryContainer = 0xFFF3E5F5,
        tertiary = 0xFFAB47BC, onTertiary = 0xFF6A1B9A,
        tertiaryContainer = 0xFF8E24AA, onTertiaryContainer = 0xFFE1BEE7,
        error = 0xFFCF6679, onError = 0xFF000000,
        errorContainer = 0xFF8B0000, onErrorContainer = 0xFFFCD8DF,
        background = 0xFF150A1A, onBackground = 0xFFE1BEE7,
        surface = 0xFF22142A, onSurface = 0xFFE1BEE7,
        surfaceVariant = 0xFF2E1E3A, onSurfaceVariant = 0xFFCE93D8,
        outline = 0xFF8E24AA, outlineVariant = 0xFF7B1FA2,
        scrim = 0xFF000000, inverseSurface = 0xFFE1BEE7,
        inverseOnSurface = 0xFF38006B, inversePrimary = 0xFF6A1B9A
    )
}
