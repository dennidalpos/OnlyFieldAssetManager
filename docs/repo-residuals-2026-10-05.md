# Residui del repository — 5 ottobre 2026

Revisione e correzioni aggiornate all’8 ottobre 2026; il nome del report conserva la data di avvio.

Revisione iniziale del checkout `82fe4e2`, successiva all’audit registrato in [audit-2026-10-05.md](audit-2026-10-05.md). L’audit ha identificato 11 nuovi rilievi e 4 collaudi/pulizie preesistenti (inizialmente 4 P1, 9 P2, 2 P3), senza P0. Aggiornamento 8 ottobre: AUD-12–43 chiusi; restano 4 attività aperte/parziali, RES-13/19/23/24 (3 P2, 1 P3). I rilievi iniziali conservano evidenza e contesto storico; chiusure in [roadmap](../roadmap.md), lavoro ancora aperto nel [tracker](../PROJECT_STATUS.json).

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
| P2 | AUD-24 | Recupero dopo arresto improvviso o rollback fallito | Chiuso; journal e prove di riavvio |
| P2 | AUD-25 | Opzioni di fusione dei dati associati | Chiuso; scelte, cestino e file verificati |
| P2 | AUD-26 | Ripristino senza contesto originale o con tipo non supportato | Chiuso 6 ottobre; politica, credenziali e rifiuti verificati |
| P2 | AUD-27 | Riferimenti secondari durante ripristino | Chiuso; contesto e collisioni verificati |
| P2 | AUD-28 | ID tra progetti Android | Chiuso; 24 tabelle e flussi verificati |
| P2 | AUD-29 | Rete di alimentazione al ripristino | Chiuso; rifiuto e riprova verificati |
| P2 | AUD-30 | Cicli con più sorgenti nella validazione/import | Chiuso; controllo completo condiviso |
| P2 | AUD-34 | Picker rapidi Windows fuori mappa | Chiuso; guasto filesystem e riprova verificati |
| P2 | AUD-35 | Bozze mappa Android dopo edit asincrono | Chiuso; sei prove native e sessione/cancellazione |
| P2 | AUD-36 | Editor completi/allegati Windows | Chiuso; otto regressioni dei form, 193 test verdi |
| P2 | AUD-37 | Picker/editor Android fuori mappa | Chiuso; 17 prove native, esiti tardivi e 502 report verdi |
| P2 | AUD-38 | Modelli/Credenziali Windows dopo save fallito | Chiuso; dieci regressioni dialogo/pannello e 203 test verdi; EXE in RES-23 |
| P2 | AUD-39 | Comandi specializzati e credenziali Android | Chiuso; otto prove native, 18 regressioni nuove e 530 report verdi |
| P2 | AUD-40 | Navigazione prima della cancellazione apparato/rack Android | Chiuso; 10 regressioni JVM e due prove native con AppRoot reale |
| P2 | AUD-41 | Serie foto Android e risultati tardivi | Chiuso; nove prove JVM mirate e cinque native con camera sintetica |
| P3 | AUD-22 | Scaffolding, helper di test nel runtime e commenti obsoleti | Chiuso; runtime e prove ripuliti |
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

Riferimenti: `CoreModule` (rimosso in AUD-22), `ExchangeModule` (rimosso in AUD-22), [DesktopStorageManager.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopStorageManager.kt), [ProjectRepository.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/ProjectRepository.kt), [Models.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/model/Models.kt), [DevicePresets.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/forms/DevicePresets.kt).

**Compatibilità utile da conservare:** schema storico SQLCipher e fixture di versioni rifiutate sono regressioni attive; alias degli allegati e verificatori password legacy sono ancora supportati e testati. La sola parola “legacy” non giustifica la loro eliminazione. Nessuna dipendenza dichiarata viene rimossa in questo audit.

### RES-24 e collaudi preesistenti

Confermata la presenza locale di `build/import-benchmark`, `build/native-window-app`, `build/native-window-exports` e `build/task-verification`. Conservare `build/reports` e `build/phone-backup`. I precedenti rifiuti della pulizia automatica restano evidenze storiche: questa sessione non ha ritentato quei comandi né verificato i lock dell’emulatore. RES-24 mantiene stato e criteri precedenti.

