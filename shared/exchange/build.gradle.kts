plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
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
    implementation(libs.kotlinx.serialization.json)
    // QR matrices for the label sheet (pure Java)
    implementation(libs.zxing.core)
    testImplementation(libs.junit)
}

tasks.withType<Test>().configureEach {
    systemProperty("ofam.test.classpath", sourceSets["test"].runtimeClasspath.asPath)
}


// Importable demo project (2 sites × 2 floors, cabled switches): fixtures/demo/onlyfield-demo.ofam
tasks.register<JavaExec>("demoPackage") {
    group = "application"
    description = "Writes the demo .ofam package for manual tests."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.onlyfield.assetmanager.exchange.DemoSeed")
    workingDir = rootDir
}
