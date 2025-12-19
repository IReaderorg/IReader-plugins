package io.github.ireaderorg.plugins.slategray

import ireader.plugin.api.*
import ireader.plugin.annotations.IReaderPlugin
import ireader.plugin.annotations.PluginMetadata

/**
 * Slate Gray Theme - Professional minimal theme with elegant gray tones.
 */
@IReaderPlugin
@PluginMetadata(
    id = "io.github.ireaderorg.plugins.slate-gray",
    name = "Slate Gray",
    version = "1.0.0",
    versionCode = 1,
    description = "Professional minimal theme with elegant gray tones",
    author = "IReader Team"
)
class SlateGrayTheme : ThemePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.slate-gray",
        name = "Slate Gray",
        version = "1.0.0",
        versionCode = 1,
        description = "Professional minimal theme with elegant gray tones",
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
            bars = if (isDark) 0xFF263238 else 0xFF546E7A,
            onBars = 0xFFFFFFFF,
            isBarLight = false
        )
    }

    private val lightColors = ThemeColorScheme(
        primary = 0xFF546E7A, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFCFD8DC, onPrimaryContainer = 0xFF263238,
        secondary = 0xFF78909C, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFECEFF1, onSecondaryContainer = 0xFF37474F,
        tertiary = 0xFF607D8B, onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFCFD8DC, onTertiaryContainer = 0xFF455A64,
        error = 0xFFB00020, onError = 0xFFFFFFFF,
        errorContainer = 0xFFFCD8DF, onErrorContainer = 0xFF8B0000,
        background = 0xFFECEFF1, onBackground = 0xFF263238,
        surface = 0xFFFFFFFF, onSurface = 0xFF263238,
        surfaceVariant = 0xFFF5F5F5, onSurfaceVariant = 0xFF455A64,
        outline = 0xFF90A4AE, outlineVariant = 0xFFB0BEC5,
        scrim = 0xFF000000, inverseSurface = 0xFF263238,
        inverseOnSurface = 0xFFECEFF1, inversePrimary = 0xFFB0BEC5
    )
    
    private val darkColors = ThemeColorScheme(
        primary = 0xFFB0BEC5, onPrimary = 0xFF263238,
        primaryContainer = 0xFF455A64, onPrimaryContainer = 0xFFCFD8DC,
        secondary = 0xFF90A4AE, onSecondary = 0xFF37474F,
        secondaryContainer = 0xFF546E7A, onSecondaryContainer = 0xFFECEFF1,
        tertiary = 0xFF78909C, onTertiary = 0xFF455A64,
        tertiaryContainer = 0xFF546E7A, onTertiaryContainer = 0xFFCFD8DC,
        error = 0xFFCF6679, onError = 0xFF000000,
        errorContainer = 0xFF8B0000, onErrorContainer = 0xFFFCD8DF,
        background = 0xFF0F1518, onBackground = 0xFFCFD8DC,
        surface = 0xFF1A2226, onSurface = 0xFFCFD8DC,
        surfaceVariant = 0xFF252E33, onSurfaceVariant = 0xFFB0BEC5,
        outline = 0xFF546E7A, outlineVariant = 0xFF455A64,
        scrim = 0xFF000000, inverseSurface = 0xFFCFD8DC,
        inverseOnSurface = 0xFF263238, inversePrimary = 0xFF546E7A
    )
}
