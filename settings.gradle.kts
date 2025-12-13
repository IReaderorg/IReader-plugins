enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

rootProject.name = "IReader-plugins"

include(":annotations")
include(":compiler")

// Auto-include all plugins from plugins directory
File(rootDir, "plugins").eachDir { category ->
    category.eachDir { plugin ->
        if (File(plugin, "build.gradle.kts").exists()) {
            val name = ":plugins:${category.name}:${plugin.name}"
            include(name)
            project(name).projectDir = plugin
        }
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
        google()
        maven { setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
        maven { setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2") }
        maven { setUrl("https://jitpack.io") }
    }
}

inline fun File.eachDir(block: (File) -> Unit) {
    listFiles()?.filter { it.isDirectory }?.forEach { block(it) }
}
