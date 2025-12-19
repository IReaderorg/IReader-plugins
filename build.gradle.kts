buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath(libs.android.gradle)
        classpath(libs.kotlin.gradle)
        classpath(libs.serialization.gradle)
    }
}

allprojects {
    group = "io.github.ireaderorg.plugins"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

// Generate plugin repository index
tasks.register<PluginRepoTask>("repo") {
    group = "build"
    description = "Generate plugin repository index"
    repoDir.set(layout.projectDirectory.dir("repo"))
    pluginBuildDirs.set(
        subprojects.filter {
            it.path.startsWith(":plugins:") && it.path.count { c -> c == ':' } == 3
        }.map { it.layout.buildDirectory.get().asFile.absolutePath }
    )
    // Plugin source directories for icon lookup (assets/icon.png)
    pluginSourceDirs.set(
        subprojects.filter {
            it.path.startsWith(":plugins:") && it.path.count { c -> c == ':' } == 3
        }.map { it.projectDir.absolutePath }
    )
    // Base URL for plugin icons - icons will be at {baseIconUrl}/images/{pluginId}.png
    // This URL points to the gh-pages branch where the repo is hosted
    baseIconUrl.set("https://raw.githubusercontent.com/IReaderorg/IReader-plugins/gh-pages/repo")
}

// Build all plugins
tasks.register("buildAllPlugins") {
    group = "build"
    description = "Build all plugins"
    // Only include actual plugin projects (3 segments: :plugins:category:pluginName)
    dependsOn(subprojects.filter {
        it.path.startsWith(":plugins:") && it.path.count { c -> c == ':' } == 3
    }.map { "${it.path}:assemble" })
}
