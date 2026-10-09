# Residui del repository — 9 ottobre 2026

Audit del checkout corrente, comprese le modifiche locali al demo e ad `AGENTS.md`. **Sette nuovi rilievi aperti: un P1 e sei P2**. Conservati i quattro residui operativi RES-13/19/23/24: totale **11 attività, 1 P1 / 9 P2 / 1 P3**. Nessun P0 identificato. Priorità e stato corrente nel [tracker](../PROJECT_STATUS.json); chiusure precedenti nella [roadmap](../roadmap.md). Questo audit registra attività, senza correggere il runtime.

## Perimetro e metodo

Inventariati tutti i **382 file versionati**, inclusi 309 sorgenti Kotlin, 161 applicativi in `src/main`. Quattro moduli Gradle e sorgenti UI condivisi in `shared/configurator`. Revisione trasversale di dominio/validazione/modifiche, persistenza e mapper, recupero, scambio/cifratura/media/import/fusione, editor e navigazione, mappe/configuratore, documenti, localizzazione, demo/fixture/test, build/CI/rilascio e documentazione. Approfonditi chiamanti, esiti di salvataggio e copertura dei punti sospetti; nessun segnaposto applicativo TODO/FIXME/HACK/NotImplemented individuato dalla ricerca.

Distinzione dell’evidenza: **riprodotto** con dati sintetici e toolchain del progetto; **da codice** per un contratto o percorso incompleto; **collaudo da eseguire** per telefono, EXE, stampante o hardware. Le ricerche e le prove mirate non certificano ogni riga, combinazione UI o dispositivo. Nessuna installazione, importazione nativa, pubblicazione o modifica di progetti utente; nessuna pulizia storica ritentata. Non è stata eseguita una nuova suite generale, né una campagna di vulnerabilità delle dipendenze.

Gli AUD precedenti sono confrontati con implementazione e chiusure, senza riaprire il loro contesto storico. AUD-44 riguarda Alimentazioni/PoE/Badge **Windows** già corretti: AUD-46 copre gli editor ancora esclusi. AUD-47 riguarda pagine/layout di stampa, senza riaprire le callback corrette da AUD-21. AUD-48 riguarda l’acquisizione prima del limite media, senza riaprire il rollback media già corretto.

## Priorità e ordine

P1: rischio di sovrascrittura dei dati; affrontare prima del packaging su un workspace già usato. P2: perdita di bozze, salvataggio, acquisizione e correttezza dei documenti. P3: pulizia verificata delle risorse sintetiche. Ordine suggerito: **AUD-45 → AUD-46 → AUD-49 → AUD-48 → AUD-47 → AUD-50 → AUD-51**, quindi completare i collaudi pertinenti. Le dipendenze di RES-19/23 riguardano la loro chiusura finale; i casi indipendenti possono proseguire.

| Priorità | ID | Attività | Evidenza attuale |
| --- | --- | --- | --- |
| P1 | AUD-45 | Escludere i dati del runtime sorgente dall’assemblaggio portable | Riprodotto con Gradle Sync 9.7.1 |
| P2 | AUD-46 | Conservare le bozze degli editor rete/cablaggio e moduli Android al save fallito | VLAN Windows riprodotta in dialogo e pannello; restante perimetro da codice |
| P2 | AUD-49 | Rifiutare numeri non finiti prima del salvataggio | NaN e Infinity riprodotti, form/modello accettano e serializer rifiuta |
| P2 | AUD-48 | Completare limiti e bordi dell’acquisizione cartografica Android | Risposta chunked oltre 2 MiB riprodotta; griglia/decode da codice |
| P2 | AUD-47 | Rispettare pagine e attributi della stampa Android | Da codice e contratto Android; stampa nativa non eseguita |
| P2 | AUD-50 | Eliminare la selezione legacy delle alimentazioni dal nome A/B nei documenti | Markdown e celle XLSX D2/E2 riprodotti |
| P2 | AUD-51 | Applicare escaping coerente al testo utente Markdown | Nome apparato con pipe rompe la riga di inventario |
| P2 | RES-13 | Fotocamera, scansione, multitouch e interruzioni improvvise | Parziale; normale riavvio già verificato, foto rinviate dall’utente |
| P2 | RES-19 | Matrice UX completa Android | Parziale; TalkBack, rotazione reale e tablet fisici restano aperti |
| P2 | RES-23 | Matrice EXE Windows e dialoghi nativi | Parziale; prove già concluse conservate |
| P3 | RES-24 | Pulizia delle sole risorse sintetiche inventariate | Parziale; precedenti rimozioni bloccate non ritentate |

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
