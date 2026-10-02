# Decisions: Toolchain e Dipendenze

Data aggiornamento: 2 ottobre 2026

## Versioni e componenti selezionati

- **JDK Daemon Gradle:** JetBrains Runtime JDK 21 (`C:/Users/Utente/.gradle/jdks/jetbrains_s_r_o_-21-amd64-windows.2`) configurato tramite `org.gradle.java.home` e `.idea/gradle.xml` per compatibilità con Gradle 8.13 e Android Studio.
- **Gradle:** 8.13
- **Android Gradle Plugin (AGP):** 8.13.2
- **Kotlin:** 2.1.10
- **Kotlin Compose Plugin:** `org.jetbrains.kotlin.plugin.compose` 2.1.10
- **KSP:** 2.1.10-1.0.29
- **Robolectric:** 4.17 (aggiornato per supporto alla strumentazione bytecode Java 21)
- **Room:** 2.6.1
- **Min SDK:** 34 (Android 14)
- **Compile SDK / Target SDK:** 35
- **AndroidX Core KTX:** 1.15.0
- **AndroidX Activity Compose:** 1.10.0
- **Compose BOM:** 2025.02.00
- **Material 3:** Compose Material3 via Compose BOM

## Configurazione Moduli

- `:mobile:app` -> App Android Compose (minSdk 34)
- `:shared:core` -> Modello e regole pure JVM (senza dipendenze Android UI/Context)
- `:shared:exchange` -> Serializzazione/validazione pura JVM (senza dipendenze Android UI/Context)
- `:pc:app` -> App Desktop Windows Compose for Desktop (Kotlin/JVM 21, Compose Multiplatform 1.7.3) abilitata nello step W00 per l'editor portatile Windows 11 x64.

