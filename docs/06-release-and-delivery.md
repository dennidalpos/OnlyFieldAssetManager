# Rilascio, Build e Consegna

Data: 2 ottobre 2026

## Comandi Gradle di Verifica e Build

Tutti i comandi eseguono dalla radice del repository usando il wrapper Gradle:

```powershell
# Esecuzione dell'intera suite di test unitari (73 test)
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest

# Generazione APK Android Debug e Release
.\gradlew.bat :mobile:app:assembleDebug :mobile:app:assembleRelease

# Generazione Distribuzione Windows Desktop (AppImage / Executable portable x64)
.\gradlew.bat :pc:app:jar :pc:app:createDistributable
```

## Artefatti di Rilascio Finale

- **Android APK Debug:** `mobile/app/build/outputs/apk/debug/app-debug.apk`
- **Windows Desktop JAR:** `pc/app/build/libs/app.jar`
- **Windows Desktop Portable Distribution:** `pc/app/build/compose/binaries/main/appImage/OnlyFieldAssetManager/`
- **Contratto Dati Consolidato:** Versione `1.7` (`docs/02-domain-data-contract.md`)
