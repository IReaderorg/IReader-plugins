package io.github.ireaderorg.plugins.nordicfrost

import ireader.plugin.api.*
import ireader.plugin.annotations.IReaderPlugin
import ireader.plugin.annotations.PluginMetadata

/**
 * Nordic Frost Theme - Cool icy blues with silver accents inspired by Scandinavian winters.
 */
@IReaderPlugin
@PluginMetadata(
    id = "io.github.ireaderorg.plugins.nordic-frost",
    name = "Nordic Frost",
    version = "1.0.0",
    versionCode = 1,
    description = "Cool icy blues with silver accents inspired by Scandinavian winters",
    author = "IReader Team"
)
class NordicFrostTheme : ThemePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.nordic-frost",
        name = "Nordic Frost",
        version = "1.0.0",
        versionCode = 1,
        description = "Cool icy blues with silver accents inspired by Scandinavian winters",
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
            bars = if (isDark) 0xFF1A237E else 0xFF5C6BC0,
            onBars = 0xFFFFFFFF,
            isBarLight = false
        )
    }

    private val lightColors = ThemeColorScheme(
        primary = 0xFF5C6BC0, onPrimary = 0xFFFFFFFF,
        primaryContainer = 0xFFE8EAF6, onPrimaryContainer = 0xFF1A237E,
        secondary = 0xFF78909C, onSecondary = 0xFFFFFFFF,
        secondaryContainer = 0xFFCFD8DC, onSecondaryContainer = 0xFF37474F,
        tertiary = 0xFF90A4AE, onTertiary = 0xFFFFFFFF,
        tertiaryContainer = 0xFFECEFF1, onTertiaryContainer = 0xFF455A64,
        error = 0xFFB00020, onError = 0xFFFFFFFF,
        errorContainer = 0xFFFCD8DF, onErrorContainer = 0xFF8B0000,
        background = 0xFFF5F7FA, onBackground = 0xFF263238,
        surface = 0xFFFFFFFF, onSurface = 0xFF263238,
        surfaceVariant = 0xFFECEFF1, onSurfaceVariant = 0xFF546E7A,
        outline = 0xFFB0BEC5, outlineVariant = 0xFFCFD8DC,
        scrim = 0xFF000000, inverseSurface = 0xFF263238,
        inverseOnSurface = 0xFFECEFF1, inversePrimary = 0xFF9FA8DA
    )
    
    private val darkColors = ThemeColorScheme(
        primary = 0xFF9FA8DA, onPrimary = 0xFF1A237E,
        primaryContainer = 0xFF3949AB, onPrimaryContainer = 0xFFE8EAF6,
        secondary = 0xFFB0BEC5, onSecondary = 0xFF37474F,
        secondaryContainer = 0xFF546E7A, onSecondaryContainer = 0xFFCFD8DC,
        tertiary = 0xFF90A4AE, onTertiary = 0xFF455A64,
        tertiaryContainer = 0xFF607D8B, onTertiaryContainer = 0xFFECEFF1,
        error = 0xFFCF6679, onError = 0xFF000000,
        errorContainer = 0xFF8B0000, onErrorContainer = 0xFFFCD8DF,
        background = 0xFF0D1117, onBackground = 0xFFE8EAF6,
        surface = 0xFF161B22, onSurface = 0xFFE8EAF6,
        surfaceVariant = 0xFF21262D, onSurfaceVariant = 0xFFB0BEC5,
        outline = 0xFF546E7A, outlineVariant = 0xFF37474F,
        scrim = 0xFF000000, inverseSurface = 0xFFE8EAF6,
        inverseOnSurface = 0xFF1A237E, inversePrimary = 0xFF5C6BC0
    )
}
