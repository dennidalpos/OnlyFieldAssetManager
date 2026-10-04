# Build e rilascio

## Build locale

```powershell
.\gradlew.bat :pc:app:packagePortable
```

Il task crea `dist/OnlyFieldAssetManager/OnlyFieldAssetManager.exe` e lo ZIP portable. La directory `data/` resta accanto all'eseguibile e non deve essere inclusa nel pacchetto.

## CI

`.github/workflows/release.yml` si avvia manualmente o su tag `v*`. Verifica test JVM/Compose, genera APK debug e ZIP portable, esegue `prepare-release.ps1` e calcola SHA-256. L'avvio manuale carica artefatti; un tag pubblica la release.

La pubblicazione reale su tag resta RES-01 finche non eseguita. Non creare tag o release per la sola verifica della documentazione.
