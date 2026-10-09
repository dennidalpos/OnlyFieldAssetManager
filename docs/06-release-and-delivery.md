# Build e rilascio

## Build locale

L’assemblaggio portable esclude `data/**` dalla sorgente e conserva `data/**` nella destinazione. Un runtime già usato non introduce progetti, media o preferenze nella distribuzione. Anche lo ZIP esclude i dati. Regressione ripetibile: `powershell -NoProfile -File tools/testing/portable-data.ps1`, con runtime sintetico contaminato e destinazione popolata; verifica hash, inventario e ZIP usando le definizioni dei task di produzione. AUD-45 corretto e verificato il 9 ottobre 2026; [semantica Gradle Sync](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Sync.html) consultata nella stessa data.

```powershell
.\gradlew.bat :pc:app:packagePortable
```

Il task crea `dist/OnlyFieldAssetManager/OnlyFieldAssetManager.exe` e lo ZIP portable. La directory `data/` resta accanto all'eseguibile e non deve essere inclusa nel pacchetto.

## CI

`.github/workflows/release.yml` si avvia manualmente o su tag `v*`. Verifica test JVM/Compose, genera APK debug e ZIP portable, esegue `prepare-release.ps1` e calcola SHA-256. L'avvio manuale carica artefatti; un tag pubblica la release.

La pubblicazione reale su tag è verificata dalla [release v1.0.1](https://github.com/dennidalpos/OnlyFieldAssetManager/releases/tag/v1.0.1): [run 37238450362](https://github.com/dennidalpos/OnlyFieldAssetManager/actions/runs/37238450362) completata con esito `success`, APK debug, ZIP portable e `SHA256SUMS` presenti. Stato ricontrollato tramite GitHub CLI il 5 ottobre 2026; RES-01 già chiuso. Non creare tag o release per la sola verifica della documentazione. Le modifiche successive alla release richiedono una nuova pubblicazione esplicita.

## Risorse di test Windows — AUD-22

processTestResources copia la sola fixtures/v1_sample_project.json nel classpath test. Il JAR principale non include la fixture; distribuzione e comando portable invariati. Le prove non dipendono dalla directory corrente. [Gradle ProcessResources](https://docs.gradle.org/current/dsl/org.gradle.language.jvm.tasks.ProcessResources.html), consultato il 6 ottobre 2026.

La rigenerazione createDistributable rifiuta un runtime sorgente contenente data/ prima di rimuoverlo. Conservare e spostare quei dati prima di rigenerare; usare l’EXE in dist per le sessioni applicative. Il controllo non cancella o sposta dati automaticamente.