RES-13, RES-19 e RES-23 mantengono ID, prove già svolte e parti non eseguite. Dipendenze aggiunte per ripetere i collaudi interessati dopo le correzioni. Nessun test JVM sostituisce fotocamera integrata/gesti reali, TalkBack, matrice Android o errore nativo di stampa. Lettori USB e scanner esterni esclusi dal collaudo per decisione utente del giorno 8 ottobre.

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

## Chiusura AUD-27 e nuovo AUD-28 — 6 ottobre 2026

AUD-27 (6 ottobre): il ripristino richiede i piani originali nella stessa sede, contenitori/figli e montaggi ancora disponibili. Un contenitore spostato su un altro piano o un figlio ricollocato bloccano il ripristino. ID di porte attive, porte duplicate nel JSON e ID di collocazioni già presenti sono rifiutati; nessuna collocazione saltata o sostituita. Progetto, credenziali, base di scambio, cestino e media restano invariati su rifiuto; si può riprovare dopo aver ripristinato il contesto. Le foto delle porte di un apparato nel cestino restano locali anche quando un apparato attivo riusa l’ID della porta e non vengono esportate. Controlli nel progetto corrente; collisioni tra progetti Android tracciate separatamente in AUD-28.

Sette regressioni core e due prove di flusso Android/Windows; il caso delle foto perse su porta riutilizzata è stato corretto nella stessa attività. Suite completa 433 test verdi e APK compilato; estensione finale mirata verde in 31s. Evidenze e perimetro temporale del totale in roadmap. Il rilievo AUD-27 precedente è storico.

AUD-28 P2, da codice: Entities usa ID globali per tabella; ProjectStore cancella il solo progetto corrente, InventoryDao reinserisce con REPLACE. Modello e ripristino vedono solo il progetto corrente: un ID riutilizzato in un altro progetto può sostituirne righe. Import/ripristino e preservazione dell’altro progetto restano da riprodurre su database isolato; nessun recupero globale dichiarato verificato. Riferimenti: [Entities](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/local/Entities.kt), [ProjectStore](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/ProjectStore.kt), [InventoryDao](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/local/InventoryDao.kt).

## Chiusura AUD-22 — 6 ottobre 2026

Scaffolding, quattro prove tautologiche, loader fixture e API di sola prova rimossi dal runtime. Regressioni reali adattate ai percorsi comuni e conservate; compatibilità effettiva e fixture storiche intatte. Cinque chiavi i18n inutilizzate per lingua eliminate. 431 test verdi, APK e APK test compilati; risorsa fixture assente dal JAR principale. Dettagli/evidenze in roadmap e verifica. Il rilievo AUD-22 precedente conserva l’audit iniziale; collegamenti ai simboli eliminati convertiti in riferimenti storici. Nessun residuo tecnico nuovo dalla pulizia; collaudi e AUD-28 restano aperti.

## Chiusura AUD-24 — 6 ottobre 2026

Journal durevole cifrato, recupero automatico e blocco/riprova implementati secondo decisione utente. 448 test verdi e APK/APK test compilati; ultimo controllo mirato 20 prove verdi. Arresti reali dei processi Windows e riapertura di Room persistente verificati; nativo Android SQLCipher/Keystore e focus/messaggi restano nei collaudi RES-13/19/23. Dettagli in roadmap e storage. Il rilievo precedente AUD-24 è storico. AUD-25 confermato e prossimo, AUD-28 resta da riprodurre.

## Chiusura AUD-25 — 6 ottobre 2026

Quattro scelte condivise applicate e visibili nei due dialoghi; dati esclusi nel cestino, conflitti/ID/classificazione e file conservati. Cicli di alimentazione rifiutati con riprova secondo decisione utente. 16 combinazioni, riapertura, export, ripristino e rifiuto verificati; ultimi esiti nella roadmap. Il rilievo iniziale AUD-25 è storico. Nativo UI/focus in RES-19/23; nuovo snapshot locale richiede la versione con AUD-25 per ripristinare i dati associati, senza modifica allo schema Room o .ofam. AUD-28 resta da riprodurre su DB isolato.

