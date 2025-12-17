plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.graalvm-engine")
    name.set("GraalVM JavaScript Engine")
    version.set("25.0.1")
    versionCode.set(1)
    description.set("GraalVM Polyglot JavaScript engine for Desktop - high-performance JS execution with full ES2022+ support")
    author.set("IReader Team")
    type.set(PluginType.JS_ENGINE)
    permissions.set(listOf(PluginPermission.STORAGE))
}

// This plugin is Desktop-only - uses reflection to access GraalVM at runtime
// The GraalVM Polyglot libraries are provided by the host app
// Native libraries are bundled in the plugin package

// No external dependencies - plugin uses reflection to access GraalVM at runtime
