plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("org.jetbrains.compose")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared:core"))
    implementation(project(":shared:exchange"))
    implementation(libs.kotlinx.serialization.json)

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    testImplementation(libs.junit)
}

compose.desktop {
    application {
        mainClass = "com.onlyfield.assetmanager.pc.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.AppImage,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe
            )
            packageName = "OnlyFieldAssetManager"
            packageVersion = "1.0.0"
            description = "OnlyFieldAssetManager Portable Windows Desktop App"
            copyright = "© 2026 OnlyField"
            vendor = "OnlyField"

            windows {
                menu = true
                upgradeUuid = "6f8c70f0-8c20-4e12-a7d5-2489c6d3ef2b"
            }
        }
    }
}
