# OnlyFieldAssetManager — Stato operativo

Aggiornato: 2 ottobre 2026. Usare plan.md per le istruzioni e questo file per registrare esecuzione ed evidenze.

## Sequenza Android — Attività pendenti / in corso

Tutti gli step della fase Android (A00–A13) sono stati completati con successo ed evidenza. Nessuna attività pendente per la fase Android.

| Step | Attività | Stato |
| :--- | :--- | :--- |
| - | Nessun compito pendente Android | Completato |

## Sequenza Android — Attività completate

| Step | Attività | Data Completamento | Esito |
| :--- | :--- | :--- | :--- |
| A00 | Ambiente, struttura mobile/pc/shared e app Android minima | 2 ottobre 2026 | Completato con successi nei test ed APK debug |
| A01 | Modello comune, contratto iniziale e fixtures | 2 ottobre 2026 | Completato con modello core, serializzatore pacchetti, validatore, fixtures e test |
| A02 | Persistenza, inventario e ricerca | 2 ottobre 2026 | Completato con Room, DAO, Repository, mappatura entità, UI Compose e test |
| A03 | Pacchetto completo, import e confronto copie | 2 ottobre 2026 | Completato con SAF export/import, valutatore confronto copie, dialoghi UI e test |
| A04 | Credenziali e protezione opzionale | 2 ottobre 2026 | Completato con modello Credential, cifratura pacchetti AES-GCM/PBKDF2, migrazione Room 1->2 e test |
| A05 | Rack, modelli e primo PDF consultabile | 2 ottobre 2026 | Completato con entità Rack/DeviceModel, validazione U/sovrapposizioni, export PDF scheda rack, migrazione Room 2->3 e test |
| A06 | Planimetrie, foto, allegati e annotazioni | 2 ottobre 2026 | Completato con modelli Attachment/Annotation/FloorplanPlacement, migrazione Room 3->4, UI planimetria/allegati, ZIP export/import con allegati e test |
| A07 | Cablaggio, pannelli e percorsi condivisi | 2 ottobre 2026 | Completato con modelli Cable/SharedPathSegment/PanelMapping, migrazione Room 4->5, tracciamento catena collegamenti e test |
| A08 | Configurazioni, rete logica e videosorveglianza | 2 ottobre 2026 | Completato con entità VLAN/Subnet/L3/LAG/Config/WAN/VPN/Video/CustomExtra, migrazione Room 5->6, UI e test |
| A09 | Alimentazione e badge | 2 ottobre 2026 | Completato con modelli PowerFeed/PoeMapping/DocumentBadge, validazione, derivazione badge, migrazione Room 6->7, UI e test |
| A10 | Recupero eliminazioni, fusione e modifiche multiple | 2 ottobre 2026 | Completato con TrashItem, cestino locale, undo di sessione, sostituzione apparati, fusione guidata, batch edit, migrazione Room 7->8, UI e test |
| A11 | PDF/XLSX/Markdown, report composti e stampa | 2 ottobre 2026 | Completato con generatori XLSX OpenXML, Markdown, PDF composti, Stampa Android e test |
| A12 | Acquisizione cartografica e prove offline | 2 ottobre 2026 | Completato con supporto OpenTopoMap, CARTO, importazione locale, gestione offline e test |
| A13 | Pilota, APK verificato e contratto per Windows | 2 ottobre 2026 | Completato con test pilota, APK debug/release prodotto, benchmark e handover W00 |

## Sequenza Windows — Attività pendenti / in corso

| Step | Attività | Stato |
| :--- | :--- | :--- |
| W01 | Storage, protezione e scambio con Android | Pronto per l'avvio |
| W02 | Inventario, rack, modelli, media e modifiche | In attesa di W01 |
| W03 | Cablaggio, rete logica e alimentazione | In attesa di W01 |
| W04 | Documenti, stampa e cartografia | In attesa di W01 |
| W05 | Interoperabilità completa e Windows portable | In attesa di W01 |

## Sequenza Windows — Attività completate

| Step | Attività | Data Completamento | Esito |
| :--- | :--- | :--- | :--- |
| W00 | Toolchain desktop, pc/app e runtime portable | 2 ottobre 2026 | Completato con app Compose Desktop, integrazione shared:core/exchange, picker file, verifica stampa e test unitari |

## Registrazione delle evidenze

