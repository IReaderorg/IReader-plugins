package io.github.ireaderorg.plugins.desertsand

import ireader.plugin.api.*
import ireader.plugin.annotations.IReaderPlugin
import ireader.plugin.annotations.PluginMetadata

/**
 * Desert Sand Theme - Warm earthy tones inspired by desert landscapes.
 */
@IReaderPlugin
@PluginMetadata(
    id = "io.github.ireaderorg.plugins.desert-sand",
    name = "Desert Sand",
    version = "1.0.0",
    versionCode = 1,
    description = "Warm earthy tones inspired by desert landscapes",
    author = "IReader Team"
)
class DesertSandTheme : ThemePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.desert-sand",
        name = "Desert Sand",
        version = "1.0.0",
        versionCode = 1,
        description = "Warm earthy tones inspired by desert landscapes",
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
            bars = if (isDark) 0xFF5D4037 else 0xFFBF8040,
            onBars = 0xFFFFFFFF,
            isBarLight = false
        )
    }

    private val lightColors = ThemeColorScheme(
        primary = 0xFFBF8040, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFFFE0B2, onPrimaryContainer = 0xFF5D4037,
        secondary = 0xFFA1887F, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFD7CCC8, onSecondaryContainer = 0xFF4E342E,
        tertiary = 0xFFFF8F00, onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFFFE082, onTertiaryContainer = 0xFFE65100,
        error = 0xFFB00020, onError = 0xFFFFFFFF,
        errorContainer = 0xFFFCD8DF, onErrorContainer = 0xFF8B0000,
        background = 0xFFFFF8E1, onBackground = 0xFF5D4037,
        surface = 0xFFFFFFFF, onSurface = 0xFF5D4037,
        surfaceVariant = 0xFFFFECB3, onSurfaceVariant = 0xFF795548,
        outline = 0xFFBCAAA4, outlineVariant = 0xFFD7CCC8,
        scrim = 0xFF000000, inverseSurface = 0xFF5D4037,
        inverseOnSurface = 0xFFFFF8E1, inversePrimary = 0xFFFFCC80
    )
    
    private val darkColors = ThemeColorScheme(
        primary = 0xFFFFCC80, onPrimary = 0xFF5D4037,
        primaryContainer = 0xFF8D6E63, onPrimaryContainer = 0xFFFFE0B2,
        secondary = 0xFFBCAAA4, onSecondary = 0xFF4E342E,
        secondaryContainer = 0xFF6D4C41, onSecondaryContainer = 0xFFD7CCC8,
        tertiary = 0xFFFFB74D, onTertiary = 0xFFE65100,
        tertiaryContainer = 0xFFF57C00, onTertiaryContainer = 0xFFFFE082,
        error = 0xFFCF6679, onError = 0xFF000000,
        errorContainer = 0xFF8B0000, onErrorContainer = 0xFFFCD8DF,
        background = 0xFF1A1410, onBackground = 0xFFFFE0B2,
        surface = 0xFF2D241C, onSurface = 0xFFFFE0B2,
        surfaceVariant = 0xFF3E3028, onSurfaceVariant = 0xFFFFCC80,
        outline = 0xFF8D6E63, outlineVariant = 0xFF6D4C41,
        scrim = 0xFF000000, inverseSurface = 0xFFFFE0B2,
        inverseOnSurface = 0xFF5D4037, inversePrimary = 0xFFBF8040
    )
}
