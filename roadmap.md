# OnlyFieldAssetManager — Stato operativo

Aggiornato: 2 ottobre 2026. Usare plan.md per le istruzioni e questo file per registrare esecuzione ed evidenze.

## Sequenza Android — Attività pendenti / in corso

Eseguire in ordine A04–A13. Prima di passare allo step successivo completare le verifiche del precedente e registrare gli esiti.

| Step | Attività | Stato |
| :--- | :--- | :--- |
| A04 | Credenziali e protezione opzionale | Da iniziare |
| A05 | Rack, modelli e primo PDF consultabile | Da iniziare |
| A06 | Planimetrie, foto, allegati e annotazioni | Da iniziare |
| A07 | Cablaggio, pannelli e percorsi condivisi | Da iniziare |
| A08 | Configurazioni, rete logica e videosorveglianza | Da iniziare |
| A09 | Alimentazione e badge | Da iniziare |
| A10 | Recupero eliminazioni, fusione e modifiche multiple | Da iniziare |
| A11 | PDF/XLSX/Markdown, report composti e stampa | Da iniziare |
| A12 | Acquisizione cartografica e prove offline | Da iniziare |
| A13 | Pilota, APK verificato e contratto per Windows | Da iniziare |

## Sequenza Android — Attività completate

| Step | Attività | Data Completamento | Esito |
| :--- | :--- | :--- | :--- |
| A00 | Ambiente, struttura mobile/pc/shared e app Android minima | 2 ottobre 2026 | Completato con successi nei test ed APK debug |
| A01 | Modello comune, contratto iniziale e fixtures | 2 ottobre 2026 | Completato con modello core, serializzatore pacchetti, validatore, fixtures e test |
| A02 | Persistenza, inventario e ricerca | 2 ottobre 2026 | Completato con Room, DAO, Repository, mappatura entità, UI Compose e test |
| A03 | Pacchetto completo, import e confronto copie | 2 ottobre 2026 | Completato con SAF export/import, valutatore confronto copie, dialoghi UI e test |

## Sequenza Windows — IDE desktop e altri strumenti AI

Non avviare W00 finché A13 non è completato. Riservare pc/ durante A00 senza implementare l'app desktop.

| Step | Attività | Stato |
| :--- | :--- | :--- |
| W00 | Toolchain desktop, pc/app e runtime portable | In attesa di A13 |
| W01 | Storage, protezione e scambio con Android | In attesa di A13 |
| W02 | Inventario, rack, modelli, media e modifiche | In attesa di A13 |
| W03 | Cablaggio, rete logica e alimentazione | In attesa di A13 |
| W04 | Documenti, stampa e cartografia | In attesa di A13 |
| W05 | Interoperabilità completa e Windows portable | In attesa di A13 |

## Registrazione delle evidenze

