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
    implementation(libs.pdfbox)

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    testImplementation(libs.junit)
    testImplementation(compose.desktop.uiTestJUnit4)
}

// jpackage is required to build the Windows app-image. The JDK used to run Gradle
// (e.g. Android Studio JBR) may not ship it, so packaging uses a dedicated JDK 21 toolchain.
val packagingJdk = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}

compose.desktop {
    application {
        mainClass = "com.onlyfield.assetmanager.pc.MainKt"
        javaHome = packagingJdk.get().metadata.installationPath.asFile.absolutePath
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
            modules("java.instrument", "jdk.unsupported")

            windows {
                iconFile.set(project.file("icons/app.ico"))
                menu = true
                upgradeUuid = "6f8c70f0-8c20-4e12-a7d5-2489c6d3ef2b"
            }
        }
    }
}

// Portable delivery: dist/OnlyFieldAssetManager/OnlyFieldAssetManager.exe (+ app/, runtime/)
// and a ZIP of the same folder. The local data/ folder next to the exe is never deleted or zipped.
val portableAppName = "OnlyFieldAssetManager"
val portableDistDir = rootProject.layout.projectDirectory.dir("dist")

val assemblePortable by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Builds the portable Windows x64 folder with the .exe in its root under dist/."
    dependsOn("createDistributable")
    from(layout.buildDirectory.dir("compose/binaries/main/app/$portableAppName"))
    into(portableDistDir.dir(portableAppName))
    preserve { include("data/**") }
}

tasks.register<Zip>("packagePortable") {
    group = "distribution"
    description = "Builds the portable Windows x64 folder and its ZIP archive under dist/."
    dependsOn(assemblePortable)
    from(portableDistDir.dir(portableAppName)) {
        exclude("data/**")
        into(portableAppName)
    }
    archiveFileName.set("$portableAppName-portable-x64-${compose.desktop.application.nativeDistributions.packageVersion}.zip")
    destinationDirectory.set(portableDistDir)
}
