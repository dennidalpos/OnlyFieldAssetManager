plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    id("org.jetbrains.compose") version "1.12.1" apply false
    alias(libs.plugins.ksp) apply false
}

// Tests assert the default Italian texts: pin the test JVM locale so results do not depend on the machine (CI runners are en-US).
subprojects {
    tasks.withType<Test>().configureEach {
        systemProperty("user.language", "it")
        systemProperty("user.country", "IT")
    }
}
