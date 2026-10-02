# Decisions: Toolchain e Dipendenze (Step A00)

Data: 2 ottobre 2026

## Versioni e componenti selezionati

- **JDK Daemon Gradle:** JetBrains Runtime JDK 21 (`C:/Users/Utente/.gradle/jdks/jetbrains_s_r_o_-21-amd64-windows.2`) configurato tramite `org.gradle.java.home` e `.idea/gradle.xml` per compatibilità con Gradle 9.0.0 e Android Studio.
- **Gradle:** 9.0.0
- **Android Gradle Plugin (AGP):** 8.8.0
- **Kotlin:** 2.1.0
- **Kotlin Compose Plugin:** `org.jetbrains.kotlin.plugin.compose` 2.1.0
- **Min SDK:** 34 (Android 14)
- **Compile SDK / Target SDK:** 35
- **AndroidX Core KTX:** 1.15.0
- **AndroidX Activity Compose:** 1.10.0
- **Compose BOM:** 2025.01.00
- **Material 3:** Compose Material3 via Compose BOM

## Configurazione Moduli

- `:mobile:app` -> App Android Compose (minSdk 34)
- `:shared:core` -> Modello e regole pure JVM (senza dipendenze Android UI/Context)
- `:shared:exchange` -> Serializzazione/validazione pura JVM (senza dipendenze Android UI/Context)
- `:pc:app` -> Riservato per l'editor Windows (abilitato in W00)
