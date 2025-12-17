package io.github.ireaderorg.plugins.graalvmengine

import ireader.plugin.api.*

/**
 * GraalVM JavaScript Engine Plugin for Desktop.
 * 
 * This plugin bundles the GraalVM Polyglot JS JARs (~38MB total).
 * The host app loads these JARs to provide GraalVM JS engine support.
 * 
 * Bundled JARs (in libs/ folder):
 * - polyglot-24.1.1.jar - Core Polyglot API
 * - js-language-24.1.1.jar - JavaScript language implementation
 * - truffle-api-24.1.1.jar - Truffle framework
 * - collections-24.1.1.jar - GraalVM collections
 * - word-24.1.1.jar - GraalVM word types
 * - nativeimage-24.1.1.jar - Native image support
 * 
 * The host app should:
 * 1. Extract the JAR files from the plugin package
 * 2. Add them to a URLClassLoader or the classpath
 * 3. Use the GraalVM Polyglot API (org.graalvm.polyglot.*)
 */
class GraalVMEnginePlugin : Plugin {
    
    private var pluginContext: PluginContext? = null
    
    override val manifest = PluginManifest(
        id = "io.github.ireaderorg.plugins.graalvm-engine",
        name = "GraalVM JavaScript Engine",
        version = "24.1.1",
        versionCode = 1,
        description = "GraalVM Polyglot JavaScript engine JARs for Desktop. Enables LNReader-compatible JS sources.",
        author = PluginAuthor(
            name = "IReader Team",
            website = "https://github.com/IReaderorg"
        ),
        type = PluginType.JS_ENGINE,
        permissions = listOf(PluginPermission.STORAGE),
        minIReaderVersion = "2.0.0",
        platforms = listOf(Platform.DESKTOP)
        // Note: JARs are in libs/ folder, not nativeLibraries
    )
    
    override fun initialize(context: PluginContext) {
        pluginContext = context
        context.log(LogLevel.INFO, "GraalVM JS library plugin initialized")
        context.log(LogLevel.INFO, "JAR files available at: libs/*.jar")
    }
    
    override fun cleanup() {
        pluginContext = null
    }
}