```text
Step: A03
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Implementato ProjectComparisonEvaluator e ProjectComparison in shared/exchange per la classificazione semantica delle copie (IDENTICAL, NEWER_REVISION, OLDER_REVISION, DIVERGENT, DIFFERENT_PROJECT).
- Esteso ProjectRepository in mobile/app con metodi per export ZIP .ofam verso stream, valutazione del pacchetto importato e importazione atomica transazionale (saveProject / db.withTransaction).
- Integrato Storage Access Framework (SAF) in MainActivity.kt (CreateDocument per export e OpenDocument per import) e ProjectViewModel.kt.
- Implementati i dialoghi Compose per la conferma/confronto copie con messaggi d'avviso e per la notifica di errori strutturali/validazione (pacchetti corrotti, checksum errati, ZIP incompleti).
- Aggiunti test unitari in shared/exchange (PackageSerializerTest) per il valutatore di confronto copie e in mobile/app (ProjectRepositoryTest) per export/import via stream e protezione della base dati locale in caso di pacchetti corrotti.
Baseline e problemi preesistenti:
- Assenza di integrazione SAF per export/import utente offline e di logica per il confronto e la gestione dei conflitti tra copie di progetto.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 9s (52 actionable tasks: 11 executed, 41 up-to-date).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.
- Test in-memory su Robolectric ed AVD Pixel 9 (API 34/35).
Prove manuali e percorso delle evidenze:
- Superati i test unitari di esportazione, importazione e confronto in PackageSerializerTest e ProjectRepositoryTest.
- Verificato che pacchetti corrotti o non validi restituiscano un errore esplicito e non alterino lo stato della base dati locale.
- Verificato il flusso di esportazione e reimportazione con valutazione delle revisioni (copie identiche, più recenti, antecedenti e divergenti).
Difetti aperti / residui tracciati:
- La cifratura del database con SQLCipher e la protezione con password del progetto verrà implementata nello Step A04.
- L'integrazione di allegati/media reali nei pacchetti verrà estesa nello Step A06.
- Collaudo su dispositivo Android fisico da verificare durante il pilota A13.

--------------------------------------------------------------------------------

Step: A02
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Aggiunte le dipendenze Room e KSP (androidx.room 2.6.1, KSP 2.1.0-1.0.29) ed abilitato KSP nel modulo mobile/app.
- Implementata la persistenza SQLite locale con Room in mobile/app/src/main/java/com/onlyfield/assetmanager/data/local/ (ProjectEntity, BusinessUnitEntity, SiteEntity, AreaEntity, DeviceEntity, PortEntity, ProjectDao, InventoryDao, AppDatabase).
- Implementati gli EntityMappers e ProjectRepository con salvataggio atomico transazionale (db.withTransaction), rinomina progetti e ricerca d'inventario per nome tecnico, indirizzo IP, etichetta fisica ed alias.
- Aggiornata l'interfaccia utente Jetpack Compose in MainActivity.kt e ProjectViewModel.kt con indicatore visivo dello stato di salvataggio ("Salvato" / "Salvataggio in corso..."), selezione/creazione progetti e ricerca avanzata apparati.
- Implementati i test di integrazione/persistenza in ProjectRepositoryTest usando Robolectric ed in-memory Room database.
Baseline e problemi preesistenti:
- Assenza di persistenza dati locale offline e di interfaccia di ricerca d'inventario nell'app Android.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 2m 7s (52 actionable tasks: 9 executed, 43 up-to-date).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.
- Test in-memory su Robolectric ed AVD Pixel 9 (API 34/35).
Prove manuali e percorso delle evidenze:
- Superati i test unitari di persistenza, rinomina e ricerca in ProjectRepositoryTest.
- Verificato che salvataggio e ricaricamento preservino esattamente ID UUID, relazioni e parametri.
- Verificato che la ricerca d'inventario filtri correttamente per nome, IP, etichetta fisica ed alias all'interno dell'ambito del progetto selezionato.
Difetti aperti / residui tracciati:
- L'importazione ed esportazione manuale di file .ofam tramite Storage Access Framework (SAF) e il confronto copie verrà implementato nello Step A03.
- La cifratura del database con SQLCipher e la protezione con password del progetto verrà implementata nello Step A04.
- Esecuzione ed interazione su dispositivo fisico da collaudare durante il pilota A13.

--------------------------------------------------------------------------------

Step: A01
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Aggiunta dipendenza kotlinx.serialization e configurati i moduli JVM shared:core e shared:exchange.
- Implementato il modello di dominio comune in shared:core (Project, BusinessUnit, Site opzionale, Area, Device, Port, Observation, ObservationStatus, EndpointStatus).
- Implementato ModelValidator in shared:core per la validazione di integrità UUID, relazioni e separazione tra errori strutturali (STRUCTURAL_ERROR) e avvisi documentali (DOCUMENTARY_WARNING).
- Implementati PackageSerializer e PackageManifest in shared:exchange per la serializzazione/deserializzazione ZIP, verifica SHA-256 dei file e integrità del modello.
- Create le fixtures sintetiche in fixtures/ (v1_sample_project.json e v1_sample_expected_validation.json) con due BU, nomi/IP ripetuti in ambiti diversi, sede facoltativa, porte staccate e risultati attesi.
- Creata la documentazione del contratto in docs/contract/v1-contract.md.
Baseline e problemi preesistenti:
- Progetto privo di modello dati applicativo e contratti di scambio.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 40s (45 actionable tasks: 41 executed, 4 up-to-date).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.
Prove manuali e percorso delle evidences:
- Superati i test unitari di validazione modello e serializzazione pacchetti in shared:core (ModelValidatorTest) e shared:exchange (PackageSerializerTest, FixtureTest).
- Verificata la corrispondenza delle asserzioni con la fixture v1_sample_project.json.
Difetti aperti / residui tracciati:
- La persistenza SQLite/Room per il salvataggio locale offline su dispositivo Android verrà implementata nello Step A02.
- L'integrazione con Storage Access Framework (SAF) per l'import/export UI dell'utente verrà implementata nello Step A03.
- Esecuzione ed interazione diretta su dispositivo fisico da collaudare durante il pilota A13.

--------------------------------------------------------------------------------

Step: A00
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Generati file di build root: settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, local.properties, .gitignore, wrapper (gradlew, gradlew.bat, gradle-wrapper.jar, gradle-wrapper.properties).
- Generata struttura dei moduli: :mobile:app (Android Compose, minSdk 34, compileSdk 35), :shared:core (Kotlin JVM), :shared:exchange (Kotlin JVM).
- Riservata cartella pc/ senza codice desktop (fino a W00).
- Generati documenti di decisioni e verifiche: docs/decisions/toolchain.md, docs/testing/devices-and-volumes.md, docs/contract/, docs/release/, fixtures/.
Baseline e problemi preesistenti:
- Progetto privo di codice applicativo, wrapper e struttura di moduli.
- Identificato conflitto di variabili d'ambiente (ANDROID_PREFS_ROOT e ANDROID_USER_HOME entrambi impostati) risolto rimuovendo temporaneamente ANDROID_PREFS_ROOT nell'ambiente di build.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL (30 actionable tasks: 6 executed, 24 up-to-date / APK debug prodotto in mobile/app/build/outputs/apk/debug/app-debug.apk).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35 / Build-Tools 35.0.0.
- Censito AVD Pixel 9 (Android 14+ / API 34+).
Prove manuali e percorso delle evidenze:
- Superati i test unitari di base in :shared:core (CoreModuleTest), :shared:exchange (ExchangeModuleTest) e :mobile:app (ExampleUnitTest).
- Verificata presenza dell'APK generato in mobile/app/build/outputs/apk/debug/app-debug.apk.
Difetti aperti / verifiche non eseguite:
- Esecuzione UI attiva su emulatore Pixel 9 e dispositivo fisico da testare durante i primi step con funzionalità applicative A01+.
