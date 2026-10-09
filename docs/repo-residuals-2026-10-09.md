# Residui del repository — 9 ottobre 2026

**Secondo audit completato:** AUD-52–64 verificati e rimossi. Nel [tracker](../PROJECT_STATUS.json) restano RES-13/19/23/24: **4 residui, 0 P1 / 3 P2 / 1 P3**. Completamenti ed evidenze nella [roadmap](../roadmap.md); riproduzioni e conteggi dell’audit sono la baseline storica.

L’audit iniziale identificava sette nuovi rilievi (un P1 e sei P2) oltre ai quattro residui operativi. Le riproduzioni e il perimetro iniziali sono conservati come evidenza storica; descrivono il codice precedente alle correzioni.

## Perimetro e metodo dell’audit iniziale

Inventariati tutti i **382 file versionati**, inclusi 309 sorgenti Kotlin, 161 applicativi in `src/main`. Quattro moduli Gradle e sorgenti UI condivisi in `shared/configurator`. Revisione trasversale di dominio/validazione/modifiche, persistenza e mapper, recupero, scambio/cifratura/media/import/fusione, editor e navigazione, mappe/configuratore, documenti, localizzazione, demo/fixture/test, build/CI/rilascio e documentazione. Approfonditi chiamanti, esiti di salvataggio e copertura dei punti sospetti; nessun segnaposto applicativo TODO/FIXME/HACK/NotImplemented individuato dalla ricerca.

Distinzione dell’evidenza: **riprodotto** con dati sintetici e toolchain del progetto; **da codice** per un contratto o percorso incompleto; **collaudo da eseguire** per telefono, EXE, stampante o hardware. Le ricerche e le prove mirate non certificano ogni riga, combinazione UI o dispositivo. Nessuna installazione, importazione nativa, pubblicazione o modifica di progetti utente; nessuna pulizia storica ritentata. Non è stata eseguita una nuova suite generale, né una campagna di vulnerabilità delle dipendenze.

Gli AUD precedenti sono confrontati con implementazione e chiusure, senza riaprire il loro contesto storico. AUD-44 riguarda Alimentazioni/PoE/Badge **Windows** già corretti: AUD-46 copre gli editor ancora esclusi. AUD-47 riguarda pagine/layout di stampa, senza riaprire le callback corrette da AUD-21. AUD-48 riguarda l’acquisizione prima del limite media, senza riaprire il rollback media già corretto.

## Attività ancora aperte

Restano RES-13/19/23/24: collaudi reali con i vincoli già confermati e pulizie storiche respinte, non ritentate. AUD-52–64 completati; le riproduzioni sotto descrivono la baseline e gli esiti successivi sono nelle sezioni remediation.

| Priorità | ID | Attività | Evidenza attuale |
| --- | --- | --- | --- |
| P2 | RES-13 | Fotocamera, scansione, multitouch e interruzioni improvvise | Parziale; normale riavvio già verificato, foto rinviate dall’utente |
| P2 | RES-19 | Matrice UX completa Android | Parziale; TalkBack, rotazione reale e tablet fisici restano aperti |
| P2 | RES-23 | Matrice EXE Windows e dialoghi nativi | Parziale; prove già concluse conservate |
| P3 | RES-24 | Pulizia delle sole risorse sintetiche inventariate | Parziale; precedenti rimozioni bloccate non ritentate |

## Rilievi iniziali, ora risolti

Le sette sezioni seguenti documentano la baseline. Gli esiti correnti sono nelle sezioni remediation.

## AUD-45 — Il preserve del portable non impedisce la copia sopra i dati

Fonte: [pc/app/build.gradle.kts](../pc/app/build.gradle.kts), `assemblePortable` righe 79–85 e ZIP righe 90–94. `Sync` copia l’intero runtime `compose/binaries/main/app/OnlyFieldAssetManager` e conserva `data/**` nella destinazione, senza escluderlo dalla sorgente. L’esclusione è applicata soltanto al successivo ZIP. Il runtime generato contiene già una directory dati delle prove precedenti, inventariata in RES-24.

La semantica di `preserve` evita la cancellazione dei file destinatari; non impedisce la sovrascrittura da sorgente. Probe isolato con la stessa versione Gradle: `data/projects/same.ofam` passa da `EXISTING_TARGET` a `SYNTHETIC_SOURCE`; `target-only.ofam` resta invariato. Il task reale `packagePortable` non è stato eseguito su `dist`: la riproduzione dimostra il rischio del meccanismo, non una perdita già osservata nei dati utente.

**Chiusura:** escludere dalla copia i dati sorgenti, mantenendo la conservazione dei dati destinatari. Provare sorgente contaminata, destinazione già popolata e file omonimi: hash di progetti/media/preferenze invariati; nessun dato della sorgente introdotto e nessun dato nel ZIP. Verificare prima su fixture isolate, poi packaging previsto.

## AUD-46 — Save fallito chiude ancora editor fuori dal perimetro corretto

Fonti: [NetworkLogicalSection.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/ui/NetworkLogicalSection.kt), [CablingSection.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/ui/CablingSection.kt), [NetworkPowerScreens.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/ui/screens/NetworkPowerScreens.kt), [CablingScreen.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/ui/screens/CablingScreen.kt). In Windows, sei editor rete logica azzerano `creating/editing` prima del callback; le mappature pannello fanno lo stesso, mentre il cavo chiude dopo un callback `Unit` anche se lo stato segnala errore. Android azzera le bozze prima di `vm.edit`: sei editor rete, Alimentazioni/PoE/Badge, cavi e mappature.

Due prove Compose Windows con blocco reale della sostituzione del pacchetto riproducono il caso VLAN: messaggio d’errore presente, progetto/file/history invariati, ma campo della bozza scomparso. Dialogo e pannello hanno lo stesso esito. Le altre varianti sono identificate dai callback; non sono tutte riprodotte, e Android non è collaudato nativamente in questo audit.

**Chiusura:** applicare l’esito del salvataggio usando i meccanismi già presenti (`rememberEditSave` Android e controllo esito Windows). Coprire creazione/modifica e dialogo/pannello dei moduli elencati: errore mantiene campi, selezioni, ID e dati nascosti; rilascio del guasto e riprova salvano una volta, chiudono e aggiungono un’unica modifica annullabile. Non estendere la chiusura AUD-44 a questi editor.

## AUD-49 — Validazione numerica incompleta

Fonti: [FieldValidators.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/forms/FieldValidators.kt), `decimal`/`parseDecimal`, e [EntityForms.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/forms/EntityForms.kt), `PowerFeedForm`. La conversione `toDoubleOrNull` accetta `NaN` e `Infinity`; i confronti min/max non escludono NaN, e un solo minimo non esclude Infinity. `ModelValidator` non segnala queste potenze; il controllo finitezza presente sui punti delle tratte non copre i valori numerici in questione.

Due probe costruiscono alimentazioni con `loadWatts="NaN"` e `"Infinity"`: nessun errore del form, modello strutturalmente valido, `PackageSerializer.exportPackage` solleva `SerializationException`. La configurazione JSON corrente rifiuta questi numeri. Il valore non entra correttamente nel pacchetto; combinato con AUD-46 può far perdere la bozza durante il save fallito.

**Chiusura:** validazione finita coerente tra form e modello, verificando tutti i campi decimali interessati (alimentazioni, PoE, lunghezze e coordinate pertinenti). Rifiutare valori non rappresentabili prima di scrivere, mantenere errori localizzati e round-trip dei valori finiti; non abilitare numeri speciali nel formato JSON per aggirare l’errore.

## AUD-48 — Acquisizione cartografica Android ancora incompleta

