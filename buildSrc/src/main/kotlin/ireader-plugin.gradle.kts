import org.gradle.kotlin.dsl.*
import java.util.Properties

/**
 * Convention plugin for IReader plugins
 * Supports both JVM (Desktop) and Android platforms
 */
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp")
}

// Create plugin extension
val pluginConfig = extensions.create<PluginExtension>("pluginConfig")

// Set defaults
pluginConfig.apply {
    minIReaderVersion.convention(PluginConfig.minIReaderVersion)
    permissions.convention(emptyList())
    authorEmail.convention("")
    authorWebsite.convention("")
    iconUrl.convention("")
    monetization.convention(PluginMonetizationType.FREE)
    price.convention(0.0)
    currency.convention("USD")
    trialDays.convention(0)
    mainClass.convention("")
    platforms.convention(listOf(PluginPlatform.ANDROID, PluginPlatform.IOS, PluginPlatform.DESKTOP))
    featured.convention(false)
    tags.convention(emptyList())
    skipFromRepo.convention(false)
    skipFromRepoReason.convention("")
    metadata.convention(emptyMap())
}

repositories {
    mavenCentral()
    google()
    maven { setUrl("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    maven { setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2") }
}

dependencies {
    // Plugin API - compileOnly since IReader provides it at runtime
    compileOnly("io.github.ireaderorg:plugin-api:${PluginConfig.pluginApiVersion}")
    
    // Kotlin stdlib and coroutines - compileOnly since IReader provides them
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    
    // Ktor - compileOnly since IReader provides it
    compileOnly("io.ktor:ktor-client-core:3.0.3")
    compileOnly("io.ktor:ktor-client-content-negotiation:3.0.3")
    compileOnly("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
    
    // KSP Annotations for plugin discovery
    implementation(project(":annotations"))
    ksp(project(":compiler"))
}

kotlin {
    jvmToolchain(21)
}

// Register manifest generation task
val generateManifest = tasks.register<PluginManifestGenerator>("generatePluginManifest") {
    pluginId.set(pluginConfig.id)
    pluginName.set(pluginConfig.name)
    pluginVersion.set(pluginConfig.version)
    pluginVersionCode.set(pluginConfig.versionCode)
    pluginDescription.set(pluginConfig.description)
    pluginAuthor.set(pluginConfig.author)
    pluginAuthorEmail.set(pluginConfig.authorEmail)
    pluginAuthorWebsite.set(pluginConfig.authorWebsite)
    pluginType.set(pluginConfig.type.map { it.name })
    pluginPermissions.set(pluginConfig.permissions.map { list -> list.map { it.name } })
    minIReaderVersion.set(pluginConfig.minIReaderVersion)
    pluginIconUrl.set(pluginConfig.iconUrl)
    monetizationType.set(pluginConfig.monetization.map { it.name })
    price.set(pluginConfig.price)
    currency.set(pluginConfig.currency)
    trialDays.set(pluginConfig.trialDays)
    mainClass.set(pluginConfig.mainClass)
    pluginPlatforms.set(pluginConfig.platforms.map { list -> list.map { it.name } })
    featured.set(pluginConfig.featured)
    tags.set(pluginConfig.tags)
    skipFromRepo.set(pluginConfig.skipFromRepo)
    skipFromRepoReason.set(pluginConfig.skipFromRepoReason)
    metadata.set(pluginConfig.metadata)
    outputDir.set(layout.buildDirectory.dir("generated/plugin"))
}

// Generate DEX for Android from JAR
val generateDex = tasks.register<Exec>("generateDex") {
    dependsOn(tasks.named("jar"))
    
    val jarFile = tasks.named<Jar>("jar").flatMap { it.archiveFile }
    val dexDir = layout.buildDirectory.dir("dex")
    
    inputs.file(jarFile)
    outputs.dir(dexDir)
    
    doFirst {
        dexDir.get().asFile.mkdirs()
    }
    
    // Use d8 from Android SDK to convert JAR to DEX
    // Try multiple sources for Android SDK path
    var androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT") ?: ""
    
    // If not found in env, try to read from local.properties
    if (androidHome.isEmpty()) {
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            val props = Properties()
            localPropsFile.inputStream().use { props.load(it) }
            androidHome = props.getProperty("sdk.dir", "")
        }
    }
    
    // Also try parent project's local.properties (for IReader-plugins subproject)
    if (androidHome.isEmpty()) {
        val parentLocalPropsFile = rootProject.file("../local.properties")
        if (parentLocalPropsFile.exists()) {
            val props = Properties()
            parentLocalPropsFile.inputStream().use { props.load(it) }
            androidHome = props.getProperty("sdk.dir", "")
        }
    }
    
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    val d8Executable = if (isWindows) "d8.bat" else "d8"
    val d8Path = if (androidHome.isNotEmpty()) {
        File(androidHome, "build-tools/${PluginConfig.buildToolsVersion}/$d8Executable").absolutePath
    } else {
        d8Executable // Fallback to PATH
    }
    
    commandLine(
        d8Path,
        "--output", dexDir.get().asFile.absolutePath,
        "--min-api", PluginConfig.minSdk.toString(),
        jarFile.get().asFile.absolutePath
    )
    
    // Skip if d8 not available (desktop-only build)
    isIgnoreExitValue = true
}

