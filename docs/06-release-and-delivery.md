# Build e rilascio

## Build locale

**AUD-45 aperto:** l’assemblaggio portable conserva i dati destinatari dalla cancellazione, ma può sovrascriverli copiando `data/` dal runtime sorgente già usato. Evitare packaging su una `dist` popolata fino alla correzione; riprodotto su fixture isolate, nessun dato utente toccato. L’esclusione dal solo ZIP non protegge la directory di distribuzione. Evidenza e chiusura nell’[audit del 9 ottobre](repo-residuals-2026-10-09.md).

```powershell
.\gradlew.bat :pc:app:packagePortable
```

Il task crea `dist/OnlyFieldAssetManager/OnlyFieldAssetManager.exe` e lo ZIP portable. La directory `data/` resta accanto all'eseguibile e non deve essere inclusa nel pacchetto.

## CI

`.github/workflows/release.yml` si avvia manualmente o su tag `v*`. Verifica test JVM/Compose, genera APK debug e ZIP portable, esegue `prepare-release.ps1` e calcola SHA-256. L'avvio manuale carica artefatti; un tag pubblica la release.

La pubblicazione reale su tag è verificata dalla [release v1.0.1](https://github.com/dennidalpos/OnlyFieldAssetManager/releases/tag/v1.0.1): [run 37238450362](https://github.com/dennidalpos/OnlyFieldAssetManager/actions/runs/37238450362) completata con esito `success`, APK debug, ZIP portable e `SHA256SUMS` presenti. Stato ricontrollato tramite GitHub CLI il 5 ottobre 2026; RES-01 già chiuso. Non creare tag o release per la sola verifica della documentazione. Le modifiche successive alla release richiedono una nuova pubblicazione esplicita.

## Risorse di test Windows — AUD-22

processTestResources copia la sola fixtures/v1_sample_project.json nel classpath test. Il JAR principale non include la fixture; distribuzione e comando portable invariati. Le prove non dipendono dalla directory corrente. [Gradle ProcessResources](https://docs.gradle.org/current/dsl/org.gradle.language.jvm.tasks.ProcessResources.html), consultato il 6 ottobre 2026.