## Chiusura AUD-28 e nuovo AUD-29 — 6 ottobre 2026

AUD-28 riprodotto e corretto: le chiavi globali Room sono controllate prima di cancellare l’albero, nella stessa transazione. Nessuna sostituzione di righe dell’altro progetto e nessuna rimappatura implicita. Prove su 24 tabelle, import/sostituzione/fusione/ripristino, collisioni oltre 900 ID e riprova: entrambi i progetti, segreti, media, basi e cestino conservati. 20 prove mirate e 464 test completi verdi; APK e APK test compilati, esiti nella roadmap. Il precedente rilievo AUD-28 è storico.

AUD-29 P2, inizialmente da codice: ripristinare alimentazioni può creare un ciclo dopo aver modificato i collegamenti tra apparati rimasti. Esempio: D→O e N→D nel cestino, poi O→N nel catalogo; il ripristino ricrea D→O→N→D. DeviceTrashData.restore controlla ID e oggetti disponibili, senza verificare il grafo risultante. Non confondere il blocco della fusione AUD-25 con copertura di questo ingresso. Riferimenti: [snapshot associati](../shared/core/src/main/java/com/onlyfield/assetmanager/core/edit/DeviceTrashData.kt), [regole condivise](../shared/core/src/main/java/com/onlyfield/assetmanager/core/edit/ProjectEdits.kt); criterio e livello di evidenza nel tracker.

## Chiusura AUD-29 e nuovo AUD-30 — 7 ottobre 2026

AUD-29 riprodotto e corretto: ripristino rifiutato quando le alimentazioni conservate e quelle nuove formano un ciclo, inclusi rami con più sorgenti. Nessun dato/cestino/media modificato; correzione e riprova riuscite in entrambe le app. 65 prove mirate verdi e APK/APK test compilati; esiti nella roadmap. Il rilievo AUD-29 precedente è storico.

AUD-30 P2, inizialmente da codice: ModelValidator percorre la prima sorgente di ciascun apparato. A→X e A→B, B→Y e B→A nascondono A↔B dietro X/Y, senza che il controllo completo di fusione/ripristino protegga l’import. Da riprodurre con pacchetto sintetico e consolidare con un solo algoritmo. Nessun cambio di formato o dipendenza previsto.

## Chiusura AUD-30 — 7 ottobre 2026

Ciclo nascosto dietro sorgenti alternative riprodotto e corretto. Modello, fusione, ripristino e validazione/import usano un solo controllo iterativo completo; vecchio percorso e relativa chiave i18n eliminati. POWER_FEED_CYCLE_DETECTED resta strutturale e l’import rifiuta il pacchetto, plain o protetto. Grafi profondi, archi ripetuti e ordine delle sorgenti verificati. 40 prove mirate e 470 test completi verdi, APK/APK test compilati; dettagli ed evidenze nella roadmap. Restano soltanto i quattro collaudi/pulizie RES-13/19/23/24.

## AUD-32 — Bozza Windows dopo errore di save — 7 ottobre 2026

Rilievo iniziale da codice, in attesa della riproduzione nativa: DesktopAppState.update intercetta l’errore di persistenza e restituisce Unit; FloorHomeSection chiude comunque il form di nuova sede/piano subito dopo la chiamata. Catalogo e copia persistita sono protetti dal percorso reversibile, ma la bozza può andare persa. Riprodurre con un errore filesystem su storage isolato e verificare i form con lo stesso pattern; chiusura soltanto con campi conservati, errore visibile e riprova riuscita. AUD-32 è P2 nel tracker. Il blocco visivo del worker è un rilievo distinto (AUD-31).

## AUD-31 chiuso — 7 ottobre 2026

Pannello occupato nascosto sotto Nuova sede riprodotto nell’EXE e con 2 regressioni Compose, poi corretto con Dialog sopra i form. 10 prove mirate, 472 nei report completi e collaudo EXE con hash dei 16 allegati invariati. Rimosso dal tracker; matrice Windows ancora RES-23, errore di save/bozza distinto in AUD-32. Evidenze e limiti nella roadmap.

