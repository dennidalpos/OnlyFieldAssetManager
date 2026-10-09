# Residui del repository — 9 ottobre 2026

**Stato dopo la remediation del 9 ottobre:** AUD-45–51 completati e rimossi dal [tracker](../PROJECT_STATUS.json). Restano **RES-13/19/23/24: quattro attività, 0 P1 / 3 P2 / 1 P3**. Chiusure, regressioni e limiti nella [roadmap](../roadmap.md) e nelle sezioni remediation sotto.

L’audit iniziale identificava sette nuovi rilievi (un P1 e sei P2) oltre ai quattro residui operativi. Le riproduzioni e il perimetro iniziali sono conservati come evidenza storica; descrivono il codice precedente alle correzioni.

## Perimetro e metodo dell’audit iniziale

Inventariati tutti i **382 file versionati**, inclusi 309 sorgenti Kotlin, 161 applicativi in `src/main`. Quattro moduli Gradle e sorgenti UI condivisi in `shared/configurator`. Revisione trasversale di dominio/validazione/modifiche, persistenza e mapper, recupero, scambio/cifratura/media/import/fusione, editor e navigazione, mappe/configuratore, documenti, localizzazione, demo/fixture/test, build/CI/rilascio e documentazione. Approfonditi chiamanti, esiti di salvataggio e copertura dei punti sospetti; nessun segnaposto applicativo TODO/FIXME/HACK/NotImplemented individuato dalla ricerca.

Distinzione dell’evidenza: **riprodotto** con dati sintetici e toolchain del progetto; **da codice** per un contratto o percorso incompleto; **collaudo da eseguire** per telefono, EXE, stampante o hardware. Le ricerche e le prove mirate non certificano ogni riga, combinazione UI o dispositivo. Nessuna installazione, importazione nativa, pubblicazione o modifica di progetti utente; nessuna pulizia storica ritentata. Non è stata eseguita una nuova suite generale, né una campagna di vulnerabilità delle dipendenze.

Gli AUD precedenti sono confrontati con implementazione e chiusure, senza riaprire il loro contesto storico. AUD-44 riguarda Alimentazioni/PoE/Badge **Windows** già corretti: AUD-46 copre gli editor ancora esclusi. AUD-47 riguarda pagine/layout di stampa, senza riaprire le callback corrette da AUD-21. AUD-48 riguarda l’acquisizione prima del limite media, senza riaprire il rollback media già corretto.

## Attività ancora aperte

Ordine di remediation eseguito: AUD-45 → AUD-46 → AUD-49 → AUD-48 → AUD-47 → AUD-50 → AUD-51. Per i residui seguenti restano collaudi reali o pulizie storiche; non sono state ritentate le rimozioni precedentemente respinte.

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

### Verifica finale

643 test JVM/Compose e quattro prove native PDF verdi; APK compilate. Comandi, risultati e limiti nella roadmap e nella guida di verifica. Restano i quattro residui indicati sopra, senza nuovi task runtime. Packaging protegge i dati destinatari e rifiuta la rigenerazione con data/ sorgente; nessun recupero pendente per la vecchia fixture sintetica, come confermato dall’utente.

Pulizia finale della remediation: il controllo automatico ha respinto la rimozione dei soli build/tmp/remediation_docs.py, build/tmp/__pycache__/remediation_docs.cpython-313.pyc e della directory cache se vuota, con motivo blocked by policy. Nessuna rimozione eseguita o ritentata. Due file inventariati con dimensioni/SHA-256 in build/reports/tracker-remediation-20261009/cleanup-blocked-inventory.json; aggiunti a RES-24. I report restano conservati.
