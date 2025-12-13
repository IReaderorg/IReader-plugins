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
}

// Build all plugins
tasks.register("buildAllPlugins") {
    group = "build"
    description = "Build all plugins"
    dependsOn(subprojects.filter { it.path.startsWith(":plugins:") }.map { "${it.path}:assemble" })
}
