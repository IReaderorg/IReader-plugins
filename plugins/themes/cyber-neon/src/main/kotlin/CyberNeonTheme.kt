package io.github.ireaderorg.plugins.cyberneon

import ireader.plugin.api.*
import ireader.plugin.annotations.IReaderPlugin
import ireader.plugin.annotations.PluginMetadata

/**
 * Cyber Neon Theme - Futuristic cyberpunk vibes with electric cyan and magenta.
 */
@IReaderPlugin
@PluginMetadata(
    id = "io.github.ireaderorg.plugins.cyber-neon",
    name = "Cyber Neon",
    version = "1.0.0",
    versionCode = 1,
    description = "Futuristic cyberpunk theme with electric cyan and magenta accents",
    author = "IReader Team"
)
class CyberNeonTheme : ThemePlugin {
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.cyber-neon",
        name = "Cyber Neon",
        version = "1.0.0",
        versionCode = 1,
        description = "Futuristic cyberpunk theme with electric cyan and magenta accents",
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
            bars = if (isDark) 0xFF0D0D1A else 0xFF00838F,
            onBars = if (isDark) 0xFF00E5FF else 0xFFFFFFFF,
            isBarLight = false
        )
    }

    private val lightColors = ThemeColorScheme(
        primary = 0xFF00BCD4, onPrimary = 0xFF003544,
        primaryContainer = 0xFFB2EBF2, onPrimaryContainer = 0xFF006064,
        secondary = 0xFFE040FB, onSecondary = 0xFF38006B,
        secondaryContainer = 0xFFF3E5F5, onSecondaryContainer = 0xFF7B1FA2,
        tertiary = 0xFF00E5FF, onTertiary = 0xFF003544,
        tertiaryContainer = 0xFFB2EBF2, onTertiaryContainer = 0xFF006064,
        error = 0xFFFF1744, onError = 0xFFFFFFFF,
        errorContainer = 0xFFFFCDD2, onErrorContainer = 0xFFB71C1C,
        background = 0xFFF0F4F8, onBackground = 0xFF1A1A2E,
        surface = 0xFFFFFFFF, onSurface = 0xFF1A1A2E,
        surfaceVariant = 0xFFE0F7FA, onSurfaceVariant = 0xFF006064,
        outline = 0xFF00ACC1, outlineVariant = 0xFFB2EBF2,
        scrim = 0xFF000000, inverseSurface = 0xFF1A1A2E,
        inverseOnSurface = 0xFFE0F7FA, inversePrimary = 0xFF00E5FF
    )
    
    private val darkColors = ThemeColorScheme(
        primary = 0xFF00E5FF, onPrimary = 0xFF003544,
        primaryContainer = 0xFF00838F, onPrimaryContainer = 0xFFB2EBF2,
        secondary = 0xFFE040FB, onSecondary = 0xFF38006B,
        secondaryContainer = 0xFF9C27B0, onSecondaryContainer = 0xFFF3E5F5,
        tertiary = 0xFF76FF03, onTertiary = 0xFF1B5E20,
        tertiaryContainer = 0xFF33691E, onTertiaryContainer = 0xFFCCFF90,
        error = 0xFFFF5252, onError = 0xFF000000,
        errorContainer = 0xFFB71C1C, onErrorContainer = 0xFFFFCDD2,
        background = 0xFF0D0D1A, onBackground = 0xFFE0E0E0,
        surface = 0xFF16162B, onSurface = 0xFFE0E0E0,
        surfaceVariant = 0xFF1F1F3D, onSurfaceVariant = 0xFF00E5FF,
        outline = 0xFF00838F, outlineVariant = 0xFF006064,
        scrim = 0xFF000000, inverseSurface = 0xFFE0E0E0,
        inverseOnSurface = 0xFF1A1A2E, inversePrimary = 0xFF00BCD4
    )
}
