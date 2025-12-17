plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.j2v8-engine")
    name.set("J2V8 JavaScript Engine")
    version.set("6.3.4")
    versionCode.set(1)
    description.set("V8 JavaScript engine for Android - enables LNReader-compatible sources with full ES6+ support")
    author.set("IReader Team")
    type.set(PluginType.JS_ENGINE)
    permissions.set(listOf(PluginPermission.STORAGE))
    mainClass.set("io.github.ireaderorg.plugins.j2v8engine.J2V8EnginePlugin")
}

// This plugin is Android-only - no JVM dependencies needed for compilation
// The J2V8 classes are provided by the host app at runtime
// Native libraries are bundled in the plugin package

// No external dependencies - plugin uses reflection to access J2V8 at runtime
