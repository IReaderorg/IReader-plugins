import java.util.zip.ZipFile
import java.util.zip.ZipEntry

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
    platforms.set(listOf(PluginPlatform.ANDROID)) // Android only
}

// J2V8 version and Maven coordinates
val j2v8Version = "6.2.1"
val j2v8Group = "com.eclipsesource.j2v8"

// ABIs to include
val androidAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

// Task to download J2V8 AAR and extract native libraries
val downloadJ2V8Natives = tasks.register("downloadJ2V8Natives") {
    val jniLibsDir = file("src/main/jniLibs")
    outputs.dir(jniLibsDir)
    
    doLast {
        // Download J2V8 AAR from Maven Central
        val aarUrl = "https://repo1.maven.org/maven2/com/eclipsesource/j2v8/j2v8/$j2v8Version/j2v8-$j2v8Version.aar"
        val aarFile = file("build/tmp/j2v8-$j2v8Version.aar")
        
        aarFile.parentFile.mkdirs()
        
        if (!aarFile.exists()) {
            logger.lifecycle("Downloading J2V8 AAR from $aarUrl")
            ant.invokeMethod("get", mapOf("src" to aarUrl, "dest" to aarFile))
        }
        
        // Extract native libraries from AAR
        logger.lifecycle("Extracting native libraries from J2V8 AAR")
        jniLibsDir.mkdirs()
        
        val zipFile = ZipFile(aarFile)
        try {
            val entries = zipFile.entries()
            while (entries.hasMoreElements()) {
                val entry: ZipEntry = entries.nextElement()
                if (entry.name.startsWith("jni/") && entry.name.endsWith(".so")) {
                    // jni/arm64-v8a/libj2v8.so -> arm64-v8a/libj2v8.so
                    val relativePath = entry.name.removePrefix("jni/")
                    val outputFile = File(jniLibsDir, relativePath)
                    outputFile.parentFile.mkdirs()
                    
                    val inputStream = zipFile.getInputStream(entry)
                    val outputStream = outputFile.outputStream()
                    try {
                        inputStream.copyTo(outputStream)
                    } finally {
                        inputStream.close()
                        outputStream.close()
                    }
                    logger.lifecycle("Extracted: $relativePath (${outputFile.length()} bytes)")
                }
            }
        } finally {
            zipFile.close()
        }
    }
}

// Make packagePlugin depend on downloading natives
tasks.named("packagePlugin") {
    dependsOn(downloadJ2V8Natives)
}
