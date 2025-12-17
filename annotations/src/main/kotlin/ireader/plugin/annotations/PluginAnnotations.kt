package ireader.plugin.annotations

/**
 * Marks a class as an IReader Plugin entry point.
 * The annotated class must implement one of:
 * - ThemePlugin
 * - TTSPlugin
 * - TranslationPlugin
 * - FeaturePlugin
 *
 * Example:
 * ```kotlin
 * @IReaderPlugin
 * class MyTheme : ThemePlugin {
 *     // ...
 * }
 * ```
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class IReaderPlugin

/**
 * Specifies plugin metadata. Optional - can also be defined in build.gradle.kts.
 * If both are specified, annotation values take precedence.
 * 
 * Note: Named PluginMetadata to avoid conflict with ireader.plugin.api.PluginInfo data class.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class PluginMetadata(
    val id: String = "",
    val name: String = "",
    val version: String = "",
    val versionCode: Int = 0,
    val description: String = "",
    val author: String = "",
    val authorEmail: String = "",
    val authorWebsite: String = "",
    val iconUrl: String = ""
)

/**
 * Marks a plugin as requiring specific permissions.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class RequiresPermissions(
    vararg val permissions: Permission
)

/**
 * Plugin permissions enum for annotation use.
 */
enum class Permission {
    NETWORK,
    STORAGE,
    READER_CONTEXT,
    LIBRARY_ACCESS,
    PREFERENCES,
    NOTIFICATIONS
}

/**
 * Marks a plugin as premium (paid).
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class PremiumPlugin(
    val price: Double,
    val currency: String = "USD",
    val trialDays: Int = 0
)

/**
 * Marks a plugin as freemium (free with paid features).
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class FreemiumPlugin
