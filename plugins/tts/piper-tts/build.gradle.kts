plugins {
    id("ireader-plugin")
}

pluginConfig {
    id.set("io.github.ireaderorg.plugins.piper-tts")
    name.set("Piper TTS")
    version.set("1.2.0")
    versionCode.set(1)
    description.set("High-performance neural text-to-speech with 30+ voices in 20+ languages. Powered by Piper.")
    author.set("IReader Team")
    type.set(PluginType.TTS)
    permissions.set(listOf(PluginPermission.STORAGE, PluginPermission.NETWORK))
    mainClass.set("io.github.ireaderorg.plugins.pipertts.PiperTTSPlugin")
    platforms.set(listOf(PluginPlatform.DESKTOP)) // Desktop only
}

// This plugin is Desktop-only - uses reflection to access Piper JNI at runtime
// The Piper JNI library is provided by the host app
// Native libraries are bundled in the plugin package

// No external dependencies - plugin uses reflection to access Piper JNI at runtime
