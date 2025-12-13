plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.1.0")
    implementation("com.android.tools.build:gradle:8.7.3")
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.1.0-1.0.29")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}