## AUD-33 — Riprova dei picker mappa — 7 ottobre 2026

P2 da lettura del codice, non riprodotto: FloorHomeSection chiude MapObjectPicker prima di chiamare update; la scelta della pagina in PlanChooser azzera selectingPlan/newPlanId anche dopo errore di persistenza. AUD-32 copre i sette form di progetto/sede/area, non questi picker. Verificare guasto filesystem isolato, integrità della copia precedente, conservazione di tipologia/posizione/pagina, errore raggiungibile e riprova. Non cambiare il contratto condiviso Android/Windows senza prima controllare i consumatori.

## AUD-32 chiuso — 7 ottobre 2026

Il rilievo iniziale sopra è storico: save fallito riprodotto nell’EXE e in sette regressioni su progetto/sede/area. Chiusura condizionata al successo e messaggio nel dialogo mappa; bozza conservata e riprova riuscita dopo rilascio del file. Copia/history invariati e undo verificati automaticamente. 16 prove mirate, 479 nei report completi, quattro sedi finali e 16 hash media invariati nell’EXE; dettagli e limiti nella roadmap. AUD-32 rimosso, AUD-33 resta da riprodurre; quattro residui RES-13/19/23/24 ancora aperti.

Checkpoint finale AUD-31/32: app isolata chiusa e nessun processo residuo; i due backup telefono conservano gli SHA-256 iniziali. Il controllo automatico ha respinto la rimozione di build/tmp/res23-20261007 con «blocked by policy», senza motivazione ulteriore. Nessun ritentativo; nuovo scratch incluso in RES-24 con percorso e assenza di reparse point verificati. Evidenze conservate in build/reports.

## AUD-33 chiuso e nuovi AUD-34/35 — 7 ottobre 2026

Il rilievo iniziale AUD-33 è storico: cinque regressioni riproducono perdita del picker mappa dopo save fallito. Chiusura condizionata al successo, errore nel dialogo e riprova dalla selezione conservata; callback di salvataggio invariati. 22 prove mirate e 484 nei report completi verdi, APK/EXE compilati. EXE isolato verifica oggetto/pagina PDF, hash del pacchetto invariati sul guasto e PDF conservato; dettagli e limiti nella roadmap. AUD-33 rimosso dal tracker.

AUD-34 P2, da codice e non riprodotto: ObjectPickerDialog di InventorySection/RackSection e RackUnitPicker chiudono la bozza prima del callback Unit; RackSection aggiorna selectedRackId prima della persistenza. PlanChooser da FloorplanMediaSection chiude dopo il callback senza verificarne l’esito. Verificare nome/tipo/sede/unità/pagina, selezione precedente, copia/history, errore e riprova su storage isolato; controllare i consumatori prima di cambiare i callback.

AUD-35 P2, da codice e non riprodotto: i form nuova sede/piano e i picker oggetto/pagina in FloorHomeScreen chiudono subito dopo vm.edit. Il ViewModel accoda la persistenza e gestisce il guasto dopo che la UI ha già scartato il composable. Distinto dal blocco dei comandi AUD-15 e dal collaudo visuale RES-19; riprodurre con repository isolato, conservare bozze e selezioni fino all’esito, verificare errore/riprova e sessione/cancellazione. Nessun test o collaudo di questi scenari rivendicato.

## AUD-34 chiuso e residuo AUD-36 — 7 ottobre 2026

Il rilievo iniziale AUD-34 è storico: corretti quattro host, 16 prove mirate e 185 test Windows completi verdi. Guasto filesystem reale, bozza/errore/riprova, catalogo/copia/history e undo verificati; il percorso unità usa l’azione accessibile della riga nel layout ristretto del test. Nessun nuovo collaudo EXE. Dettagli nella roadmap; AUD-34 rimosso dal tracker.

AUD-36 P2: gli editor DeviceDialog/RackDialog aperti anche con Aggiungi e modifica, PlaceDeviceDialog, ReplaceDialog, BatchEditDialog e il form allegato conservano il pattern di chiusura anticipata. Evidenza da codice, non riprodotta. Verificare errori storage, tutte le bozze/selezioni, history e riprova prima di chiudere; non estendere automaticamente gli esiti dei picker rapidi a questi flussi.