// Package plugin as .iplugin (ZIP with manifest + JAR + DEX + native libs)
val packagePlugin = tasks.register<Zip>("packagePlugin") {
    dependsOn(generateManifest, tasks.named("jar"))
    
    // Try to include DEX but don't fail if not available
    try {
        dependsOn(generateDex)
    } catch (e: Exception) {
        logger.info("DEX generation skipped: ${e.message}")
    }
    
    archiveBaseName.set(pluginConfig.id)
    archiveVersion.set(pluginConfig.version)
    archiveExtension.set("iplugin")
    destinationDirectory.set(layout.buildDirectory.dir("outputs"))
    
    // Include manifest
    from(layout.buildDirectory.dir("generated/plugin")) {
        include("plugin.json")
    }
    
    // Include JAR for Desktop/JVM
    from(tasks.named<Jar>("jar").map { it.archiveFile }) {
        rename { "classes.jar" }
    }
    
    // Include DEX for Android (if generated)
    from(layout.buildDirectory.dir("dex")) {
        include("classes.dex")
        into("android")
    }
    
    // Include native libraries for Android (jniLibs/<abi>/*.so)
    // Note: Don't check exists() at configuration time - directory may be created at execution time
    from("src/main/jniLibs") {
        into("native/android")
    }
    
    // Include native libraries for Desktop (native/<platform>/*.dll|.so|.dylib)
    from("src/main/native") {
        into("native")
    }
    
    // Include bundled JARs/libraries (libs/*.jar)
    from("src/main/libs") {
        into("libs")
    }
    
    // Include KSP generated sources
    from(layout.buildDirectory.dir("generated/ksp/main/kotlin")) {
        into("generated")
    }
}

// Create APK-like structure for Android
val packageAndroidPlugin = tasks.register<Zip>("packageAndroidPlugin") {
    dependsOn(generateManifest, generateDex)
    
    archiveBaseName.set(pluginConfig.id)
    archiveVersion.set(pluginConfig.version)
    archiveExtension.set("apk")
    destinationDirectory.set(layout.buildDirectory.dir("outputs/android"))
    
    // Include manifest as AndroidManifest.xml
    from(layout.buildDirectory.dir("generated/plugin")) {
        include("plugin.json")
        rename("plugin.json", "assets/plugin.json")
    }
    
    // Include DEX
    from(layout.buildDirectory.dir("dex")) {
        include("classes.dex")
    }
}

tasks.named("assemble") {
    dependsOn(packagePlugin)
    // Only depend on Android package if DEX generation succeeds
    finalizedBy(packageAndroidPlugin)
}