Fonte: [CartographicMapManager.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/cartography/CartographicMapManager.kt), `fetchTileBytes` e `acquireMapSnapshot`. HTTP 200 viene letto con `readBytes()` senza limite; il controllo sul PNG finale arriva dopo download e decode. Probe su server locale della sessione, chiuso al termine: risposta chunked **2.097.153 byte** interamente accettata. [DesktopCartographyManager.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopCartographyManager.kt) limita invece la risposta a 2 MiB, disconnette in `finally` e verifica tile 256×256.

Da codice: Android decodifica senza controllo preventivo delle dimensioni e non disconnette esplicitamente la connessione. La griglia 3×3 usa `center±1` senza avvolgere X né limitare Y: coordinate esterne alla griglia ai bordi e a zoom basso. Desktop gestisce già questi casi. Non sono state scaricate tile dal servizio pubblico, misurata una perdita di memoria o riprodotto un esaurimento RAM. La costante italiana `NO_NETWORK_MESSAGE` è residuo usato da una sola asserzione, mentre il percorso effettivo usa `Messages`.

**Chiusura:** limite della risposta durante la lettura, decode con dimensioni controllate, gestione risorse anche all’errore e coordinate valide ai bordi. Prove locali di chunked/Content-Length assente, risposta e immagine sovradimensionate, errore a metà griglia, zoom basso e antimeridiano/limiti latitudine; conservare attribuzione, testo localizzato e dati del progetto al fallimento. Consolidare la vecchia costante nel perimetro immediato.

## AUD-47 — Stampa Android ignora selezione pagine e layout

Fonte: [ProjectPrintDocumentAdapter.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/export/ProjectPrintDocumentAdapter.kt), `onLayout`/`onWrite`. `pages` non viene utilizzato; viene generato l’intero documento e dichiarato `ALL_PAGES`. `newAttributes` serve soltanto al flag di layout modificato. [PdfExportManager.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/export/PdfExportManager.kt) produce pagine fisse 595×842, senza ricevere formato, orientamento o margini richiesti dal sistema.

Il contratto Android richiede la scrittura delle pagine specificate e il layout secondo i nuovi attributi. Le prove PDF esistenti verificano la generazione, non questa selezione nello spooler. Evidenza statica; esito su stampante/anteprima nativa **non verificato**. Callback, errori e cancellazione corretti in AUD-21 restano acquisiti.

**Chiusura:** documento multipagina con pagina singola, intervallo e intervalli disgiunti; verificare pagine PDF effettive e callback coerenti. Cambiare orientamento, formato e margini deve produrre il layout richiesto senza tagli; preservare cancellazione/errori e completamento singolo. Aggiungere regressioni dell’adapter e collaudo nativo nell’ambito RES-19.

## AUD-50 — Selezione legacy delle alimentazioni nei documenti

Fonti: [MarkdownExportManager.kt](../shared/exchange/src/main/java/com/onlyfield/assetmanager/exchange/MarkdownExportManager.kt) righe 160–161 e [XlsxExportManager.kt](../shared/exchange/src/main/java/com/onlyfield/assetmanager/exchange/XlsxExportManager.kt) righe 357–358. Le colonne A/B cercano `feedName == "A"/"B"`, non `feedType`, nonostante il nome libero ammesso dal form e già usato nel demo (`Ingresso A/B`, `UPS → PDU`, `Alimentazione singola`). La riga riporta totali, ma non una lista completa delle alimentazioni o il dispositivo sorgente. I generatori PDF enumerano le alimentazioni e risolvono la sorgente, confermando la divergenza tra formati.

Probe su progetto valido con due alimentazioni PRIMARY_A/SECONDARY_B chiamate `Ingresso A/B`: descrizioni di presa assenti nel Markdown e celle D2/E2 del foglio XLSX alimentazione uguali a `-`. Nessun problema di filtro coinvolto.

**Chiusura:** documentare tutte le alimentazioni pertinenti con nome, tipo, sorgente e campi disponibili; eliminare la dipendenza dal nome convenzionale. Regressioni con nomi personalizzati, più sorgenti, tipi UPS/PDU/diretta/altro, filtri e tre lingue; parità delle informazioni previste nei formati. Definire la presentazione dei record multipli prima di cambiare le colonne di consegna.

## AUD-51 — Escaping Markdown parziale

Fonte: [MarkdownExportManager.kt](../shared/exchange/src/main/java/com/onlyfield/assetmanager/exchange/MarkdownExportManager.kt), inventario riga 69 e righe rack/cablaggio/VLAN/alimentazioni/badge/allegati. Testo utente interpolato senza escaping. L’helper `escaped` è applicato alla sezione hardware e alle note, quindi la protezione resta disomogenea; gestisce pipe e LF, non tutti i contesti inline.

Probe con nome apparato valido `SW|north`: la riga della tabella a sette colonne contiene nove pipe non escapate invece delle otto attese; una colonna in più altera la presentazione. La specifica GFM richiede escaping della pipe anche all’interno degli span inline. A capo e delimitatori Markdown richiedono verifica nei restanti contesti; nessuna esecuzione di codice o accesso a credenziali dedotto da questo rilievo.

**Chiusura:** escaping contestuale uniforme per celle, titoli, elenchi e span di codice; preservare il testo utente. Provare pipe, backslash, backtick e CR/LF nei principali campi e nelle tre lingue: struttura/numero colonne invariati, nessuna riga o intestazione introdotta dal contenuto.

## Legacy e attività già tracciate

La selezione A/B di AUD-50 e la chiusura anticipata di AUD-46 sono implementazioni rimaste fuori dal consolidamento precedente. `NO_NETWORK_MESSAGE` rientra in AUD-48; nessuna attività generica di pulizia duplicata. Compatibilità dei vecchi pacchetti/alias allegati/hash password, fixture di migrazione Room e test dei formati rifiutati hanno ancora chiamanti o prove: non sono classificati come codice morto solo per età o nome. Nessun aggiornamento indiscriminato delle dipendenze o nuova architettura proposto.

RES-13/19/23 mantengono esiti già acquisiti, perimetro hardware deciso dall’utente e criteri di chiusura del tracker. RES-24 conserva gli inventari e le rimozioni precedentemente bloccate: non eliminare report, backup, chiavi, demo o runtime storici senza verifica specifica. Lo scratch di questo audit è distinto da quelle risorse.

Pulizia dello scratch corrente respinta dal controllo automatico prima dell’esecuzione, motivo `blocked by policy`, senza ritentativi. Aggiunte a RES-24 le sole directory `build/tmp/repo-audit-20261009-portable`, `build/tmp/repo-audit-20261009-network` e `build/tmp/repo-audit-20261009-data`; inventario in `build/reports/repo-audit-20261009/cleanup-blocked-inventory.json`. Contengono probe, cache Gradle e dati sintetici della sessione, con copie delle evidenze già archiviate. Rimozione da completare quando consentita o manualmente, preservando i report.

## Verifiche ed evidenze

Evidenze locali ignorate da Git: `build/reports/repo-audit-20261009`, con sorgenti dei probe, init script, XML e inventario. I probe assertano il comportamento difettoso osservato: **un loro esito verde conferma la riproduzione e non chiude il rilievo**. Non sono aggiunti alla suite permanente.

| Verifica eseguita | Esito | Limite |
| --- | --- | --- |
| Gradle Sync isolato `probePortable --offline` | BUILD SUCCESSFUL in 4s; sovrascrittura dimostrata | Packaging reale e dati utente non toccati |
| `:pc:app:test --tests '*AuditNetworkSaveProbe'` con init script isolato | BUILD SUCCESSFUL in 5s; 2 casi, zero errori/fallimenti/saltati | Compose Windows; non EXE e non tutti gli editor |
| `:shared:exchange:test` AuditDataProbe + DocumentExportTest, `:mobile:app:testDebugUnitTest` AuditTileProbe + CartographicMapManagerTest | BUILD SUCCESSFUL in 3s; 10 casi, zero errori/fallimenti/saltati | 5 probe e 5 prove esistenti; nessun dispositivo Android |