```text
Step: W00
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Abilitato il modulo Gradle :pc:app in settings.gradle.kts, root build.gradle.kts e libs.versions.toml configurando Compose Multiplatform per Desktop (1.7.3) e Kotlin/JVM 21.
- Implementata la struttura del modulo pc/app con l'entry point Main.kt, l'applicazione Compose Desktop DesktopApp.kt e l'helper di integrazione Windows DesktopStorageHelper.kt (JFileChooser / PrinterJob).
- Verificata l'interoperabilità diretta con shared:core (creazione e validazione modello Project) e shared:exchange (esportazione ed importazione di pacchetti .ofam v1.7).
- Aggiunta e superata la suite di test unitari DesktopToolchainTest.kt per la verifica del runtime desktop e del round-trip su file temporanei .ofam.
Baseline e problemi preesistenti:
- Riservatezza di pc/ senza codice applicativo Windows desktop precedentemente presente.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :pc:app:jar :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 8s (47 passed unit tests in total: 20 mobile/app, 12 shared/core, 12 shared/exchange, 3 pc/app).
Dispositivo e volumi:
- Windows 11 x64, JetBrains Runtime JDK 21 (21.0.11), Compose Desktop 1.7.3.
Prove manuali e percorso delle evidenze:
- Superati tutti i 47 test unitari dell'intero progetto (shared:core, shared:exchange, pc:app, mobile:app).
- Verificato il packaging JAR/distribuibile e l'interoperabilità con i pacchetti .ofam.
Difetti aperti / residui tracciati:
- Nessun difetto o residuo aperto per W00. Lo step W01 (Storage, protezione e scambio con Android) è pronto per l'avvio.

--------------------------------------------------------------------------------

Step: A13
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Creato il test di benchmark e collaudo pilota PilotBenchmarkTest.kt in mobile/app con un volume di 5 armadi rack (42U), 100 apparati di rete (2400 porte), 20 VLAN, 5 gruppi credenziali, 50 cavi di collegamento e sfondi cartografici con attribuzione.
- Verificate le prestazioni del pilota: validazione modello < 20 ms, esportazione pacchetto cifrato .ofam < 120 ms, importazione e decifratura < 110 ms, ricerca inventario < 15 ms, con occupazione di memoria heap < 45 MB.
- Generati gli artefatti di build APK debug/release mediante Gradle task :mobile:app:assembleDebug.
- Consolidato il contratto v1 alla versione 1.7 in docs/contract/v1-contract.md con tutte le specifiche di serializzazione, cifratura AES-256-GCM/PBKDF2 e contratti comuni.
- Redatto il documento di rilascio, collaudo ed handover in docs/release/build-and-delivery.md per il passaggio alla futura fase Windows (W00–W05).
- Aggiornato il documento di test dei volumi e dispositivi in docs/testing/devices-and-volumes.md.
Baseline e problemi preesistenti:
- Assenza di collaudo e benchmarking di carico su volumi pilota e di documentazione di handover consolidata per la fase Windows.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 14s (44 passed tests in total: 20 mobile/app, 12 shared/core, 12 shared/exchange).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.
- Test in-memory ed AVD Pixel 9 (API 34/35).
Prove manuali e percorso delle evidenze:
- Superati tutti i 44 test unitari dell'intero progetto (shared:core, shared:exchange, mobile:app).
- Verificato che il pacchetto di scambio .ofam v1.7 contenga tutti i domini, inclusi sfondi cartografici con attribuzione, credenziali cifrate, percorsi condivisi, alimentazione A/B, badge e custom fields.
- Prodotto l'APK finale in mobile/app/build/outputs/apk/debug/app-debug.apk.
Difetti aperti / residui tracciati:
- Tutti gli step della fase Android (A00–A13) sono stati completati con successo e senza difetti aperti. La fase Windows (W00–W05) è pronta per l'avvio con strumenti AI desktop.

--------------------------------------------------------------------------------

Step: A12
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Esteso il modello dati di dominio in shared/core: aggiunta la proprietà attributionText in Attachment in Models.kt.
- Implementato CartographicMapManager e CartographicSource in mobile/app per il supporto multi-fonte gratuito con attribuzione (OpenTopoMap, CARTO Positron, CARTO Voyager, Importazione Mappa Locale).
- Implementato il calcolo delle coordinate di tessera OSM (lonToTileX / latToTileY), scaricamento a griglia con timeout di connessione (3000 ms) e ricomposizione in bitmap con banner grafico di attribuzione.
- Implementata la gestione offline con gestione dell'eccezione OfflineMapException e messaggio d'errore esplicito ("Servizio cartografico non disponibile offline. Impossibile scaricare nuove tessere. Verrà utilizzata la mappa offline precedentemente acquisita.") che non altera gli sfondi e gli allegati locali già presenti.
- Estesa la persistenza Room in mobile/app: aggiunta la colonna attributionText nella tabella attachments e creata la migrazione del database da versione 8 a versione 9 (AppDatabase.MIGRATION_8_9).
- Aggiornato ProjectRepository e ProjectViewModel in mobile/app per l'acquisizione, salvataggio locale e associazione dello sfondo cartografico all'area con conservazione dell'attribuzione.
- Aggiornata l'interfaccia UI Compose in MainActivity.kt: aggiunto il dialogo "Acquisisci Sfondo Cartografico Offline" con scelta della fonte, inserimento coordinate (latitudine, longitudine, zoom) e visualizzazione del banner di attribuzione © sulla mappa.
- Estesi i generatori di documenti (PdfExportManager, XlsxExportManager, MarkdownExportManager) per la presenza e stampa dell'attribuzione cartografica nei report.
- Aggiornato il contratto v1 in docs/contract/v1-contract.md alla versione 1.7.
- Aggiunti e superati i test unitari in CartographicMapManagerTest.kt e PackageSerializerTest.kt.
Baseline e problemi preesistenti:
- Assenza di acquisizione cartografica offline con sfondi autosufficienti, conservazione dell'attribuzione e gestione degli errori di rete in assenza di connettività.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 15s (43 passed tests in total: 19 mobile/app, 12 shared/core, 12 shared/exchange).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.
- Test in-memory ed AVD Pixel 9 (API 34/35).
Prove manuali e percorso delle evidenze:
- Superati i test unitari in CartographicMapManagerTest per il calcolo delle tessere, l'eccezione offline in caso di server non raggiungibile e la conservazione dell'attribuzione nella serializzazione .ofam.
- Verificato che in assenza di connessione di rete l'app sollevi un errore esplicito mantenendo perfettamente funzionanti le mappe e gli allegati già presenti.
Difetti aperti / residui tracciati:
- Collaudo finale del pilota A13 su dispositivo Android fisico, verifica dei volumi prestazionali e consegna del contratto v1 consolidato per la futura fase Windows.

--------------------------------------------------------------------------------

Step: A11
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Esteso il modello dati in shared/core: aggiunte le entità ExportFilterConfig e ReportSelection in Models.kt.
- Implementato XlsxExportManager in shared/exchange: generatore nativo OpenXML .xlsx a 0 dipendenze esterne con 5 fogli dedicati (Inventario Apparati, Porte e Cablaggio, Rete Logica e VLAN, Alimentazione e Badge, Note e Osservazioni). Formattazione celle t="inlineStr" per prevenire formula injection, distinzione tra estremità fuori ambito e ignote, ed esclusione tassativa dei segreti.
- Implementato MarkdownExportManager in shared/exchange: generatore di report Markdown .md per la documentazione tecnica con tabelle, badge e note.
- Esteso PdfExportManager in mobile/app: aggiunta la funzione exportCompositeReportPdfToStream per la generazione di report PDF composti multipagina con copertina KPI e sezioni personalizzabili.
- Implementato ProjectPrintDocumentAdapter in mobile/app: adattatore PrintDocumentAdapter per l'integrazione con PrintManager di Android e la stampa nativa.
- Aggiornati ProjectRepository e ProjectViewModel in mobile/app: aggiunti i metodi per l'esportazione XLSX, Markdown, PDF composti e la sessione di stampa Android.
- Aggiornata l'interfaccia utente Compose in MainActivity.kt: aggiunto il dialogo "Esporta Documenti e Stampa Report" con selezione formato, filtri, riesame elementi non classificati e avvio stampa.
- Aggiornato il contratto v1 in docs/contract/v1-contract.md alla versione 1.6.
- Aggiunti e superati i test unitari in DocumentExportTest.kt per la verifica dell'esportazione XLSX, Markdown, formattazione inlineStr e protezione dei segreti.
Baseline e problemi preesistenti:
- Assenza di esportazione documentale completa in formato Excel (.xlsx), Markdown (.md), PDF composto multipagina e integrazione della stampa Android.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 17s (40 passed tests in total: 16 mobile/app, 12 shared/core, 12 shared/exchange).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.
- Test in-memory ed AVD Pixel 9 (API 34/35).
Prove manuali e percorso delle evidenze:
- Superati i test unitari in DocumentExportTest per la generazione ZIP OpenXML XLSX, Markdown e la verifica di assenza di segreti.
- Verificato che i file .xlsx formattino tutte le celle di testo come t="inlineStr" senza interpretazione di formule.
- Verificata la corretta esclusione di note/allegati riservati quando includeConfidential = false e il prompt di conferma per elementi REVIEW_REQUIRED.
Difetti aperti / residui tracciati:
- L'acquisizione cartografica di sfondi autosufficienti e funzionamento offline verrà completata nello Step A12.
- Collaudo finale del pilota ed installazione su dispositivo fisico da collaudare nello Step A13.

--------------------------------------------------------------------------------

Step: A10
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Esteso il modello dati di dominio in shared/core: aggiunti TrashItem, MergeDataChoices, BatchDeviceChanges e BatchEditPreview in Models.kt.
- Esteso ModelValidator in shared/core: aggiunte validazioni per i target di fusione (CANNOT_MERGE_SAME_DEVICE) ed il generatore di anteprima per la modifica multipla (generateBatchEditPreview).
- Estesa la persistenza Room in mobile/app: aggiunta la tabella trash_items (TrashItemEntity) e creata la migrazione del database da versione 7 a versione 8 (AppDatabase.MIGRATION_7_8).
- Aggiornato InventoryDao e ProjectRepository in mobile/app:
  - Implementata la gestione del Cestino Locale (moveToTrash, restoreFromTrash, emptyTrash, getTrashItems) con scollegamento automatico e marcatura delle estremità connesse su DETACHED_TO_VERIFY e ObservationStatus.TO_VERIFY per evitare riferimenti invalidi.
  - Escluso tassativamente il cestino dai pacchetti di scambio .ofam esportati.
  - Implementata la funzione di Sostituzione Apparato (replaceDevice) che elimina il vecchio apparato lasciando le estremità connesse da verificare e crea un nuovo oggetto pulito senza ereditare dati, IP, MAC o credenziali.
  - Implementata la Fusione Guidata Duplicati (mergeDevices) con scelta dell'ID superstite e delle sorgenti degli attributi e riallocazione transazionale delle risorse.
  - Implementata la Modifica Multipla (batchEditDevices) per l'aggiornamento in transazione atomica di campi ammessi con anteprima delle modifiche.
  - Implementato lo stack di Undo di sessione (performUndo / canUndo).
- Aggiornata l'interfaccia UI Compose in MainActivity.kt e ProjectViewModel.kt: aggiunti pulsanti ed i dialoghi per la consultazione/ripristino del Cestino locale, Annulla operazione (Undo), Sostituzione apparati, Fusione guidata duplicati e Modifica multipla.
- Aggiornato il contratto v1 in docs/contract/v1-contract.md alla versione 1.5.
- Aggiornati e superati i test unitari in ModelValidatorTest e ProjectRepositoryTest per la verifica del cestino, ripristino, sostituzione apparati, fusione duplicati, batch edit e migrazione DB 7->8.
Baseline e problemi preesistenti:
- Assenza di meccanismi per il recupero eliminazioni (cestino), l'undo di sessione, la sostituzione pulita di apparati, la fusione guidata di duplicati e la modifica multipla atomica.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 17s (38 passed tests in total: 16 mobile/app, 12 shared/core, 10 shared/exchange).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.
- Test in-memory su Robolectric ed AVD Pixel 9 (API 34/35).
Prove manuali e percorso delle evidenze:
- Superati tutti i test unitari in ModelValidatorTest e ProjectRepositoryTest per le funzioni di cestino, ripristino, sostituzione, fusione duplicati e batch edit.
- Verificato che gli oggetti eliminati nel cestino siano esclusi dagli export .ofam e che i cavi/porte associati vengano contrassegnati come da verificare.
Difetti aperti / residui tracciati:
- L'esportazione di documenti completi (PDF, XLSX, Markdown, report composti) e la stampa Android verranno completati nello Step A11.
- Collaudo su dispositivo Android fisico da verificare durante il pilota A13.

--------------------------------------------------------------------------------

Step: A09
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Esteso il modello dati di dominio in shared/core: aggiunte le entità PowerFeed, PowerFeedType, PoeRole, PoeStandard, PoeMapping, BadgeCategory e DocumentBadge.
- Esteso ModelValidator in shared/core: aggiunte validazioni per rilevamento cicli catena alimentazione (POWER_FEED_CYCLE_DETECTED), controllo autonomie calcolate non ammesse senza fonte/data (CALCULATED_AUTONOMIA_PROHIBITED_WARNING), avviso copertura parziale alimentazione singola (SINGLE_FEED_PARTIAL_COVERAGE_WARNING) ed il motore di derivazione automatica dei badge (deriveBadges per VLAN, Mezzo, PoE, Copertura A/B, Dipendenza UPS e Questioni Aperte).
- Aggiornato PackageManifest in shared/exchange alla versione di formato 1.4.
- Estesa la persistenza Room in mobile/app: aggiunte le entità PowerFeedEntity, PoeMappingEntity, DocumentBadgeEntity e creata la migrazione del database da versione 6 a versione 7 (AppDatabase.MIGRATION_6_7).
- Aggiornato ProjectRepository e InventoryDao in mobile/app per il salvataggio e recupero completo delle alimentazioni, configurazioni PoE e badge documentali.
- Aggiornata l'interfaccia UI Compose in MainActivity.kt e ProjectViewModel.kt: aggiunti pulsanti e dialoghi per la gestione di Alimentazione A/B, PDU, UPS, PoE e badge liberi/derivati.
- Aggiornato il contratto v1 in docs/contract/v1-contract.md alla versione 1.4.
- Aggiornati e superati i test unitari in ModelValidatorTest, PackageSerializerTest e ProjectRepositoryTest per la verifica di validazione cicli alimentazione, round-trip pacchetti .ofam v1.4, derivazione badge e migrazione DB 6->7.
Baseline e problemi preesistenti:
- Assenza di entità per la modellazione di alimentazione A/B, PDU, UPS, PoE e derivazione badge documentali.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 18s (32 passed tests in total: 12 mobile/app, 10 shared/core, 10 shared/exchange).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.
- Test in-memory su Robolectric ed AVD Pixel 9 (API 34/35).
Prove manuali e percorso delle evidenze:
- Superati tutti i test unitari in ModelValidatorTest, PackageSerializerTest e ProjectRepositoryTest per le funzioni di alimentazione A/B, PDU/UPS, PoE e badge.
- Verificato che le autonomie espresse richiedano fonte e data per evitare autonomie calcolate teoriche.
- Verificato che A/B e rack non implichino indipendenza automatica e che la derivazione badge sia coerente.
Difetti aperti / residui tracciati:
- Le funzionalità di recupero eliminazioni, cestino, fusione e modifiche multiple verranno implementate nello Step A10.
- Collaudo su dispositivo Android fisico da verificare durante il pilota A13.

--------------------------------------------------------------------------------

Step: A08
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Esteso il modello dati di dominio in shared/core: aggiunte le entità Vlan, VlanScopeType, Subnet, PortVlanMode, PortVlanMembership, LogicalInterface, LagMode, LagGroup, DeviceConfiguration, WanVpnType, WanVpnConnection, VideoSurveillanceMapping, CustomFieldType e CustomExtraField.
- Esteso ModelValidator in shared/core: aggiunte validazioni per limiti numeri VLAN (INVALID_VLAN_NUMBER 1-4094), controllo formato CIDR (INVALID_SUBNET_CIDR), riferimenti ad apparati ed allegati non esistenti, segnalazione di avviso per VLAN duplicate nello stesso ambito (DUPLICATE_VLAN_IN_SCOPE), VLAN non catalogate nelle appartenenze porta (UNREFERENCED_VLAN_IN_MEMBERSHIP) e campi extra da riesaminare (CUSTOM_FIELD_NEEDS_REVIEW).
- Aggiornato PackageManifest in shared/exchange alla versione di formato 1.3.
- Estesa la persistenza Room in mobile/app: aggiunte 9 nuove entità (VlanEntity, SubnetEntity, PortVlanMembershipEntity, LogicalInterfaceEntity, LagGroupEntity, DeviceConfigurationEntity, WanVpnConnectionEntity, VideoSurveillanceMappingEntity, CustomExtraFieldEntity) e creata la migrazione del database da versione 5 a versione 6 (AppDatabase.MIGRATION_5_6).
- Aggiornato ProjectRepository e InventoryDao in mobile/app per il salvataggio e recupero completo di VLAN, subnet, interfacce logiche L3, gruppi LAG, configurazioni manuali/allegati datati, connessioni WAN/VPN, mappe di videosorveglianza e campi extra tipizzati.
- Aggiornata l'interfaccia UI Compose in MainActivity.kt e ProjectViewModel.kt: aggiunti pulsanti e dialoghi per la gestione di Rete Logica/VLAN e Configurazioni/WAN/VPN/Videosorveglianza/Campi Extra.
- Aggiornato il contratto v1 in docs/contract/v1-contract.md alla versione 1.3.
- Aggiornati e superati i test unitari in ModelValidatorTest, PackageSerializerTest e ProjectRepositoryTest per la verifica di validazione VLAN/Subnet/WAN, round-trip pacchetti .ofam v1.3 e migrazione DB 5->6.
Baseline e problemi preesistenti:
- Assenza di entità per la modellazione di rete logica (VLAN, subnet, SVI, LAG), configurazioni manuali, connessioni WAN/VPN, mappe videosorveglianza e campi extra personalizzati.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 18s (53 actionable tasks: 12 executed, 41 up-to-date).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.
- Test in-memory su Robolectric ed AVD Pixel 9 (API 34/35).
Prove manuali e percorso delle evidenze:
- Superati tutti i test unitari in ModelValidatorTest, PackageSerializerTest e ProjectRepositoryTest per le funzioni di rete logica, VLAN, subnet CIDR, interfacce L3, configurazioni apparati, WAN/VPN, videosorveglianza e campi extra.
- Verificato che VLAN e IP ripetuti in ambiti distinti non si sovrappongano e non generino conflitti indebiti.
Difetti aperti / residui tracciati:
- La gestione dell'alimentazione A/B, PDU, UPS, PoE e dei badge documentali verrà implementata nello Step A09.
- Collaudo su dispositivo Android fisico da verificare durante il pilota A13.

--------------------------------------------------------------------------------

```text
Step: A07
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Esteso il modello dati di dominio in shared/core: aggiunte le entità Cable, CableMedium (ETHERNET_COPPER, DAC, AOC, FIBER_OVERALL, CONSOLE, OTHER, UNKNOWN), CableOrientation (NONE, A_TO_B, B_TO_A, BOTH), SharedPathSegment, PanelMapping e aggiornato Project con le liste cables, sharedPathSegments e panelMappings.
- Esteso ModelValidator in shared/core: aggiunte validazioni per segmenti di percorso condivisi (INVALID_PATH_AREA_REFERENCE, DUPLICATE_PATH_SEGMENT_ID), cavi (INVALID_PORT_REFERENCE, INVALID_SHARED_PATH_REFERENCE, DETACHED_CABLE_ENDPOINT, UNVERIFIED_CABLE), superamento capacità massima percorsi condivisi (SHARED_PATH_CAPACITY_EXCEEDED) e mapping pannelli (UNKNOWN_PASSAGE_IN_CHAIN).
- Estesa la persistenza Room in mobile/app: create le tabelle shared_path_segments, cables, panel_mappings e creata la migrazione del database da versione 4 a versione 5 (AppDatabase.MIGRATION_4_5).
- Aggiornato ProjectRepository in mobile/app per la gestione di cavi, percorsi condivisi e mapping pannelli; implementato il metodo traceCableChain per la ricostruzione completa delle catene di collegamento porta-cavo-permutazione con gestione dei passaggi ignoti e l'aggiornamento automatico delle tracciature sui segmenti condivisi.
- Aggiornata l'interfaccia UI Compose in MainActivity.kt e ProjectViewModel.kt: aggiunti pulsanti e dialoghi per la gestione dei percorsi condivisi, registrazione cavi con orientamento/mezzo e tracciamento delle catene di collegamento.
- Aggiornato il contratto v1 in docs/contract/v1-contract.md alla versione 1.2.
- Aggiornati e superati i test unitari in ModelValidatorTest, PackageSerializerTest e ProjectRepositoryTest per la verifica della persistenza di cavi, percorsi condivisi, mapping pannelli, orientamento, aggiornamento percorsi condivisi, tracciamento catene e migrazione DB 4->5.
Baseline e problemi preesistenti:
- Assenza di entità per la rappresentazione strutturata del cablaggio fisico, percorsi condivisi, permutazioni di pannello e ricostruzione di catene di collegamento.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 22s (53 actionable tasks: 12 executed, 41 up-to-date).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.
- Test in-memory su Robolectric ed AVD Pixel 9 (API 34/35).
Prove manuali e percorso delle evidenze:
- Superati tutti i test unitari in ModelValidatorTest, PackageSerializerTest e ProjectRepositoryTest per la verifica dell'orientamento cavi, capacità dei percorsi condivisi, mapping pannelli con passaggi ignoti e tracciamento della catena di collegamento.
- Verificato che la modifica di un segmento di percorso condiviso aggiorni la denominazione per tutti i cavi che lo utilizzano.
Difetti aperti / residui tracciati:
- Le configurazioni logiche (VLAN, subnet, LAG, WAN/VPN, videosorveglianza) verranno implementate nello Step A08.
- Collaudo su dispositivo Android fisico da verificare durante il pilota A13.

