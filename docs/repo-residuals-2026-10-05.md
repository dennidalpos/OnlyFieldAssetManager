# Residui del repository — 5 ottobre 2026

Revisione conclusa il 6 ottobre 2026; il nome del report conserva la data di avvio.

Revisione del checkout `82fe4e2`, successiva all’audit e alle correzioni registrate in [audit-2026-10-05.md](audit-2026-10-05.md). L’audit ha identificato **11 nuovi rilievi** e **4 collaudi/pulizie preesistenti**, inizialmente **4 P1, 9 P2, 2 P3**. Nessun P0 identificato. Aggiornamento 6 ottobre: AUD-12/13/14/15/16/17/18/19/20/21 chiusi, AUD-23 chiuso per scelta del limite prudenziale; AUD-26 chiuso con politica confermata; 8 attività aperte/parziali (6 P2, 2 P3), compresi AUD-24, AUD-25 e nuovo AUD-27. I dettagli descrivono lo stato rilevato; chiusure successive in [roadmap](../roadmap.md), lavoro ancora aperto nel [tracker](../PROJECT_STATUS.json).

## Perimetro, fonti e limiti

Inventario completo dei 325 file versionati: 255 sorgenti Kotlin, di cui 152 applicativi; quattro moduli Gradle e configuratore condiviso. Esaminati contratti e decisioni, struttura/build/CI/rilascio, dominio e modifiche, persistenza e mapper, scambio/cifratura/staging, import/fusione, media, documenti, navigazione e principali ingressi UI, test, fixture e risorse. Ricerche trasversali di segnaposto, compatibilità legacy, eccezioni, I/O e chiamanti; approfondimento dei confini di salvataggio e dei punti sospetti. Non è una certificazione di ogni percorso né di ogni riga.

I risultati distinguono **riprodotto** con dati sintetici, **da codice** e **collaudo non eseguito**. Un riferimento statico non dimostra che il difetto sia stato osservato sul telefono. Non sono stati installati APK, avviate applicazioni native, stampati documenti, modificati progetti utente o rimossi output delle sessioni precedenti. La modifica ad `AGENTS.md` presente all’inizio è preservata.

Fonti primarie consultate durante la revisione:

- [Android: coroutine, thread principale, errori e cancellazione](https://developer.android.com/kotlin/coroutines/coroutines-best-practices), pagina aggiornata il 24 settembre 2026: base per AUD-18/20/21.
- [Room: transazioni e risultati coerenti di query separate](https://developer.android.com/reference/androidx/room/Transaction): base per il rischio di letture miste di AUD-15. L’applicazione al caricamento manuale di `ProjectStore` è una deduzione dal codice, non una riproduzione.
- [Room: query asincrone](https://developer.android.com/training/data-storage/room/async-queries): una query `suspend` non trasferisce automaticamente al worker le copie di file che la seguono.
- [Java 21: operazioni `Files` e `ATOMIC_MOVE`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html): l’atomicità del singolo movimento di file non copre l’insieme media/progetto/base di AUD-14.

Le fonti esterne chiariscono le proprietà delle API; comportamento e priorità derivano dal checkout. Versioni delle dipendenze considerate come configurazione effettiva, senza proporre aggiornamenti generalizzati.

## Ordine di lavoro

P1: integrità dei dati o blocco del flusso operativo; intervenire prima dell’uso operativo. P2: correttezza dei documenti, completezza dei flussi, prestazioni e collaudi da pianificare. P3: pulizia e semplificazione a impatto minore. Ordine iniziale: **AUD-12 → AUD-13 → AUD-14 → AUD-15**, poi P2 rispettando `dependsOn`. I collaudi manuali non vengono chiusi dalle prove JVM.

| Priorità | ID | Attività | Evidenza |
| --- | --- | --- | --- |
| P1 | AUD-12 | Confronto con la copia locale effettiva prima dell’import | Chiuso 6 ottobre; evidenza iniziale conservata |
| P1 | AUD-13 | Limiti e rollback di tutti gli ingressi media | Chiuso 6 ottobre; evidenza iniziale conservata |
| P1 | AUD-14 | Atomicità di import/fusione, media e base | Chiuso 6 ottobre; rollback verificato |
| P1 | AUD-15 | Modifiche, undo e letture coerenti Android | Chiuso 6 ottobre; ordinamento e letture verificati |
| P2 | AUD-16 | Sostituzione/fusione Android tramite regole condivise | Chiuso 6 ottobre; parità e rollback verificati |
| P2 | AUD-17 | Incertezze e stati di rilievo nei documenti | Chiuso 6 ottobre; regressioni JVM e PDF nativi |
| P2 | AUD-18 | Annullamento e proprietà dei pacchetti durante import Android | Chiuso 6 ottobre; lettura/KDF e proprietà verificate |
| P2 | AUD-19 | Media eliminati esclusi dall’export e pulizia dei file di proprietà | Chiuso 6 ottobre; hash, raccolta e rollback verificati |
| P2 | AUD-20 | I/O residuo e disponibilità media senza decodifica sulla UI | Chiuso 6 ottobre |
| P2 | AUD-21 | Errori e cancellazione nei comandi scoperti | Chiuso 6 ottobre; callback, rollback e preferenze verificati |
| P2 | AUD-23 | Capacità esatta Android protetta al limite ZIP | Chiuso 6 ottobre per decisione: mantenere limite prudenziale |
| P2 | RES-13 | Collaudo hardware reale | Aperto, conservato |
| P2 | RES-19 | Matrice Android/accessibilità e nuovi flussi | Parziale, conservata |
| P2 | RES-23 | Comandi/focus Windows ed errore nativo di stampa | Parziale, conservato |
| P2 | AUD-24 | Recupero dopo arresto improvviso o rollback fallito | Da codice; aperto |
| P2 | AUD-25 | Opzioni di fusione dei dati associati | Ricerca dei chiamanti; aperto |
| P2 | AUD-26 | Ripristino senza contesto originale o con tipo non supportato | Chiuso 6 ottobre; politica, credenziali e rifiuti verificati |
| P2 | AUD-27 | Riferimenti secondari durante ripristino | Da codice; non riprodotti su persistenza |
| P3 | AUD-22 | Scaffolding, helper di test nel runtime e commenti obsoleti | Ricerca dei chiamanti |
| P3 | RES-24 | Risorse locali delle prove precedenti | Directory ancora presenti; nessuna rimozione tentata |

## P1: integrità e continuità del lavoro

### AUD-12 — Il confronto usa il progetto aperto, non sempre la copia ricevuta

**Chiuso il 6 ottobre 2026.** Confronto per ID incoming su entrambe le app; copia Windows chiusa protetta sbloccata prima della Review, sostituzione esplicita e fusione con protezione locale conservata. Regressioni permanenti `LocalImportComparisonTest` e `RepositoryDispatchTest`; 350 test JVM senza fallimenti. Collaudo Android nativo nella matrice RES-19.

**Riprodotto su Windows.** Creare una copia locale, chiuderla, importare un pacchetto dello stesso ID con nome e data precedenti. `DesktopAppState.importFile` non apre `AppDialog.Compare`: salva direttamente l’incoming sulla copia locale. La presenza di un progetto aperto diverso non risolve il problema: il confronto usa quell’altro progetto.

**Android, da codice.** `PackageExchange.evaluateImportPackage` carica `currentProjectId ?: pkg.project.id`. Se A è aperto e arriva B, il confronto non considera B locale. `ImportDialogs` usa `currentProjectId != null` per `sameProjectExists`, proponendo Fusione anche con `DIFFERENT_PROJECT`; `startMerge` cerca invece la copia dell’ID incoming.

Riferimenti: [DesktopAppState.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopAppState.kt), `importFile`; [PackageExchange.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/PackageExchange.kt), `evaluateImportPackage`; [AppRoot.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/ui/AppRoot.kt), `ImportDialogs`.

Chiudere con prove su nessun progetto aperto/A aperto/B aperto, file precedente, conferma e annullamento. La fusione richiede identità uguali.

### AUD-13 — Un allegato rifiutato resta nei media e impedisce i salvataggi successivi

**Chiuso il 6 ottobre 2026.** Ingressi media con limiti prima del commit e rollback di file/cache/catalogo; fotocamera Android continua dopo persistenza riuscita. Regressioni con 33 MiB e I/O fallita su entrambe le app; commit Android marcato prima del rientro cancellabile. Verifica ordinaria: 359 test senza errori. Budget conservativo al limite protetto separato in AUD-23.

**Riprodotto su Windows.** Il picker generico passa a `addAttachment`, che deposita il file con `storeAttachmentFile` prima di chiamare `update`. Un file sintetico da **33 MiB** supera il limite di 32 MiB: il catalogo resta invariato, ma il payload rimane nello storage. Il successivo rinomina del progetto fallisce con `Salvataggio automatico non riuscito: Pacchetto oltre i limiti di import oppure parametri crittografici non validi.` Il ramo dei media nel configuratore ha un rollback che il picker generico non usa.

**Android, da codice.** `addAttachment`, `saveMediaEdit` e foto non verificano gli stessi limiti prima di registrare il media; il serializer li rifiuta soltanto al successivo export. `storeAttachmentFile` Windows legge inoltre l’intero sorgente con `readBytes` prima di verificarne la dimensione. Non è stato provocato un OOM.

Riferimenti: [DesktopAppState.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopAppState.kt), `addAttachment/saveMapEdit`; [DesktopStorageManager.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopStorageManager.kt), `storeAttachmentFile`; [ProjectViewModel.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/ui/ProjectViewModel.kt), ingressi media.

Chiudere con limiti approvati verificati prima del commit, rollback di file/cache/catalogo ed edit successivo riuscito, anche con password ed errore I/O. Nessun nuovo limite o formato proposto.

### AUD-14 — Import e fusione non hanno un esito unitario

**Aggiornamento 6 ottobre.** Includere anche `open/prepareMedia` Windows e la fusione della copia chiusa: i file non protetti esistenti possono essere sostituiti prima del salvataggio del progetto. La prevenzione AUD-13 riguarda nuovi media, non questo flusso.

**Da codice, non riprodotto.** Android estrae gli allegati prima di salvare progetto e verificatore, poi aggiorna la base separatamente. Se una fase successiva fallisce, i vecchi dati possono puntare a byte già sostituiti. L’estrazione protegge il singolo file incompleto, non ripristina l’intero import.

Windows `applyMerge` estrae i media, invoca `update`, che può restituire dopo avere assorbito un errore di persistenza, e chiama comunque `rememberSyncBase`. Il recupero non deve considerare scambiato un progetto che non è stato salvato. La selezione dei payload deve inoltre rispettare l’esito della fusione.

Riferimenti: [PackageExchange.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/PackageExchange.kt), `importProjectPackage/importMerged`; [DesktopAppState.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopAppState.kt), `applyMerge/update`; [AttachmentFiles.kt](../shared/exchange/src/main/java/com/onlyfield/assetmanager/exchange/AttachmentFiles.kt), `extract`.

Chiudere con errori iniettati in estrazione/salvataggio/base, confrontando catalogo, byte, cestino e base prima/dopo. Le prove Android di questo audit non sono partite; nessun rollback viene dichiarato riprodotto.

### AUD-15 — Snapshot Android non coordinati

**Da codice, non riprodotto.** `edit` pubblica lo snapshot prima di salvare, ogni comando lancia una coroutine e il catch ripristina `before` senza controllare la versione corrente. Undo salva uno snapshot intero, anche se nel frattempo il progetto è cambiato o è stato chiuso. Cestino, sostituzione e password percorrono altri read-modify-write. La barra `busy` di `AppScaffold` è un indicatore, non una barriera agli input.

`ProjectStore.load` legge progetto e collezioni con molte query separate senza un’unica transazione di lettura. Un salvataggio completo tra due query può produrre una vista mista pur avendo una transazione di scrittura corretta. La serializzazione delle transazioni Room non risolve da sola snapshot preparati precedentemente o undo obsoleti.

Riferimenti: [ProjectViewModel.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/ui/ProjectViewModel.kt), `edit/restoreSnapshot/reload`; [ProjectStore.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/ProjectStore.kt), `load/save`; [Components.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/ui/components/Components.kt), `AppScaffold`.

Chiudere con sequenze deterministiche di edit ravvicinati, errore del primo save, undo dopo altri edit, edit più cestino/password e chiusura/cambio progetto. Letture ed export devono riflettere una sola versione.

## P2: flussi e documenti

### AUD-16 — Operazioni Android duplicate e divergenti

`TrashOperations.replaceDevice` conserva `areaId/rackId` ma lascia ai valori predefiniti U, altezza e montaggio. `ProjectEdits.replaceDevice`, usato da Windows, li conserva. Inoltre cancellazione del vecchio apparato e inserimento del nuovo non sono un’unica transazione. `mergeDevices` duplica a sua volta selezione dei campi e creazione delle porte.

Riferimenti: [TrashOperations.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/TrashOperations.kt) e [ProjectEdits.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/edit/ProjectEdits.kt). Da codice: la prova di parità Android non è stata eseguita. Obiettivo: regole condivise, transazione unica con il cestino, parità su rack/contenimenti/porte e rimozione della logica sostituita.

### AUD-17 — Assenza di rilievo trasformata in “Verificato”

**Riprodotto nel Markdown.** Un `Device` senza `Observation` produce una riga con stato “Verificato”. Anche il conteggio delle criticità esclude il dato assente. `DeviceForm.from` presenta invece lo stesso apparato come “Da verificare”; lasciare quel valore senza modifiche può conservare `observation=null`.

Lo stesso fallback a `VERIFIED` compare in XLSX e PDF Android. L’inventario PDF Desktop non riporta `Observation`. Verificare anche gli avvisi pertinenti al documento: paginazione e numero di sezioni non ne provano la presenza.

Riferimenti: [MarkdownExportManager.kt](../shared/exchange/src/main/java/com/onlyfield/assetmanager/exchange/MarkdownExportManager.kt), [XlsxExportManager.kt](../shared/exchange/src/main/java/com/onlyfield/assetmanager/exchange/XlsxExportManager.kt), [PdfExportManager.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/export/PdfExportManager.kt), [ReportContent.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/report/ReportContent.kt), [EntityForms.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/forms/EntityForms.kt). Chiudere con regola uniforme e regressioni per assenza, verifica esplicita e conflitto in tutti i formati.

### AUD-18 — Annullamento import Android senza annullamento del lavoro

**Chiuso il 6 ottobre.** Review tardiva riprodotta sul sorgente precedente e corretta con Job/identità e trasferimento della proprietà. Otto regressioni permanenti includono lettura, PBKDF2 reale, ritorno del worker, doppio import, chiusura e conferma/fusione. Il testo seguente conserva il rilievo iniziale; comandi ed evidenze nella roadmap.

`startImport` non conserva il `Job`; `cancelImport` pulisce soltanto lo stato già pubblicato. Un worker ancora in lettura può tornare dopo l’annullamento o dopo un nuovo import, sostituire Review e perdere la proprietà del pacchetto precedente. `onCleared` e la consegna del risultato hanno un ulteriore confine di proprietà; la conferma non imposta `busy`.

Da codice, nessuna race provocata. Riferimenti: [ProjectViewModel.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/ui/ProjectViewModel.kt), `startImport/cancelImport/onCleared/confirmImport`, e [PackageExchange.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/PackageExchange.kt). Chiudere con doppio import, annullamento in lettura/KDF, chiusura ViewModel e controllo dello staging.

### AUD-19 — Media rimossi ancora esportati

**Chiuso il 6 ottobre.** Export attivo distinto dalla working copy con media di undo/cestino; raccolta di proprietà e rollback su guasto, metadati del cestino conservati durante sostituzione e scadenza delle 50 revisioni Windows verificata. Diciassette prove media nuove e suite completa di 406 test verde, APK compilato; ultima verifica mirata di 40 test verde. Il testo seguente conserva la riproduzione iniziale. AUD-26 ha chiuso sito originale e tipi non supportati; AUD-27 conserva i riferimenti secondari. Recupero da arresto ancora in AUD-24.

**Riprodotto su Windows.** Aggiungere un piccolo allegato, rimuoverlo tramite `ProjectEdits.deleteAttachment`, esportare il progetto. Il catalogo è vuoto, ma la entry canonica e i suoi byte rimangono nel pacchetto: `exportPackageToFile` usa tutto `localMedia`. Non occorre modificare direttamente il JSON.

Android `deleteProject` rimuove righe e base, ma non i file sotto `attachments/<projectId>`. Rimozione degli allegati e undo richiedono una politica comune dei file attivi, recuperabili e definitivamente rimossi. Non cancellare indiscriminatamente i media necessari al cestino o all’undo; questi non devono però entrare nello scambio come payload orfani.

Riferimenti: [DesktopStorageManager.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopStorageManager.kt), `localMedia/exportPackageToFile`; [ProjectEdits.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/edit/ProjectEdits.kt), `deleteAttachment`; [ProjectRepository.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/ProjectRepository.kt), `deleteProject`. Chiudere confrontando payload dell’export, recuperabilità locale e pulizia delle sole directory di proprietà.

### AUD-20 — I/O residuo sul Main e letture pesanti nella composizione

**Chiuso il 6 ottobre.** Query/copie del picker e scrittura cartografica Android spostate sul worker con AUD-13. Su Windows disponibilità tramite catalogo/esistenza del file, senza lettura/decifratura; anteprime di editor, piano e selettore PDF ricevono un loader eseguito sul worker. Rimosso il vecchio overload che riceveva byte già caricati dalla composizione. Tre regressioni Compose/storage passate; verifica conclusiva 363 test senza errori. Il testo seguente conserva il rilievo iniziale.

Android `addAttachment` esegue query e copia dello stream su `viewModelScope`; soltanto il conteggio PDF usa `Dispatchers.IO`. `downloadMap` usa un worker per la rete ma scrive il file dopo il rientro sul Main. Su Windows l’elenco media chiama `attachmentBytes(att)` durante la composizione per stabilire se il file è disponibile; questa API carica/decifra il contenuto, anziché controllarne l’esistenza.

Da codice; ANR, blocco UI e picchi heap non misurati. Riferimenti: [ProjectViewModel.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/ui/ProjectViewModel.kt), [FloorplanMediaSection.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/ui/FloorplanMediaSection.kt) e [DesktopStorageManager.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopStorageManager.kt). Chiudere con verifiche del thread e misure ai limiti, evitando di materializzare media per un controllo di disponibilità.

### AUD-21 — Errori e cancellazione non gestiti uniformemente

**Chiuso il 6 ottobre 2026.** Callback password Android completa e vincolata alla sessione; cancellazione rilanciata. Errori apertura/cestino, riprova e rollback verificati. Preferenze Windows con errori visibili, commit prima del cambio tema/lingua, proprietà e file precedenti conservati. Suite completa 415 test e APK; evidenze nella roadmap. Descrizione seguente storica.

`openProject`, `renameProject`, `changePassword`, `deleteFromTrash`, `emptyTrash` Android non gestiscono gli errori del repository dentro la coroutine. Altri catch generici, compresi edit e cestino, non rilanciano la cancellazione. Windows ignora gli errori di lettura impostazioni e salvataggio tema. Non tutti i comandi hanno le garanzie già verificate per import/export.

Da codice; crash e disco pieno non provocati. Riferimenti: [ProjectViewModel.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/ui/ProjectViewModel.kt) e [DesktopAppState.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopAppState.kt), `toggleDarkTheme/loadSettings`. Chiudere con errori mirati, stato coerente, messaggio utile e cancellazione propagata.

## P3: legacy e pulizia

### AUD-22 — Scaffolding e API di test nel runtime

Ricerca dei riferimenti nell’intero repository:

- `CoreModule` e `ExchangeModule` sono usati solo dai rispettivi test di nome/dipendenza. `ExampleUnitTest` verifica `2 + 2`; `ExampleInstrumentedTest` verifica il nome del package.
- `DesktopStorageManager.loadAndroidFixtureFile` è chiamato solo da `DesktopStorageTest`; `cleanTempFolder` non ha chiamanti e ignora gli errori di rimozione.
- `ProjectRepository.saveAttachment/updateAreaFloorplan/searchInventory/traceCableChain/batchEditDevices` sono chiamati solo dai test. La UI utilizza già modifiche o ricerca condivise: verificarne la necessità e spostare/rimuovere solo ciò che non serve.
- Commenti `BU_ID`, contratto 1.8/1.7 in `Models` e lettori 1.11 in `DevicePresets` non descrivono più il contratto greenfield corrente.

Riferimenti: [CoreModule.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/CoreModule.kt), [ExchangeModule.kt](../shared/exchange/src/main/java/com/onlyfield/assetmanager/exchange/ExchangeModule.kt), [DesktopStorageManager.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopStorageManager.kt), [ProjectRepository.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/ProjectRepository.kt), [Models.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/model/Models.kt), [DevicePresets.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/forms/DevicePresets.kt).

**Compatibilità utile da conservare:** schema storico SQLCipher e fixture di versioni rifiutate sono regressioni attive; alias degli allegati e verificatori password legacy sono ancora supportati e testati. La sola parola “legacy” non giustifica la loro eliminazione. Nessuna dipendenza dichiarata viene rimossa in questo audit.

### RES-24 e collaudi preesistenti

Confermata la presenza locale di `build/import-benchmark`, `build/native-window-app`, `build/native-window-exports` e `build/task-verification`. Conservare `build/reports` e `build/phone-backup`. I precedenti rifiuti della pulizia automatica restano evidenze storiche: questa sessione non ha ritentato quei comandi né verificato i lock dell’emulatore. RES-24 mantiene stato e criteri precedenti.

RES-13, RES-19 e RES-23 mantengono ID, prove già svolte e parti non eseguite. Dipendenze aggiunte per ripetere i collaudi interessati dopo le correzioni. Nessun test JVM sostituisce scanner/foto/USB fisici, TalkBack, matrice Android o errore nativo di stampa.

## Verifiche e riproduzioni

Baseline prima dell’aggiornamento del tracker:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest --no-parallel --max-workers=1
```

Esito: `BUILD SUCCESSFUL in 33s`, una task eseguita (Exchange) e 42 aggiornate; Core, Desktop e Android JVM hanno riutilizzato i risultati esistenti. Non sono dichiarate nuove esecuzioni per le task `UP-TO-DATE`.

Prove temporanee con sorgenti sotto `build/repo-audit`, inclusi con init Gradle senza modificare i sorgenti versionati:

| Prova che asserisce il comportamento corretto | Esito | Rilievo |
| --- | --- | --- |
| `incomingMustCompareAgainstTheClosedLocalCopy` | Nessun `AppDialog.Compare`, incoming già salvato | AUD-12 |
| `rejectedOversizeAttachmentMustLeaveTheProjectEditable` | Anche il rinomina dopo il rifiuto fallisce per il limite del payload | AUD-13 |
| `deviceWithoutObservationMustNotBeReportedAsVerified` | Riga Markdown contiene “Verificato” senza rilievo | AUD-17 |
| `outgoingPackageMustNotIncludeDeletedAttachmentPayload` | Entry del media eliminato ancora presente nell’export | AUD-19 |

Quattro assertion fallite: **difetti riprodotti**, non regressioni introdotte dalla modifica documentale. La prima esecuzione ha rieseguito le suite Exchange/Desktop (69 e 133 prove preesistenti superate, più tre prove dell’audit fallite); la successiva mirata Desktop ha eseguito tre prove, tutte fallite, inclusa quella aggiunta sull’export dei media eliminati. JUnit e riepilogo conservati localmente in `build/reports/repo-audit-2026-10-05`; scenari e risultati sono qui documentati anche per un nuovo checkout.

I due tentativi di includere le quattro prove Android sono terminati con `No tests found for given includes: [*RepoAudit*]` e poi `[*RepoAuditAndroidTest*]`. L’ipotesi è l’acquisizione dei source set Kotlin di AGP prima dell’init `projectsEvaluated`. Tentativo interrotto dopo il secondo errore, senza aggirare i controlli. Le quattro prove Android non sono state compilate/eseguite e non costituiscono evidenza di riproduzione.

I sorgenti temporanei e l’init sono rimossi dopo aver conservato i soli report. La verifica finale ordinaria e i controlli documentali sono registrati in [roadmap](../roadmap.md); nessun controllo esistente viene modificato per rendere verdi i rilievi.

## AUD-23 — Capacità esatta Android protetta vicino al limite ZIP

**Chiuso per decisione il 6 ottobre 2026.** Confermato il budget prudenziale senza password aggiuntiva, con possibile rifiuto vicino al limite. Requisito aggiornato in plan.md/contratto/workflow; nessuna verifica esatta a 256 MiB dichiarata. Le quattro regressioni MediaCapacityTest passano nella suite AUD-21.

Il budget preventivo di `MediaCapacity` usa dimensioni effettive e un limite superiore per GCM, compressione e header ZIP, senza conservare la password. Il margine evita commit non esportabili ma può rifiutare un’aggiunta ancora ammissibile. `protectedPreflightKeepsAConservativeMarginNearTheArchiveLimit` lo dimostra con limite ridotto e un pacchetto cifrato reale; la prova reale a 256 MiB non è stata eseguita. La verifica esatta proposta inizialmente è stata esclusa dal requisito corrente per decisione esplicita. Fonti del budget: [zlib compressBound](https://github.com/madler/zlib/blob/master/compress.c), [specifica ZIP PKWARE](https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT), consultate il 6 ottobre 2026.

## Chiusure AUD-14/15 e nuovo AUD-24 — 6 ottobre

I paragrafi precedenti conservano i rilievi iniziali. AUD-14 chiuso con import/fusione reversibili e progetto/base coerenti su guasto; AUD-15 chiuso con comandi Android ordinati, undo protetto e letture Room transazionali. Evidenze e comandi nella roadmap. AUD-18 e AUD-21 sono stati chiusi successivamente con proprietà/cancellazione import, callback password e preferenze Windows verificati. Nessun collaudo nativo implicito.

AUD-24 P2: il rollback è gestito nel processo, senza journal durevole per un arresto tra file e database/base. Restano prove di arresto/riavvio e secondo guasto durante rollback; gli errori di pulizia dopo commit sono segnalati, senza recupero guidato delle risorse. Riferimenti: ReversibleFiles, PackageExchange, DesktopStorageManager. Nessuna protezione aggiuntiva da crash viene dichiarata verificata.

## Aggiornamento AUD-16 — 6 ottobre 2026

AUD-16 chiuso: regole condivise, transazione unica e regressioni di parità/rollback, evidenze in roadmap e documentazione di verifica. Restano 12 attività (10 P2, 2 P3) dopo l'aggiunta di AUD-25.

### AUD-25 — Opzioni di fusione dei dati associati senza implementazione

Ricerca dei chiamanti: mergeCredentials, mergeConfigurations, mergePowerFeeds e mergeExtraFields compaiono soltanto nella dichiarazione di MergeDataChoices; ProjectEdits.mergeDevices non le applica. Definire il trasferimento dei record associati, i conflitti e il recupero prima di cambiare la semantica. Riferimenti: [Models.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/model/Models.kt) e [ProjectEdits.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/edit/ProjectEdits.kt). P2, da codice; non riprodotto come flusso UI.

## Chiusura AUD-17 — 6 ottobre 2026

Regola condivisa per dato assente e stati espliciti; note e avvisi filtrati nei documenti, senza credenziali. Sette prove native PDF passano su Pixel 9 API 37 dopo aver corretto l'omissione dello stato nella lista rack; suite JVM finale 381 test verdi. Le evidenze iniziali sopra restano storiche. Tracker: 11 attività aperte/parziali (9 P2, 2 P3), prossimo AUD-18; AUD-25 conserva le opzioni di fusione senza implementazione. Nessuna chiusura dei collaudi manuali.

## AUD-26 — Ripristino senza contesto originale — 6 ottobre 2026

**Chiuso il 6 ottobre 2026.** Politica confermata e implementata; dettagli e nuovo residuo AUD-27 nella sezione successiva. Il testo seguente conserva il rilievo iniziale.

La nuova prova di sostituzione con un progetto senza siti ha raggiunto restoreTrash senza ricreare l'apparato: NoSuchElementException nell'assertion sulla presenza dell'apparato, nessun errore UI, voce rimossa. Il core restituiva il progetto invariato e i chiamanti proseguivano come dopo un successo. Corretto durante AUD-19: errore localizzato senza siti, transazione Android e stato Windows conservano cestino e media. Le due regressioni mancato ripristino verificano l'errore e tutti i dati precedenti, mentre le prove con sito esistente verificano anche l'apparato ricreato.

Restano da definire la scelta del sito quando quello originale manca ma ne esiste un altro (fallback implicito al primo) e la gestione dei tipi fuori dai rami DEVICE/RACK del core. Il ramo else può ancora restituire il progetto invariato; Android/Windows possono eliminare la voce. ATTACHMENT è presente nelle fixture storiche, senza ingresso runtime individuato; Android gestisce CREDENTIAL nella propria facade, il core Desktop no. Riferimenti: [ProjectEdits.restoreFromTrash](../shared/core/src/main/java/com/onlyfield/assetmanager/core/edit/ProjectEdits.kt), [facade Android](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/TrashOperations.kt), [stato Windows](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopAppState.kt). Politica e criteri di chiusura in PROJECT_STATUS.json; nessuna ricollocazione automatica aggiunta in AUD-19.

## Chiusura AUD-26 e nuovo AUD-27 — 6 ottobre 2026

AUD-26 chiuso: sito originale richiesto per gli apparati, contesto sito/piano dei rack conservato; credenziali condivise Android/Windows. Tipo non supportato, ID attivo, progetto/ID serializzato incoerente o voce non presente producono errore senza rimuovere dati/cestino. 68 prove mirate e 425 test completi verdi, APK compilato; esiti e comandi nella roadmap. Decisione utente: bloccare e conservare quando manca il sito originale. Il paragrafo AUD-26 precedente conserva il rilievo iniziale.

AUD-27 P2, da codice: ObjectHierarchy.restore riaggiunge containmentPlacements senza verificare il piano; un DEVICE può mantenere areaId verso un piano rimosso anche con sito originale presente. Gli ID delle porte non sono controllati contro quelli degli apparati attivi; Room insertPorts usa REPLACE. Restano da riprodurre sostituzione/ripristino con stesso sito e piani/porte diversi e da definire rifiuto/recupero senza sovrascrittura o ricollocazione implicita. Le collisioni degli ID delle sole entità DEVICE/RACK/CREDENTIAL sono già bloccate da AUD-26. Riferimenti: ObjectContainment.restore, ProjectEdits.restoreFromTrash, ProjectStore.save e InventoryDao.insertPorts. Nessuna copertura globale dei riferimenti dichiarata.