## AUD-35 chiuso e residuo AUD-37 — 7 ottobre 2026

Il rilievo iniziale AUD-35 è storico: guasto riprodotto in sede/piano/oggetto/PDF nativi, bozze conservate fino all’esito dopo correzione. Sei scenari finali verdi su moto g86 API 36, inclusi immagine/rimozione; Room e media isolati. Quattro regressioni nuove del ViewModel coprono esito/riprova/undo, no-op, sessione ed errore tardivo, cancellazione. Suite JVM: 492 test verdi; dettagli e limiti nella roadmap. AUD-35 rimosso dal tracker.

AUD-37 P2, da codice e non riprodotto: InventoryScreens chiude picker/DeviceDialog/BatchDialog prima di edit; StructureRackScreens chiude picker rack/unità, editor e form struttura/modelli prima della conferma; MediaScreens chiude la scelta pagina da Allegati subito dopo edit. Verificare tutti i consumatori, bozze/selezioni/copia/undo e riprova su repository isolato usando l’esito session-scoped. Gli esiti del workspace mappa non chiudono automaticamente questi flussi né i collaudi manuali RES-13/19/23.

Collaudo AUD-35: il runner Gradle ha disinstallato l’app sul moto g86. Utente conferma che c’era solo Demo Comune; APK e demo ripristinati e verificati (import/riapertura/media), due backup storici SHA-256 invariati. Sei prove native ripetute con adb am instrument, app conservata; APK test rimosso. Procedura e incidente nella roadmap/verifica. connectedDebugAndroidTest non va usato su dispositivi con dati da conservare.


## AUD-36 chiuso e residuo AUD-38 — 7 ottobre 2026

Editor Inventario/Rack/allegati corretti; otto regressioni dei form e 193 test Windows verdi. Bozza, file, selezioni, copia/cestino/history, errore/riprova e undo verificati su storage isolato. Il rendering Skiko del nuovo rack nel runner DesktopApp completo resta un limite della verifica, descritto in RES-23; nessun nuovo collaudo EXE rivendicato. Dettagli e comandi nella roadmap.

AUD-38 P2: DeviceModelsSection chiude creazione/modifica/applicazione modello e CredentialsSection chiude il form prima della verifica dell'esito. Evidenza da codice, non riprodotta; verificare guasto filesystem, bozze/selezioni, errore/riprova e history. Nessuna chiusura implicita dagli esiti di AUD-36.


## AUD-37 chiuso e residuo AUD-39 — 7 ottobre 2026

Gli host Android degli edit generici attendono l'esito prima della chiusura. Diciassette prove native su moto g86 API 36, guasto SQLite isolato e riprova/undo verdi; due nuove regressioni per editor dismesso e suite di 502 report verdi. Dati/app/demo/backup preservati; nessun collaudo SQLCipher/Keystore/TalkBack aggiunto. Dettagli e comandi nella roadmap.

AUD-39 P2: sostituzione chiude e naviga prima di replaceDevice; fusione chiude prima di mergeDevices. Import/classificazione allegato, credenziale e download cartografico in MediaScreens chiudono prima dell'esito asincrono. Evidenza da codice, non riprodotta; verificare bozze/selezioni/navigazione, rifiuti/errore/riprova, cestino/media/undo dove previsto e sessione/cancellazione. Distinto dagli edit generici corretti in AUD-37.

RES-24 esteso a quattro nuove fixture JUnit di questa sessione (6708 byte) dopo interruzione dei worker Windows: rimozione respinta dal controllo automatico con blocked by policy, senza altra motivazione. Percorsi/provenienza nel tracker e in build/reports/aud36-scratch-inventory.json. Risorse storiche non ritentate; report e backup conservati.


## AUD-38 chiuso — 7 ottobre 2026

Guasto filesystem riprodotto nei cinque flussi, sia dialogo sia pannello; bozze, errori, riprova e integrità/undo verificati. 203 test Windows verdi. Rimosso dal tracker; collaudo DesktopApp/EXE conservato in RES-23. Evidenze nella roadmap e in build/reports/aud38-full.