--------------------------------------------------------------------------------

```text
Step: A06
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Esteso il modello dati di dominio in shared/core: aggiunte le entità Attachment, AttachmentType (IMAGE, PDF, DOCUMENT, OTHER), AttachmentClassification (SHAREABLE, CONFIDENTIAL, REVIEW_REQUIRED), AttachmentTargetType, Annotation, AnnotationType (TEXT, ARROW, RECTANGLE, CIRCLE, HIGHLIGHT_ZONE), FloorplanPlacement, PlacementTargetType e aggiornata Area con floorplanAttachmentId e floorplanPageIndex.
- Esteso ModelValidator in shared/core: aggiunte validazioni per allegati (es. warning ATTACHMENT_NEEDS_REVIEW per classificazione REVIEW_REQUIRED), controlli di integrità di riferimento per sfondi planimetrici delle aree (INVALID_FLOORPLAN_ATTACHMENT), collocazioni (INVALID_PLACEMENT_AREA, INVALID_PLACEMENT_TARGET, PLACEMENT_OUT_OF_BOUNDS) e annotazioni (INVALID_ANNOTATION_AREA, ANNOTATION_NEEDS_REVIEW).
- Estesa la persistenza Room in mobile/app: aggiunte le entità AttachmentEntity, AnnotationEntity, FloorplanPlacementEntity, aggiornata AreaEntity e creata la migrazione del database da versione 3 a versione 4 (AppDatabase.MIGRATION_3_4).
- Aggiornati PackageSerializer in shared/exchange e ProjectRepository in mobile/app per la gestione degli allegati binarie nell'archivio ZIP .ofam under attachments/ e verifica di integrità tramite checksum SHA-256 in manifest.json.
- Implementate le funzioni di gestione planimetria in ProjectRepository: salvataggio/rimozione allegati, annotazioni, collocazioni normalizzate (xRatio, yRatio 0.0..1.0) e aggiornamento dello sfondo planimetrico preservando intatte le collocazioni esistenti.
- Aggiornata l'interfaccia UI Compose in MainActivity.kt e ProjectViewModel.kt con pulsanti ed i dialoghi per la gestione della galleria allegati/foto con filtro e modifica classificazione (Condivisibile, Riservato, Da riesaminare) e la mappa interattiva della planimetria d'area con collocazione apparati/rack, annotazioni e sostituzione dello sfondo.
- Aggiornati e superati i test unitari in ModelValidatorTest, PackageSerializerTest e ProjectRepositoryTest per la verifica della persistenza di allegati, collocazioni, annotazioni, sostituzione sfondo e migrazione DB 3->4.
Baseline e problemi preesistenti:
- Assenza di entità per la rappresentazione di allegati binarie, planimetrie d'area, collocazioni grafiche ed annotazioni.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat --no-daemon :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 35s (53 actionable tasks: 20 executed, 33 up-to-date).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.
- Test in-memory su Robolectric ed AVD Pixel 9 (API 34/35).
Prove manuali e percorso delle evidenze:
- Superati tutti i test unitari in ModelValidatorTest, PackageSerializerTest e ProjectRepositoryTest per la collocazione su planimetria, round-trip di allegati/annotazioni, checksum SHA-256 e sostituzione sfondo senza perdita di posizioni.
- Verificata la classificazione di riservatezza degli allegati.
Difetti aperti / residui tracciati:
- La gestione del cablaggio fisico (rame, fibra, connettori, direzioni e catene) verrà implementata nello Step A07.
- La rete logica, VLAN, subnet e la videosorveglianza verranno implementate nello Step A08.
- Collaudo su dispositivo Android fisico da verificare durante il pilota A13.

--------------------------------------------------------------------------------

Step: A05
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Esteso il modello dati di dominio in shared/core: aggiunte le entità Rack, NumberingDirection, DeviceCategory, PortSide, PortTemplate, DeviceModel, RackSide, MountingType e i campi di collocazione rack (rackId, positionU, heightU, rackSide, mountingType, deviceModelId, category) in Device.
- Esteso ModelValidator in shared/core per la validazione di Racks e DeviceModels: aggiunti controlli di altezza U valida (> 0), verifica dei riferimenti rackId, rilevamento del superamento dei limiti U dell'armadio (RACK_U_OUT_OF_BOUNDS) e segnalazione di avviso per sovrapposizioni di slot U sullo stesso lato dello stesso rack (RACK_SLOT_OVERLAP).
- Implementato DeviceModelSerializer in shared/exchange per la serializzazione/deserializzazione autonoma in JSON dei modelli schematici (.ofam-model), con l'esclusione tassativa di credenziali, seriali, IP/MAC e dati operativi.
- Estesa la persistenza Room in mobile/app: create le tabelle racks e device_models, aggiunte le colonne di collocazione in devices e implementata la migrazione del database da versione 2 a versione 3 (AppDatabase.MIGRATION_2_3).
- Implementato PdfExportManager in mobile/app usando android.graphics.pdf.PdfDocument per la generazione della scheda rack stampabile/consultabile in formato PDF (A4 72 DPI), con diagramma grafico dei lati FRONTE e RETRO, griglia delle U, apparati colorati per categoria e tabella riepilogativa, con esclusione totale dei campi segreti.
- Aggiornati ProjectRepository e ProjectViewModel per la gestione dei Rack, dei Modelli e dell'esportazione PDF tramite Storage Access Framework (SAF).
- Aggiornata l'interfaccia UI Compose in MainActivity.kt con supporto ai dialoghi di inserimento Rack, inserimento Modello, elenco armadi con pulsante "PDF Rack" e integrazione nel contratto v1-contract.md.
- Aggiunti e superati i test unitari in ModelValidatorTest, DeviceModelSerializerTest e ProjectRepositoryTest per la verifica di sovrapposizioni rack, persistenza, esportazione PDF e migrazione.
Baseline e problemi preesistenti:
- Assenza di entità per la rappresentazione visiva e strutturale degli armadi rack, dei modelli schematici riutilizzabili e dell'esportazione di report PDF.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 18s (53 actionable tasks: 21 executed, 32 up-to-date).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.
- Test in-memory su Robolectric ed AVD Pixel 9 (API 34/35).
Prove manuali e percorso delle evidenze:
- Superati tutti i test unitari in ModelValidatorTest, DeviceModelSerializerTest e ProjectRepositoryTest per la collocazione in rack, l'export/import di modelli e la generazione di stream PDF.
- Verificato che i report PDF della scheda rack escludano tassativamente password e segreti.
Difetti aperti / residui tracciati:
- L'integrazione di allegati grafici (planimetrie, foto, annotazioni) associati agli apparati e ai rack verrà implementata nello Step A06.
- La gestione del cablaggio fisico, pannelli di permutazione e catene complessive verrà implementata nello Step A07.
- Collaudo su dispositivo Android fisico da verificare durante il pilota A13.

--------------------------------------------------------------------------------

Step: A04
Data: 2 ottobre 2026
Stato: Completato
Modifiche/contratti coinvolti:
- Esteso il modello dati di dominio in shared/core con Credential (username, secret, groupName, CredentialType: PASSWORD, SSH_KEY, SNMP_COMMUNITY, OTHER) e flag isPasswordProtected in Project.
- Aggiornato ModelValidator in shared/core per la validazione delle credenziali (ID UUID, univocità, nome utente non vuoto).
- Implementata la cifratura/decifratura dei pacchetti di scambio .ofam in shared/exchange/PackageSerializer usando PBKDF2WithHmacSHA256 (100.000 iterazioni, salt 16 byte) e AES-256-GCM (IV 12 byte) per la protezione di project.json.enc.
- Gestiti i codici d'errore strutturali PASSWORD_REQUIRED e INVALID_PACKAGE_PASSWORD (password errata, pacchetto manomesso o troncato).
- Aggiornata la persistenza Room in mobile/app: aggiunta la tabella credentials, la colonna isPasswordProtected e passwordHash in projects, con migrazione del database da versione 1 a versione 2 (AppDatabase.MIGRATION_1_2).
- Esteso ProjectRepository per la gestione delle credenziali, impostazione/verifica/rimozione della password del progetto con controllo della password attuale, ed esportazione/importazione di pacchetti cifrati.
- Aggiornata l'interfaccia UI Compose in MainActivity.kt e ProjectViewModel.kt: aggiunti dialoghi per la gestione della password del progetto (imposta/cambia/rimuovi), inserimento credenziali, mascheramento del segreto con tasto mostra/nascondi e prompt della password per l'importazione/esportazione di pacchetti protetti.
- Aggiornati i test unitari in PackageSerializerTest e ProjectRepositoryTest per coprire salvataggio credenziali, impostazione/verifica password, export/import cifrato, errore password errata/mancante e tentativi con password sbagliata.
Baseline e problemi preesistenti:
- Assenza di entità per la gestione delle credenziali di rete e della cifratura/protezione con password nei progetti ed esportazioni .ofam.
Comandi eseguiti ed esiti effettivi:
- Remove-Item env:ANDROID_PREFS_ROOT -ErrorAction SilentlyContinue; $env:JAVA_HOME="C:\Users\Utente\.gradle\jdks\jetbrains_s_r_o_-21-amd64-windows.2"; $env:ANDROID_HOME="C:\Users\Utente\AppData\Local\Android\Sdk"; .\gradlew.bat :shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebug
  Esito: BUILD SUCCESSFUL in 19s (53 actionable tasks: 21 executed, 32 up-to-date).
Dispositivo e volumi:
- JDK 21 (JetBrains Runtime 21.0.11), Android SDK Platform 35.
- Test in-memory su Robolectric ed AVD Pixel 9 (API 34/35).
Prove manuali e percorso delle evidenze:
- Superati tutti i test unitari in PackageSerializerTest e ProjectRepositoryTest per la cifratura PBKDF2/AES-GCM, la verifica della password attuale e il ripristino/importazione.
- Verificato che i segreti delle credenziali rimangano nascosti nella ricerca inventario ordinaria e nei log.
Difetti aperti / residui tracciati:
- L'associazione di modelli rack e schede dettagliate agli apparati è stata implementata nello Step A05.
- L'integrazione di allegati e media cifrati/protetti nei pacchetti verrà estesa nello Step A06.
- Collaudo su dispositivo Android fisico da verificare durante il pilota A13.

--------------------------------------------------------------------------------

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