Tutti i comandi usano `--no-parallel --max-workers=1`. Totale JUnit di questa verifica mirata: **12 casi, di cui 7 probe e 5 prove esistenti**, oltre al probe Gradle. Durante la preparazione dei probe, un errore di compilazione sulla firma `MasterDetailHost` e un filtro Android senza test per source set errato sono stati corretti nel solo harness temporaneo; non sono difetti del prodotto. Il primo comando combinato ha completato i 4 probe exchange ma è terminato BUILD FAILED per il filtro Android; l’esito complessivo valido è quello finale sopra.

La CI di rilascio resta manuale/su tag e verifica la suite prevista prima degli artefatti; non qualifica hardware, EXE interattivo, stampa o l’assenza di dati sorgenti locali. Validazione conclusiva di documentazione/tracker: sintassi JSON, ID unici, priorità, riferimenti/dipendenze, collegamenti locali, UTF-8 senza BOM e `git diff --check`; esito registrato in roadmap ed evidenze.

Controllo eseguito con `build/reports/repo-audit-20261009/validate-audit.ps1`: **21 Markdown e 181 collegamenti locali esistenti**, 11 ID unici, priorità 1/9/1, riferimenti presenti e dipendenze acicliche; XML archiviate con 12 casi senza fallimenti/errori/saltati. Esito zero, `git diff --check` superato. Descrizioni storiche dei quattro residui preservate e hash demo invariato rispetto alla rigenerazione precedente all’audit.

## Fonti primarie

Consultate il 9 ottobre 2026; applicazione al prodotto dedotta dal codice e dai probe, senza inferire collaudi nativi:

- [Gradle Sync, preserve](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Sync.html): protezione dalla rimozione dei file destinatari. Documentazione online corrente; la semantica è anche riprodotta con il wrapper effettivo 9.7.1.
- [Android PrintDocumentAdapter](https://developer.android.com/reference/android/print/PrintDocumentAdapter): layout con nuovi attributi e scrittura delle pagine specificate.
- [Android PrintedPdfDocument](https://developer.android.com/reference/android/print/pdf/PrintedPdfDocument): dimensioni e area contenuto determinate dagli attributi di stampa; riferimento di contratto, senza imporre una nuova dipendenza.
- [GitHub Flavored Markdown, tables](https://github.github.com/gfm/#tables-extension-): pipe escapate anche negli span inline.

### Remediation AUD-45, 9 ottobre

La sorgente del Sync esclude data/** e la destinazione mantiene preserve(data/**); progetti, media e preferenze omonimi non vengono sovrascritti. ZIP privo di dati. AUD-45 rimosso dal tracker.

Baseline isolata: hash delle preferenze destinatario modificato (errore atteso). Dopo la correzione: powershell -NoProfile -File tools/testing/portable-data.ps1 → PASS; fixture eliminata dal finally. .\gradlew.bat :pc:app:packagePortable --no-parallel --max-workers=1 → BUILD SUCCESSFUL. Hash dei dati in dist invariati; ZIP effettivo senza data/. La rigenerazione ha eliminato la vecchia fixture sintetica nel runtime sorgente: quattro file ripristinati con SHA-256 identico, un pacchetto di 43.584 byte senza copia identica reperibile. Utente informato: recupero non richiesto, tutto materiale di test. Report portable-source-restoration.json. Aggiunto controllo preventivo su createDistributable: un runtime con data/ blocca la rigenerazione prima della rimozione. Manifest in build/reports/tracker-remediation-20261009/portable-data-before.json. Fonte primaria: [Gradle Sync](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Sync.html), consultata il 9 ottobre.

### Remediation AUD-46, 9 ottobre

Rete logica e cablaggio Windows chiudono gli editor soltanto dopo save riuscito e mostrano il guasto nella bozza. Alimentazioni/PoE/Badge, rete e cablaggio Android usano rememberEditSave: errori e campi conservati, chiusura sulla callback positiva, risultati ignorati dopo uscita dalla composizione. ID e campi nascosti delle modifiche preservati. Corretto anche il tipo sintetico non valido nella modifica dei cavi senza objectTypeId.

32 regressioni Compose Windows verdi: sei editor rete e due cablaggio, creazione/modifica e dialogo/pannello, guasto reale NOSHARE_DELETE, progetto/file/trash/history invariati, riprova singola, round-trip e undo completo. Baseline documentata: 30 casi riprodotti e due fixture picker errate, corrette prima della prova finale. Il cavo senza tipo introduceva il tipo non UUID cable e rendeva il pacchetto non importabile; fallback legacy corretto, round-trip verificato. Comando .\gradlew.bat :pc:app:test --tests *FailedNetworkSaveTest* :mobile:app:testDebugUnitTest --tests *SpecializedCommandTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL. Le regressioni helper Android verificano errore/riprova e callback dismesse; compilazione delle schermate Android riuscita. Nessun nuovo collaudo nativo/AppRoot Android o EXE completo: criteri trasferiti esplicitamente a RES-19/23. Fonte primaria [stato Compose](https://developer.android.com/develop/ui/compose/state), consultata il 9 ottobre.

### Remediation AUD-49, 9 ottobre

Numeri non finiti e overflow rifiutati dai form e dal modello per carichi W/VA, potenze PoE, budget hardware apparati/modelli, lunghezze, coordinate di posizionamenti/annotazioni/tratte. Errori it/en/es prima della scrittura: guard nei salvataggi Room, storage Windows ed export .ofam; validazioni strutturali indipendenti mantengono i rilievi documentali ammessi. JSON continua a rifiutare i numeri speciali. Il campo budget PoE usa il parser decimale condiviso e finito.

Baseline due regressioni rosse; prove finali NonFiniteValidationTest (due test, matrici NaN/Infinity/-Infinity/overflow e nove superfici numeriche), FinitePackageTest, ProjectRepositoryTest.nonFiniteUpdateLeavesRoomProjectUnchanged e NonFiniteSaveTest: cinque test verdi. Valori finiti con punto/virgola e pacchetto .ofam in round-trip; stream export vuoto, progetto Room e file/history Windows invariati al rifiuto. Comandi Gradle mirati con --no-parallel --max-workers=1: BUILD SUCCESSFUL. Fonti [Kotlin isFinite](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/is-finite.html), consultata il 9 ottobre. Residui UI nativi delle bozze gia in RES-19/23.

### Remediation AUD-48, 9 ottobre

Tile Android limitati a 2 MiB durante la lettura, connessione disconnessa in finally. Dimensioni 256x256 verificate prima del decode; bitmap individuali e risultato sempre riciclati, anche a guasto parziale. La griglia usa wrap X e clamp Y ai bordi; input geografici non finiti o fuori range rifiutati. Rimossa la costante italiana legacy NO_NETWORK_MESSAGE dal modulo Android; messaggi it/en/es conservati.

Baseline HTTP locale: risposta chunked oltre 2 MiB accettata. .\gradlew.bat :mobile:app:testDebugUnitTest --tests *Cartograph* --no-parallel --max-workers=1: BUILD SUCCESSFUL, sei test. Soglia esatta ammessa, oltre soglia rifiutato con/senza Content-Length; immagini 512x256, non immagini e payload sovradimensionati rifiutati; errore al quinto tile interrompe il risultato; griglie zoom 1/17 ai quattro bordi e attribuzione PNG 768x768 verificate con Robolectric. Server locale chiuso dal finally, nessun download pubblico o collaudo OOM rivendicato. Fonti [BitmapFactory.Options](https://developer.android.com/reference/android/graphics/BitmapFactory.Options#inJustDecodeBounds), [HttpURLConnection](https://developer.android.com/reference/java/net/HttpURLConnection#disconnect()), consultate il 9 ottobre.

### Remediation AUD-47, 9 ottobre

La stampa Android calcola geometria e paginazione dagli attributi di sistema: formato, orientamento e margini. Il layout viene misurato senza conservare pagine PDF; la scrittura genera soltanto le pagine richieste, preservando numerazione originale e restituendo intervalli effettivi ordinati e uniti. Export PDF ordinario A4 conservato. Layout e scrittura lavorano fuori Main, callback singola su Main; cancellazione controllata durante la generazione e destinazione chiusa anche prima del lavoro.

Baseline: onLayout dichiara PAGE_COUNT_UNKNOWN (regressione riprodotta). Gradle :mobile:app:testDebugUnitTest --tests "*PrintAdapterTest*" e assembleDebug/assembleDebugAndroidTest: BUILD SUCCESSFUL; 3 regressioni JVM. adb install -r (senza disinstallazione) e am instrument -e class com.onlyfield.assetmanager.PrintPdfNativeTest: OK (1 test), Moto g86. PDF reali A4 verticale e A5 orizzontale, margini asimmetrici, 160 apparati e scheda rack: dimensioni, bounding box, testo completo, pagina singola/intervallo/intervalli disgiunti e sovrapposti, conteggio e intervalli restituiti coerenti. File persistenti Android invariati per SHA-256; fixture cache rimossa. Evidenze: build/reports/tracker-remediation-20261009/print-native.txt. Fonti primarie: [PrintDocumentAdapter](https://developer.android.com/reference/android/print/PrintDocumentAdapter) e [PrintedPdfDocument](https://developer.android.com/reference/android/print/pdf/PrintedPdfDocument), consultate il 9 ottobre.

### Remediation AUD-50, 9 ottobre

Markdown e foglio XLSX Alimentazione adottano una riga per ogni record, senza selezione dal nome A/B: apparato, nome/circuito, tipo, sorgente, presa/uscita, tensione, carico VA/W, autonomia osservata, fonte/data del rilievo e note. XLSX conserva celle numeriche per tensione/carichi/autonomia; PoE e badge restano distinti dai record, compresi badge su target non apparato. PDF Android/Windows enumerano gli stessi dettagli. I filtri selezionano le alimentazioni del consumatore; si risolve solo il nome della sorgente esterna come contesto, senza esportarne inventario o altri record. Nessuna autonomia calcolata.

Baseline PowerDocumentTest: 2 test falliti per circuiti personalizzati omessi. Dopo: test mirati exchange (PowerDocumentTest/DocumentExportTest/DocumentSelectionTest), report Windows e PrintAdapterTest: BUILD SUCCESSFUL; nuova regressione PDF Windows PowerReportTest verde. Android build debug/test verde; am instrument SurveyPdfTest,PrintPdfNativeTest → OK (4 tests): PDF reali con tipi PRIMARY_A, SECONDARY_B, UPS, PDU, diretta, OTHER, UNKNOWN, sorgente esterna al filtro e campi completi, tre lingue. Database/preferenze Android invariati per SHA-256 (3 file); cambiato il solo marcatore runtime profileInstalled dopo install -r. Evidenze power-and-print-native.txt e android-after-power.json nella cartella remediation. Fonti: contratto PowerFeed corrente, selezione DocumentSelection verificata dalle regressioni; [GFM tabelle](https://github.github.com/gfm/#tables-extension-), consultato il 9 ottobre.

### Remediation AUD-51, 9 ottobre

Escaping Markdown applicato a testo utente in titoli, metadati, inventario, hardware, rack, cablaggio, VLAN, alimentazioni, badge, attribuzioni, note e avvisi. CR/LF normalizzati in spazi soltanto nel documento; punteggiatura Markdown/HTML e backslash resi letterali. Gli span usano delimitatori adeguati ai backtick del contenuto; le pipe sono esterne agli span per mantenere tabelle e backslash anche nei renderer GFM. Testo del progetto invariato. Rimossi i quattro messaggi legacy delle colonne A/B non più usati.

Baseline MarkdownEscapingTest: intestazione CR/LF introdotta dal testo utente (regressione riprodotta). Gradle exchange: 12 regressioni documenti/filtri/lingue verdi prima delle prove ai bordi; MarkdownEscapingTest ora 2 test, BUILD SUCCESSFUL. tools/testing/markdown-content.ps1 → PASS it/en/es, 7 tabelle ciascuna: parser Markdig di ConvertFrom-Markdown verifica intestazioni, liste, markup, righe/colonne e testo letterale rispetto al controllo; PASS anche 7 casi span (pipe, backslash adiacenti, delimitatori backtick, spazi e CR/LF). Fixture/evidenze in shared/exchange/build/reports/markdown-*.md; nessuna dipendenza aggiunta. Fonte primaria [GFM](https://github.github.com/gfm/) e sezioni tabelle/escape/code span, consultate il 9 ottobre.

### Verifica finale della remediation

643 test JVM/Compose e quattro prove native PDF verdi; APK compilate. Comandi, risultati e limiti nella roadmap e nella guida di verifica. Al termine di quella remediation restavano soltanto RES-13/19/23/24; il secondo audit documentato sotto aggiunge i nuovi rilievi, senza riaprire AUD-45–51. Packaging protegge i dati destinatari e rifiuta la rigenerazione con data/ sorgente; nessun recupero pendente per la vecchia fixture sintetica, come confermato dall’utente.

Pulizia finale della remediation: il controllo automatico ha respinto la rimozione dei soli build/tmp/remediation_docs.py, build/tmp/__pycache__/remediation_docs.cpython-313.pyc e della directory cache se vuota, con motivo blocked by policy. Nessuna rimozione eseguita o ritentata. Due file inventariati con dimensioni/SHA-256 in build/reports/tracker-remediation-20261009/cleanup-blocked-inventory.json; aggiunti a RES-24. I report restano conservati.

## Secondo audit completo — 9 ottobre 2026

Ricognizione dei **398 file versionati** e revisione trasversale di dominio, persistenza, scambio, media, UI, documenti, test, build e rilascio. Individuati **11 nuovi rilievi** (2 P1, 8 P2, 1 P3), oltre ai quattro residui esistenti (3 P2, 1 P3). Totale alla rilevazione **15 attività: 2 P1 / 11 P2 / 2 P3**. Il primo audit e le correzioni AUD-45–51 sopra sono conservati come storico.

`REPRODUCED` indica un caso effettivamente osservato con dati sintetici; `CODE_REVIEW` un percorso individuato nel sorgente senza riproduzione del guasto; `PARTIAL_VERIFICATION` combina casi riprodotti e conseguenze ancora da verificare. I probe sono stati eseguiti in memoria con le classi compilate del progetto e input sintetici: gli esiti essenziali sono conservati sotto, senza nuove fixture o script persistiti. Non hanno modificato i progetti utente. I 46 test esistenti verdi non costituiscono regressioni dei nuovi difetti, aperti alla rilevazione e ora chiusi dalle regressioni nelle sezioni remediation.

### AUD-52 — Impedire la cancellazione di piani con annotazioni

**P1 · baseline REPRODUCED; completato**. Fonti: [ProjectEdits.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/edit/ProjectEdits.kt), [ModelValidator.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/validation/ModelValidator.kt), [ProjectSection.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/ui/ProjectSection.kt), [StructureRackScreens.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/ui/screens/StructureRackScreens.kt).

ProjectEdits.deleteArea (ProjectEdits.kt:51) controlla apparati, rack, collocazioni e tratte ma non le annotazioni. Probe in memoria: cancellazione consentita, annotazione conservata e pacchetto esportato non riapribile con INVALID_ANNOTATION_AREA (ModelValidator.kt:373).

Probe: progetto valido prima della cancellazione (`issues=[]`); `deletion_allowed=true`, `retained_annotations=1`; dopo export/import, `saved_copy_reopens=false`, `issues=[INVALID_ANNOTATION_AREA]`. Il problema è il riferimento al piano eliminato, non l’assenza di un allegato.

**Chiusura:** Rifiutare la cancellazione del piano referenziato, conservando dati e possibilità di riapertura/scambio. Verificare rifiuto senza mutazione e pacchetto valido; mantenere consentita la cancellazione di un piano realmente non referenziato.

### AUD-53 — Rendere atomiche creazione Android e protezione richiesta

**P1 · baseline CODE_REVIEW; completato**. Fonti: [ProjectViewModel.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/ui/ProjectViewModel.kt), [ProjectRepository.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/ProjectRepository.kt), [ProjectStore.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/ProjectStore.kt), [NewSiteWizard.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/onboarding/NewSiteWizard.kt).

ProjectViewModel.finishNewSite salva progetto e password separatamente (ProjectViewModel.kt:256–257). Un guasto intermedio lascia una copia senza la protezione applicativa richiesta; buildProject genera un nuovo ID alla riprova. Rischio da codice, non guasto nativo riprodotto; la cifratura SQLCipher del database resta distinta.

La copia parziale riguarda la protezione richiesta dal wizard. Il database Android resta cifrato con SQLCipher: non è stata osservata una copia del database in chiaro né una perdita reale. Il guasto fra i due salvataggi e i duplicati alla riprova richiedono una futura regressione con errore di persistenza.

**Chiusura:** Creazione e protezione atomiche: nessuna copia parziale al guasto o annullamento e una sola copia alla riprova. Verificare creazione semplice/protetta, guasto fra i passi e password, senza duplicati.

### AUD-54 — Conservare wizard Windows e bozza di rinomina Android al guasto

**P2 · baseline CODE_REVIEW; completato**. Fonti: [DesktopAppState.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopAppState.kt), [ProjectDialogs.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/ui/dialogs/ProjectDialogs.kt), [ProjectsScreen.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/ui/screens/ProjectsScreen.kt), [ProjectViewModel.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/ui/ProjectViewModel.kt).

DesktopAppState.createProject azzera il dialogo prima della persistenza (DesktopAppState.kt:502–506); ProjectsScreen chiude la rinomina prima di vm.renameProject (ProjectsScreen.kt:111). I campi locali non restano disponibili al guasto. Percorsi fuori dal perimetro degli editor già corretti.

Il wizard Windows conserva i campi in stato locale del dialogo; la rinomina Android chiude nel callback di conferma e avvia il comando senza attendere un esito. Sono flussi distinti dagli editor già chiusi nelle remediation precedenti; nessuna nuova matrice UI nativa eseguita.

**Chiusura:** Conservare bozza, campi ed errore fino al successo; permettere correzione e riprova, chiudere soltanto dopo persistenza riuscita. Verificare entrambi i flussi, singolo salvataggio e risultati tardivi senza chiusure della schermata sbagliata.

### AUD-55 — Rifiutare pacchetti con entry o metadati incoerenti

**P2 · baseline REPRODUCED; completato**. Fonti: [PackageSerializer.kt](../shared/exchange/src/main/java/com/onlyfield/assetmanager/exchange/PackageSerializer.kt), [PackageModels.kt](../shared/exchange/src/main/java/com/onlyfield/assetmanager/exchange/PackageModels.kt), [DesktopStorageManager.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopStorageManager.kt).

PackageSerializer.readPackage (PackageSerializer.kt:247) accetta project.json duplicati con ultima copia prevalente, manifest.projectId discordante, attachmentsEncrypted senza cifratura e progetto marcato protetto in ZIP non cifrato. Quattro pacchetti sintetici importati senza errori strutturali; controllo valido accettato.

Probe del pacchetto valido: `accepted=true`, `structural=0`. Stesso esito per `duplicate_project`, `identity_mismatch`, `attachment_flag_without_encryption`, `protected_project_in_plain_package`; nel duplicato il nome importato diventa `Second duplicate wins`. Non è una prova di attacco su storage reale: occorre validare il contratto prima della conferma/import applicativo.

**Chiusura:** Rifiutare strutturalmente duplicati e incoerenze fra identità, payload e flag di protezione. Conservare import dei pacchetti validi semplici/protetti; chiudere staging e payload a ogni rifiuto senza modificare lo storage locale.

### AUD-56 — Segnalare dati Android illeggibili senza trasformarli in valori vuoti

**P2 · baseline PARTIAL_VERIFICATION; completato**. Fonti: [NetworkMappers.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/mappers/NetworkMappers.kt), [StructureMappers.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/mappers/StructureMappers.kt), [ProjectStore.kt](../mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/ProjectStore.kt).

NetworkMappers.kt:95–104 e 156–163 sostituiscono JSON VLAN tagged e membri LAG illeggibili con liste vuote: entrambi riprodotti. StructureMappers e altri mapper usano valori predefiniti per enum sconosciuti: questa parte è da codice. Un successivo salvataggio può riscrivere la rappresentazione alterata; nessuna perdita osservata su dati utente.

Probe mapper: `valid_tagged=[10,20]`, `corrupt_tagged=[]`, `corrupt_lag_members=[]`. Le liste vuote sono riprodotte; i fallback enum e il rischio della successiva riscrittura sono da codice. Non è stata eseguita una riscrittura del database del dispositivo.

**Chiusura:** Segnalare la lettura fallita e impedire salvataggi della rappresentazione alterata, preservando righe originali. Verificare JSON corrotto, enum sconosciuti, valori validi e riprova dopo correzione, con errore visibile e originali invariati.

### AUD-57 — Allineare validazione rete e controllo dei riferimenti

**P2 · baseline REPRODUCED; completato**. Fonti: [ModelValidator.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/validation/ModelValidator.kt), [FieldValidators.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/forms/FieldValidators.kt), [EntityForms.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/forms/EntityForms.kt).

ModelValidator.kt:490–538 non verifica target dello scope VLAN, riferimento subnet.vlanId e validità CIDR oltre la barra. Probe: not-an-address/99, VLAN assente e scope DEVICE inesistente accettati senza avvisi; controllo valido accettato. FieldValidators.cidr applica controlli più completi nei form.

Probe `valid_subnet`, `invalid_cidr`, `missing_vlan_reference` e `missing_scope_device`: tutti `accepted=true`, `issues=[]`. Il CIDR errato è `not-an-address/99`; gli ID mancanti sono UUID formalmente validi, ma non presenti nel progetto. La chiusura deve mantenere la possibilità di documentare dati realmente incompleti dove ammesso dal contratto.

**Chiusura:** Rendere coerenti form e validazione progetto/import; controllare CIDR e riferimenti VLAN/scope, distinguendo errori strutturali e incompletezza documentale ammessa. Verificare limiti validi, dati incompleti ammessi e riferimenti invalidi prima di sostituire dati locali.

### AUD-58 — Conservare testo XLSX con escaping ST_Xstring corretto

**P2 · baseline REPRODUCED; completato**. Fonti: [XlsxExportManager.kt](../shared/exchange/src/main/java/com/onlyfield/assetmanager/exchange/XlsxExportManager.kt), [03-export-and-documents.md](../docs/03-export-and-documents.md).

XlsxExportManager.escapeXml (XlsxExportManager.kt:76–91) gestisce solo le entità XML. U+0001 nel nome produce XML non valido in due fogli; la sequenza letterale _x0041_ resta non protetta. Probe su XLSX generato e parser XML; apertura Excel nativa non eseguita. Riferimento Microsoft ST_Xstring nel report.

Tre XLSX generati in memoria: il controllo valido non ha errori XML; `SW` seguito da U+0001 e `north` produce `not well-formed (invalid token)` in `sheet1.xml` e `sheet5.xml`; `Literal _x0041_ device` conserva la sequenza non protetta (`LITERAL_ESCAPE_LEFT_UNESCAPED=True`). Parsing con `xml.etree.ElementTree` della libreria standard Python. L’interpretazione errata in Excel è un rischio dedotto dal formato, non un collaudo Excel eseguito.

Fonti primarie consultate il 9 ottobre: [Microsoft ST_Xstring](https://learn.microsoft.com/en-us/openspecs/office_standards/ms-oi29500/d34ae755-c53f-4a44-a363-c6dd3ee018a4), per caratteri di controllo e underscore che introducono `_xHHHH_`, e [W3C XML 1.0](https://www.w3.org/TR/xml/#charsets), per i caratteri ammessi nell’XML. La gestione di CR, LF e tab deve rispettare il contesto del testo della cella.

**Chiusura:** Generare XML valido e conservare testo secondo ST_Xstring, incluse sequenze letterali _xHHHH_, controlli, Unicode e spazi/CR/LF/tab. Aggiungere regressioni nelle tre lingue, preservando testo originale e tipi delle celle.

### AUD-59 — Controllare dimensioni delle tile Windows prima della decodifica

**P2 · baseline CODE_REVIEW; completato**. Fonti: [DesktopCartographyManager.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopCartographyManager.kt), [08-floor-map.md](../docs/08-floor-map.md).

DesktopCartographyManager.kt:104–106 chiama ImageIO.read prima di verificare 256×256 pixel. Il limite di 2 MiB sui byte compressi è già presente ma non limita l’allocazione dell’immagine decodificata. Nessun OOM provocato; il controllo Android di AUD-48 resta una chiusura distinta.

La verifica 256×256 avviene dopo `ImageIO.read`: il limite sui byte della risposta non sostituisce il limite sui pixel. Il rischio di allocazione eccessiva deriva dall’ordine delle chiamate; nessun OOM provocato e nessuna nuova tile scaricata da servizi pubblici. La correzione Android AUD-48 resta valida nel suo perimetro.

Fonte primaria consultata il 9 ottobre: [Oracle ImageReader](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/ImageReader.html), API `getWidth`, `getHeight`, `read` e `dispose`, per verificare le dimensioni prima della decodifica.

**Chiusura:** Verificare dimensioni con ImageReader prima di decodificare, conservando limite sui byte e rilascio risorse. Verificare tile normale, immagine compressa di dimensioni eccessive e input malformato tramite sorgente locale, con rifiuto prima dell’allocazione completa.

### AUD-60 — Rendere riconoscibili gli errori della planimetria nei PDF Windows

**P2 · baseline CODE_REVIEW; completato**. Fonti: [PdfReportWriter.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/report/PdfReportWriter.kt), [DesktopDocumentManager.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopDocumentManager.kt), [PlanMedia.kt](../pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/PlanMedia.kt).

PdfReportWriter.kt:172 usa runCatching { planImage(area) }.getOrNull(): un errore di caricamento della planimetria viene trattato come assenza dello sfondo e il PDF può risultare riuscito. Nessuna riproduzione nativa eseguita.

Il callback della planimetria può fallire nella lettura o decodifica di immagine/PDF; il writer converte l’eccezione in `null`. L’assenza legittima della planimetria e l’esclusione tramite filtri devono continuare a funzionare. Il guasto va reso riconoscibile nel flusso Documenti, senza una nuova omissione silenziosa.

**Chiusura:** Distinguere planimetria assente da presente ma illeggibile e rendere visibile il fallimento, senza indicare completo un documento che omette lo sfondo richiesto. Verificare immagini/PDF illeggibili, sfondo valido, assenza legittima ed esclusione tramite filtri.

### AUD-61 — Conservare pieghe salvate con coordinate uguali al default storico

**P2 · baseline REPRODUCED; completato**. Fonti: [ObjectMap.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/model/ObjectMap.kt), [MapScene.kt](../shared/core/src/main/java/com/onlyfield/assetmanager/core/model/MapScene.kt), [MapSceneTest.kt](../shared/core/src/test/java/com/onlyfield/assetmanager/core/MapSceneTest.kt).

CableRoute.bends (ObjectMap.kt:36–39) elimina il punto interno quando points coincide con LEGACY_DEFAULT, senza distinguere dati correnti. Probe: tratta corrente con tre punti (0.2,0.5), (0.5,0.5), (0.8,0.5) restituisce zero pieghe invece di una. MapSceneTest mantiene esplicitamente questa euristica.

Probe su una tratta corrente esplicitamente valorizzata con i tre punti: `CURRENT_ROUTE_INTERIOR=1`, `VISIBLE_BENDS=0`. Le coordinate non identificano l’età dei dati. L’attuale test storico di `MapSceneTest` sostiene l’euristica, quindi il test verde non dimostra la conservazione di una piega corrente.

**Chiusura:** Conservare tutti i punti esplicitamente salvati nelle tratte correnti, anche dopo spostamento degli estremi, persistenza e scambio .ofam. Verificare caso riprodotto e tratte rettilinee valide; limitare eventuale compatibilità storica ai dati realmente identificabili come tali.

### AUD-62 — Eliminare il serializer di modelli senza consumatori applicativi

**P3 · baseline CODE_REVIEW; completato**. Fonti: `DeviceModelSerializer.kt` (rimosso da AUD-62), `DeviceModelSerializerTest.kt` (rimosso da AUD-62), [PackageSerializer.kt](../shared/exchange/src/main/java/com/onlyfield/assetmanager/exchange/PackageSerializer.kt).

DeviceModelSerializer.kt:6 è referenziato soltanto da DeviceModelSerializerTest; nessun consumatore applicativo o flusso di import autonomo individuato. I modelli viaggiano già nel progetto attraverso PackageSerializer e .ofam.

Ricerca dei consumatori: `rg -n 'DeviceModelSerializer' shared mobile pc` trova soltanto l’oggetto e il suo test. Alla rilevazione la rimozione non era eseguita; ora completata nella remediation AUD-62. I modelli e le loro fixture nel flusso `.ofam` restano necessari.

**Chiusura:** Eliminare DeviceModelSerializer e il test dedicato; verificare che i DeviceModel continuino a essere conservati nello scambio .ofam semplice e protetto. Mantenere compatibilità utilizzate e fixture utili.

### Verifica e limiti del secondo audit

Nella fase di analisi è stato eseguito:

```powershell
.\gradlew.bat :shared:exchange:test --tests '*PackageSerializerTest*' --tests '*ContractVersionTest*' --tests '*PasswordHasherTest*' :mobile:app:testDebugUnitTest --tests '*ProjectCommandTest*' --tests '*ProjectRepositoryTest*' --no-parallel --max-workers=1
```

Esito: **BUILD SUCCESSFUL in 1m 21s**, **46 test** (12 exchange e 34 Android JVM), zero fallimenti/errori/saltati. Probe in memoria distinti dalla suite: cancellazione/riapertura, quattro pacchetti incoerenti con controllo valido, mapper VLAN/LAG, rete, XML XLSX e pieghe. I report di `build/reports/repo-audit-20261009` appartengono al primo audit; non vengono presentati come archivio delle nuove riproduzioni.

Baseline documentale prima dell’aggiornamento: 21 Markdown, 182 link locali validi, UTF-8 senza BOM e `git diff --check` superato. Le verifiche finali dell’aggiornamento sono registrate nella roadmap. Nessuna suite generale ripetuta per queste sole modifiche documentali.

RES-13/19/23/24 conservano descrizioni e limitazioni: foto rinviate, scanner USB esclusi, riavvio normale già verificato distinto da arresto forzato/perdita improvvisa, matrici native ancora parziali e pulizie respinte non ritentate. Le compatibilità ancora usate e le fixture utili sono conservate; nessuna rimozione generica motivata dal nome “legacy”. Correzioni runtime, collaudi nativi, pulizie, commit e push restano fuori da questo intervento.

## Remediation AUD-52 — Piani con annotazioni

La cancellazione condivisa rifiuta anche i piani referenziati da annotazioni, senza mutare il progetto. PackageSerializerTest verifica rifiuto, conservazione della nota, cancellazione del piano vuoto e riapertura .ofam semplice/protetta. Baseline verde; finale .\gradlew.bat :shared:exchange:test --tests *PackageSerializerTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 5s, nessun fallimento. Prima compilazione della nuova fixture corretta per usare label e coordinate obbligatorie. Fonte ufficiale per verifica mirata: [Gradle JVM testing](https://docs.gradle.org/current/userguide/java_testing.html), consultata il 9 ottobre. Nessun collaudo nativo richiesto per il controllo condiviso.

## Remediation AUD-53 — Creazione Android atomica

ProjectRepository.createProject prepara il verificatore e salva progetto/inventario/protezione nella stessa transazione Room. Il wizard conserva ID alla riprova e blocca invii simultanei; un ID già esistente viene rifiutato. Baseline ImportedProtectionTest: BUILD SUCCESSFUL in 37s. Finale .\gradlew.bat :mobile:app:testDebugUnitTest --tests *ProjectCreationTest* --tests *ImportedProtectionTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 21s, 9 test, zero fallimenti/errori/saltati. Coperti creazione semplice/protetta, password errata, guasto dopo inserimento progetto, rollback verificatore/inventario, annullamento prima della transazione e riprova senza duplicati. Una prima fixture riutilizzava un ID sede tra progetti ed è stata corretta: il rifiuto ownership esistente resta intatto. Fonte: [Room withTransaction](https://developer.android.com/reference/androidx/room/RoomDatabaseKt), consultata il 9 ottobre. Prova Room JVM distinta da SQLCipher nativo; messaggi/focus del wizard protetto restano nella matrice RES-19.

## Remediation AUD-54 — Bozze wizard e rinomina al guasto

Il wizard Windows conserva lo stato in DesktopAppState durante la rimozione temporanea del dialogo per I/O; chiude e azzera i campi solo dopo commit riuscito. Rinomina Android usa EditSave: errore nel dialogo, campi conservati e callback ignorato dopo uscita dalla composizione. Baseline mirata verde; finale .\gradlew.bat :pc:app:test --tests *ProjectWizardSaveTest* :mobile:app:testDebugUnitTest --tests *ProjectCommandTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 26s. Prova Compose con storage inaccessibile e riprova protetta singola, prove Room di guasto/riprova e risultato tardivo. La compilazione Android della baseline includeva già il callback di rinomina appena aggiornato; la baseline Desktop era precedente. Collaudo EXE/focus e dialogo nativo Android restano in RES-23/19.

## Remediation AUD-55 — Coerenza pacchetti .ofam

Import rifiuta duplicati manifest/progetto/media, ID manifest discordante, payload plain/encrypted conflittuali, metadati crittografici in pacchetti semplici e progetto protetto in ZIP semplice. Ogni rifiuto è strutturale e chiude staging. Conservata compatibilità utilizzata: cifratura richiesta esplicitamente su progetto non protetto e vecchi allegati non cifrati dentro progetto cifrato restano ammessi. Baseline PackageSerializerTest/StagedPayloadTest verde; finale .\gradlew.bat :shared:exchange:test --tests *PackageMetadataTest* --tests *PackageSerializerTest* --tests *StagedPayloadTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 6s, zero fallimenti/errori/saltati. Nuove fixture includono duplicati reali, staging già presente, controlli validi semplici/protetti; nessun accesso a storage utente.

## Remediation AUD-56 — Letture Android rigorose

Rimossi i fallback dei cinque mapper: JSON VLAN/LAG illeggibile ed enum sconosciuti interrompono la lettura, senza costruire valori vuoti/predefiniti. Il comando edit segnala il guasto e non salva; righe e stato visibile precedente restano intatti. Baseline 37 prove verdi; finale .\gradlew.bat :mobile:app:testDebugUnitTest --tests *CorruptStoredDataTest* --tests *ProjectCommandTest* --tests *ProjectRepositoryTest* --no-parallel --max-workers=1: BUILD SUCCESSFUL in 25s, 39 test, zero fallimenti/errori/saltati. Coperti i due JSON, tutti i 29 campi enum interessati, valori validi, riprova dopo correzione ed errore del comando. La prima fixture verificava la colonna legacy del modello mentre configurationJson corrente prevale; corretta per esercitare il formato legacy effettivamente letto. Fonte: [Kotlin enumValueOf](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/enum-value-of.html), consultata il 9 ottobre. Nessuna migrazione schema; collaudo messaggi/focus Android resta RES-19.

## Remediation AUD-57 — Validazione rete e riferimenti

ModelValidator riusa il controllo CIDR IPv4 dei form e controlla subnet.vlanId e scope VLAN/subnet per progetto/sede/apparato. Riferimenti inesistenti sono strutturali; destinazione SITE/DEVICE non rilevata resta documentale, con messaggi it/en/es. Baseline ModelValidatorTest/EntityFormsTest verde; verifica con NetworkValidationTest e DemoSeedTest: BUILD SUCCESSFUL in 34s; regressioni scope ampliate: BUILD SUCCESSFUL in 3s. Conservati limiti /0 e /32, incompletezza ammessa e demo valido; import semplici/protetti invalidi non restituiscono pacchetti. Fonte [RFC 4632](https://www.rfc-editor.org/rfc/rfc4632), consultata il 9 ottobre. Emersi e tracciati AUD-63 (VLAN referenziate: blocco richiesto dall’utente) e AUD-64 (scope nelle operazioni sede/apparato), da completare separatamente.

## Remediation AUD-63 — Blocco cancellazione VLAN referenziata

Applicata la decisione utente: ProjectEdits.deleteVlan rifiuta la cancellazione quando una subnet usa la VLAN, con messaggio it/en/es. Windows visualizza il rifiuto senza chiamare il salvataggio; Android lo riceve nel comando edit senza mutazione. Baseline Desktop ProjectEdits/FailedNetworkSave verde (28s); finale shared/exchange NetworkValidationTest + pc VlanDeletionUiTest + compileDebugKotlin: BUILD SUCCESSFUL in 13s. Android ProjectCommandTest: BUILD SUCCESSFUL in 20s. Verificati dati invariati, errore visibile, riprova dopo scollegamento esplicito della subnet, salvataggio singolo e .ofam semplice/protetto valido. Nessuna modifica automatica delle reti. Rimane distinto il collaudo visivo nativo RES-19/23.

## Remediation AUD-58 — Testo XLSX conservato

XLSX applica ST_Xstring al testo delle celle: protegge underscore iniziali delle sequenze letterali, codifica controlli/XML non validi e CR, conserva LF/tab, Unicode e spazi tramite xml:space=preserve. Originali e tipi numerici invariati. Baseline documentale verde; finale XlsxTextTest/LocalizedExportsTest/DocumentSelectionTest/DemoXlsxPathsTest: BUILD SUCCESSFUL in 6s, 7 test, zero fallimenti/errori/saltati. Tutte le parti XML parsate nelle tre lingue e testo ricostruito in un solo passaggio. Due tentativi iniziali della fixture cercavano erroneamente altezza rack nei fogli; dopo segnalazione e lettura del generatore, la prova numerica usa L2 (numero porte). Fonti consultate il 9 ottobre: [Microsoft ST_Xstring](https://learn.microsoft.com/en-us/openspecs/office_standards/ms-oi29500/d34ae755-c53f-4a44-a363-c6dd3ee018a4) e [W3C XML 1.0](https://www.w3.org/TR/xml/#charsets). Apertura Excel nativa non eseguita.

## Remediation AUD-59 — Tile Windows prima del decode

Le tile Windows verificano 256x256 con ImageReader.getWidth/getHeight prima di read(0), con stream in memoria e reader.dispose in finally. Conservato limite 2 MiB anche per fetch iniettato. Baseline DesktopDocumentAndCartographyTest verde; finale TileDecodeTest + suite precedente: BUILD SUCCESSFUL in 5s, 6 test, zero fallimenti/errori/saltati. Sorgente HTTP locale della sessione chiusa al termine: tile valida, PNG 4096x4096 sotto il limite compresso, malformato e risposta oltre 2 MiB. Un PNG con solo header sovradimensionato verifica il rifiuto dimensioni prima dei pixel. Fonte [Oracle ImageReader](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/ImageReader.html), consultata il 9 ottobre. Nessun download dal servizio pubblico né OOM provocato.

## Remediation AUD-60 — Errori planimetria PDF Windows espliciti

Planimetrie richieste ma illeggibili o senza payload interrompono PDF/stampa con errore; rimosse le due conversioni silenziose in sfondo assente. Documento/stream chiusi anche al fallimento. Sfondo assente, escluso dal filtro o sezione disattivata restano validi. Baseline DeliveryPdfTest/ReportPdfTest/FloorMediaTest verde; regressioni FloorPlanPdfFailureTest includono immagini/PDF corrotti, payload mancante dopo svuotamento del cache isolato, immagine e PDF validi, filtri e assenza. Dieci test, zero errori/fallimenti/skips: `:pc:app:test --tests '*FloorPlanPdfFailureTest*' --tests '*DeliveryPdfTest*' --tests '*ReportPdfTest*' --tests '*FloorMediaTest*' --no-parallel --max-workers=1`, BUILD SUCCESSFUL in 16s. Corretti un errore di compilazione della fixture e una prima simulazione che manteneva il payload nel cache. Fonti PDFBox 3 consultate il 9 ottobre. Interazione nativa stampa/focus resta RES-23.

## Remediation AUD-61 — Pieghe esplicite conservate

Rimossa l’euristica LEGACY_DEFAULT e la costante orfana: ogni punto intermedio salvato è una piega. Default a due punti ancora rettilineo. Baseline MapSceneTest verde; dopo modifica MapSceneTest e PackageSerializerTest verdi, DesktopStorageTest verde alla riprova con stato di protezione esplicito, ProjectRepositoryTest verde dopo correzione import della fixture (BUILD SUCCESSFUL in 21s). Verificati coordinate .2/.5/.8, estremi spostati e risalvataggio, working copy semplice/protetta, Room e .ofam semplice/protetto; stesso contenuto e ID. Nessuna migrazione Room/.ofam. Documentazione mappa aggiornata.

## Remediation AUD-64 — Cancellazione e fusione bloccate per ambiti referenziati

Policy conservativa confermata dall’utente: guard prima di cancellazione sede, cestino/sostituzione apparato e fusione di entrambi gli apparati quando referenziati da ambito VLAN/subnet. Nessuna rete modificata automaticamente, nessun record di cestino creato al rifiuto. Errori it/en/es; gestione rifiuto in inventario/mappa Windows e dialoghi sostituzione/fusione. NetworkScopeRetentionTest verifica entrambe le sorgenti VLAN/subnet, entrambi i dispositivi, sede vuota referenziata, target estraneo eliminabile, rimozione esplicita, ripristino e .ofam semplice/protetto. ProjectCommandTest verifica errori, Room/inventario/cestino invariati e riprova. Gradle mirato: BUILD SUCCESSFUL in 29s. NetworkScopeUiTest: messaggio Compose di cancellazione, zero callback di save/trash, riprova; fusione Windows semplice/protetta preserva file e undo/cestino, retry/undo riusciti: BUILD SUCCESSFUL in 7s. Baseline ha rilevato vecchia aspettativa di cancellazione VLAN referenziata in ProjectEditsTest; sostituita con rifiuto e disconnessione esplicita conformi ad AUD-63. Documentazione dominio aggiornata. Focus/matrice nativa resta RES-19/23.

## Remediation AUD-62 — Serializer senza consumatori eliminato

Ricerca completa dei consumatori conferma che DeviceModelSerializer era usato soltanto dal proprio test: entrambi eliminati. Modelli e fixture utili restano nel flusso del progetto. Baseline PackageSerializerTest/DeviceModelSerializerTest verde (BUILD SUCCESSFUL in 3s); dopo rimozione PackageSerializerTest e tutte le prove Demo verdi (BUILD SUCCESSFUL in 45s). Regressione .ofam semplice/protetto confronta l’intero progetto con modello, ID, metadati, template porte, PoE, hardware/layout/override e campi extra. Nessun consumer applicativo residuo, contratto/schema invariato. Documentazione dominio aggiornata; fonti dello scambio ZIP già consultate.

## Chiusura della remediation AUD-52–64 — 9 ottobre 2026

Completati e rimossi tutti i 13 task del secondo audit, inclusi AUD-63/64 emersi dalla validazione rete. Tracker con `remainingTasks=[]`; restano RES-13/19/23/24, **4 residui (0 P1 / 3 P2 / 1 P3)**. Documentazione dominio, workflow, export, mappa e linee guida allineata; fonti primarie nelle sezioni pertinenti.

Verifica completa: **679 test, zero fallimenti/errori/saltati**: core 130, exchange 115, Windows 292, Android JVM 142. Core/exchange verdi nella prima esecuzione completa. Questa aveva quattro fallimenti Windows: tre casi della stessa fixture interop con numero VLAN al posto dell’UUID, e un selettore Compose che trovava due messaggi ora visibili. Fixture corretta con `vlan.id`, assert del messaggio nel dialogo specifico, diagnostica spostata negli assert; nessuna validazione indebolita. Riprova mirata verde in 33s, incluso wizard protetto Android con guasto, invii rapidi e riprova senza duplicati.

Finale `.\gradlew.bat :pc:app:test :mobile:app:testDebugUnitTest :pc:app:assemble :mobile:app:assembleDebug --no-parallel --max-workers=1` → **BUILD SUCCESSFUL in 7m 2s**. Build Desktop e APK debug Android riuscite; nessuna installazione, rigenerazione portable o qualificazione hardware. Report XML iniziali falliti e finali conservati in `build/reports/tracker-followup-20261009`, riepilogo `verification.json` con hash APK.

RES-19/23 includono esplicitamente i controlli nativi di wizard, rinomina, rifiuti di riferimenti rete e sfondi PDF. RES-13 mantiene gli scatti rinviati e i limiti hardware; RES-24 conserva tutte le pulizie storiche respinte, senza ritentativi. Nessun commit/push richiesto o eseguito.
Controlli conclusivi: 21 Markdown e 221 collegamenti locali validi; 54 file modificati/nuovi verificati UTF-8 senza BOM. JSON, ID, riferimenti e dipendenze del tracker validi; `git diff --check` superato. Rimosso soltanto l’helper creato per questa sessione `build/tmp/task_updates_20261009.py`; nessuna pulizia storica ritentata. Diff completo rivisto, nessun lockfile/vendor/migrazione applicata modificato. Evidenza `build/reports/tracker-followup-20261009/document-validation.json`.