## AUD-39 chiuso e residuo AUD-40 — 7 ottobre 2026

Bozze dei comandi specializzati, credenziali, import/classificazione allegato e download conservate sul guasto, con errore/riprova; callback protetti da sessione/cancellazione e durata della bozza. Otto prove native verdi e 530 report JVM/Compose senza regressioni; sorgenti sintetiche e persistenza reale, nessuna rete pubblica. Evidenze nella roadmap.

AUD-40 P2, da codice e non riprodotto: le conferme di cancellazione in DeviceDetailScreen e RackDetailScreen navigano indietro prima di moveToTrash, che non comunica un esito al chiamante. Verificare repository/media isolati, permanenza nella schermata sul guasto, errore/riprova, un solo cestino, undo previsto, ritorno unico e sessione/cancellazione. Distinto dalla sostituzione/fusione corretta in AUD-39.

## AUD-40 — Cancellazione apparato/rack Android — 8 ottobre 2026

Riprodotto su moto g86 API 36 con guasto SQLite isolato: DEVICE/RACK rimangono nel database ma la conferma torna subito alla lista. Due prove native rosse; dieci nuove regressioni JVM riproducono anche l'errore tardivo dopo chiusura/cambio progetto (due rosse, otto verdi). Fixture sincronizzate tramite ObjectHierarchy, non dati applicativi reali.

Rimossi i due back anticipati; il ritorno esistente reagisce all'oggetto assente dopo reload del commit, con controllo di progetto/destinazione. Nessuna nuova firma/callback, dipendenza, schema o formato. moveToTrash rispetta ensureActive prima di pubblicare stato/messaggi e sopprime errori di una sessione chiusa.

Verifica mirata: `.\gradlew.bat :mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.DeletionCommandTest --tests com.onlyfield.assetmanager.ProjectCommandTest --tests com.onlyfield.assetmanager.SpecializedCommandTest :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → BUILD SUCCESSFUL in 44s, 44 test senza fallimenti/errori/saltati. Baseline dei comandi esistenti verde prima della correzione. Dieci regressioni: guasto/riprova, doppia richiesta con un solo cestino/undo, media e collocazione rack ripristinati, ID assente, chiusura/cambio progetto, altra schermata e cancellazione ViewModel.

Native: `adb -s ZY32LNCB8C install -r` per APK principale/test e `adb -s ZY32LNCB8C shell am instrument -w -r -e class com.onlyfield.assetmanager.FailedDeletionNativeTest com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner` → **OK (2 tests), 35,168s**. AppRoot/ConfirmHost/Scaffold/snackbar reali: annullamento conferma, guasto SQLite, scheda e selezioni conservate, errore visualizzato, riprova, un solo ritorno e undo toccato nella UI; database/media verificati. L'undo attende la durata dello snackbar di errore precedente. Cache delete-save-UUID rimossa in finally, database/WAL e due backup storici SHA-256 invariati prima della riapertura. Report `build/reports/aud40-red`, `aud40-targeted`, `aud40-native` e log `aud40-*.log`.

AUD-40 completato e rimosso dal tracker; restano RES-13/19/23/24. Matrici UX/TalkBack, hardware/SQLCipher/Keystore ed EXE/focus/stampa conservano i propri limiti. Fonti primarie consultate l'8 ottobre: [eventi e navigazione UI Android](https://developer.android.com/topic/architecture/ui-layer/events) e [ensureActive Kotlin](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html); esiti e integrità dell'app derivano dalle prove locali.
RES-13 (8 ottobre): recupero SQLCipher/Keystore verificato su moto g86 API 36 in due processi distinti, con database/media/chiavi avvolte isolati. Cinque scenari di commit/rollback e blocco/riprova conservano l'esito Room e i backup; test roundtrip ordinario verde. Riavvio fisico, arresto forzato e perdita di alimentazione non eseguiti. Restano fotocamera integrata (anche porta/cavo e serie), scansione tramite fotocamera e gesti; lettori USB/scanner esterni esclusi dalle prove per decisione utente. Dettagli e comandi nella roadmap; nessuna modifica al runtime.

## AUD-41 — Serie foto Android e risultati tardivi — 8 ottobre 2026

Durante RES-13 emerge dal codice che onPhotoResult richiama onSaved in finally, prima della fine di launchCommand. pendingCommands resta positivo e preparePhoto rifiuta lo scatto successivo. PhotoCommandTest riproduce il guasto e altri due problemi: errore/callback dopo chiusura e accettazione del risultato dopo chiusura/riapertura dello stesso progetto. Baseline MediaAdditionTest verde prima della modifica; nuova suite iniziale cinque test, tre fallimenti, XML/log in build/reports/aud41-red e aud41-red.log.

La continuazione attende Job.join: soltanto dopo rilascio del comando può preparare lo scatto successivo. PendingPhoto conserva progetto/allegato/file/sessione; errori e callback sono vincolati alla sessione ancora valida. Cancellazione rilanciata e file non committati rimossi; PhotoCapture ignora la continuazione dopo uscita dalla composizione. Nessuna firma pubblica, dipendenza, schema o formato modificato.

Mirata: `.\gradlew.bat :mobile:app:testDebugUnitTest --tests com.onlyfield.assetmanager.PhotoCommandTest --tests com.onlyfield.assetmanager.MediaAdditionTest --no-parallel --max-workers=1` → **BUILD SUCCESSFUL in 17s, nove test**, zero fallimenti/errori/saltati. Verificati prima continuazione, cleanup su errore, undo, esiti dopo chiusura/cancellazione e risultato appartenente a un'altra apertura dello stesso progetto.

Native: assembleDebug/assembleDebugAndroidTest → BUILD SUCCESSFUL in 14s; install -r e `adb -s ZY32LNCB8C shell am instrument -w -r -e class com.onlyfield.assetmanager.PhotoSeriesNativeTest com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner` → **OK (5 tests), 31,371s**, moto g86 API 36. Tre serie DEVICE/PORT/CABLE con due JPEG sintetici e terzo risultato annullato; guasto SQLite, arresto della serie e riprova; host smontato durante il save senza nuovo lancio. Byte/target, persistenza, export/import AES-GCM e undo verificati. Camera e risultato del permesso sono simulati solo nel registry di test, senza aprire la fotocamera; Compose/helper/launcher/FileProvider/repository e media sono reali, host minimo. Non è collaudo del sensore, di rotazione reale o delle schede rapide porta/cavo complete. Fixture cache/object_photos/native-series-UUID rimosse in finally.

Scatti reali rinviati su risposta esplicita dell'utente. RES-13 mantiene la checklist fisica; RES-19/23 mantengono matrici UX/TalkBack e Windows, RES-24 le pulizie storiche respinte. Fonti primarie consultate l'8 ottobre: [Job.join](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-job/join.html), [scope Compose](https://developer.android.com/develop/ui/compose/side-effects) e [TakePicture](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.TakePicture). Esiti e integrità specifici dell'app derivano dalle prove locali.

Verifica finale: `.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1` → **BUILD SUCCESSFUL in 2m 45s**. 545 test nei report: 128 core, 91 exchange, 203 Windows, 123 Android; zero fallimenti/errori/saltati. Android rieseguito, moduli invariati UP-TO-DATE; APK test finale compilato. XML e conteggi conservati in `build/reports/aud41-full`, log `build/reports/aud41-full-suite.log`.

Database/WAL, chiavi avvolte applicative e due backup storici SHA-256 invariati prima della riapertura. Nessuna cache native-series residua; APK test rimosso con Success e MainActivity riaperta, evidenza `build/reports/aud41-native/final-integrity.txt`. AUD-41 completato e rimosso dal tracker. Restano quattro residui RES-13/19/23/24 (3 P2, 1 P3); prossima attività RES-19, scatti reali rimandati. Nessun EXE rigenerato, pulizia storica ritentata o commit/push.

## AUD-42 — Footer della scheda rapida — chiuso l'8 ottobre 2026

Durante RES-19, due riproduzioni native su host Compose isolato rilevano Chiudi sovrapposto a Scollega a 360 dp, testo 1,0, senza callback Dettagli. Il tocco può intercettare Chiudi e non aprire la conferma. I chiamanti completi attuali forniscono Dettagli: non è una riproduzione sulla cornice AppRoot o su Demo Comune. Il difetto riguarda comunque l'opzione supportata di PortQuickActions. La prima prova con Dettagli presente passa; la regressione geometrica senza Dettagli è rossa. Gli errori iniziali di selettore della nuova prova sono distinti dal difetto applicativo.

Azioni e navigazione riunite in un solo gruppo verticale del footer. Nessuna firma, dipendenza, schema o formato modificato. Tre regressioni native verdi nella matrice 360/412 dp, chiaro/scuro, testo 1,0/1,3; suite con 545 report senza fallimenti/errori/saltati. AUD-42 rimosso dal tracker; RES-19 conserva la matrice completa, TalkBack e i flussi non eseguiti. Fonti ed evidenze nella roadmap.

RES-23 (8 ottobre): nuova prova nativa del rack in DesktopApp/EditorFrame reale completata: guasto filesystem, bozza conservata, riprova Ctrl+S e riapertura persistita. Nessuna riproduzione del limite Skiko del runner nell'EXE. Finestra standard chiara, dati sintetici; altri editor/focus/input/checkbox/stampa aperti. 17 media invariati; app/helper chiusi. Nessun nuovo task correttivo.

RES-23, collaudi EXE successivi dell’8 ottobre: editor apparato con guasto/riprova e SHA-256 immediato invariato, quattro checkbox fusione con Tab/Spazio a scala standard, Ctrl+N/Ctrl+W durante save del progetto da 511 MiB e annullamento stampa con Escape verificati. Matrice restante aperta; roadmap e build/reports/res23-followup-20261008 contengono esiti e limiti. Tracker consolidato sulle sole quattro voci residue, senza cronologia dei task di codice già conclusi. Android/evidenze in attesa USB; nessuna pulizia storica ritentata.

## AUD-43 — Selettori Windows e focus — chiuso l’8 ottobre 2026

Rilevato durante RES-23 e riprodotto nel vero EXE: dopo annullamento apertura/esportazione, scorciatoie inattive prima di un clic. I quattro JFileChooser ora appartengono alla finestra AWT attiva anziché al frame Swing nascosto. Quattro regressioni native rosse → verdi; suite Windows 207 test senza fallimenti/errori/saltati e nuovo EXE verificato. Dialoghi centrati, Escape e scorciatoie/chiusura senza clic intermedi, dati invariati rispetto alla baseline dopo apertura. Dettagli, comandi e fonti primarie nella roadmap; AUD-43 rimosso dal tracker.

RES-23: completati anche creazione/modifica/applicazione modello, creazione/modifica credenziale e cambio tema con guasto reale/riprova, oltre all’annullamento dei selettori. Restano i soli scenari mancanti indicati nel tracker. RES-24 include la nuova fixture minima nel runtime generato e il probe invalido elencati in docs/05: rimozione respinta automaticamente con blocked by policy, non ritentata. Android ancora senza ADB; nessuna chiusura dei residui hardware/UX o delle pulizie storiche.

## AUD-44 — Alimentazioni/PoE/Badge Windows — chiuso l’8 ottobre 2026

Dodici regressioni rosse → verdi su creazione/modifica, dialogo/pannello; bozza, selezioni, errore, riprova, campi nascosti e undo verificati. 219 test Windows verdi, EXE isolato collaudato sulla nuova alimentazione con guasto reale/hash invariato/riprova/undo. Rimosso dal tracker. Runtime e fixture sintetici conservati in RES-24; dettagli, comandi, fonti e limiti nella roadmap. Moto g86 riconnesso; recupero evidenze in corso.

RES-19/24, aggiornamento 8 ottobre: 80 coppie finali e otto precedenti recuperate, 176 SHA-256 verificati e distinzione nel manifest; database/WAL/chiavi e backup invariati. Rimossi dal telefono solo evidenze verificate e APK test, MainActivity riaperta. Parti concluse rimosse dal tracker; matrice completa Android e pulizie Windows rimangono aperte. Dettagli e limiti nella roadmap.
