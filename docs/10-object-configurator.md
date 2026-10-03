# Configuratore grafico degli oggetti

Data: 3 ottobre 2026

## Configurazione condivisa

Android e Windows compilano le stesse sorgenti Compose di `shared/configurator`. Creazione e modifica da mappa, inventario e rack usano `MapObjectDraft`; hardware, riconciliazione delle porte, geometria e tracciamento restano nel core JVM. I campi non mostrati vengono conservati tramite copia dell'entità esistente.

Rack: altezza U, profondità esterna, profondità utile di montaggio, numerazione e montaggio. I suggerimenti sono modificabili e includono i valori già censiti. Le U indicano disponibilità e apparati installati, con apertura della relativa configurazione. Apparati: dimensioni, altezza, budget PoE, alimentazione ridondante, caratteristiche e gruppi numerati di porte. Ogni gruppo dichiara lato, mezzo, connettore, velocità, ruolo, PoE e risorsa combo; il modulo ottico installato è un dato separato della porta.

Per pannelli e prese nuovi si possono generare coppie fronte/retro con `PanelMapping` interno. Gli oggetti precedenti non ricevono lati o passaggi inventati. Oggetti senza porte mantengono uno schema del corpo; tipologie personalizzate e campi extra restano disponibili. Per i cavi si configurano estremità reali, mezzo, connettori, caratteristiche, lunghezza e segmenti di percorso condivisi.

Lo schema usa pulsanti di porta, selezione evidenziata, zoom e scorrimento. Le porte hanno nome accessibile e descrizione dello stato; il colore è accompagnato da testo. I controlli Compose sono raggiungibili da tastiera. La prova Compose del flusso completo verifica clic, selezione della destinazione, ritorno all’oggetto e salvataggio conservando gli ID.

## Collegamenti e continuità

`ConnectionGraph` è il motore comune e sostituisce il precedente tracciatore Android. Attraversa cavi, passaggi interni e riferimenti legacy, evitando di ripercorrere la tratta d'ingresso; cicli, ambiguità e riferimenti mancanti interrompono il percorso.

| Stato | Significato |
|---|---|
| Disponibile | Nessun cavo esterno occupa l'attacco. Un passaggio interno non lo occupa. |
| Verde / Percorso completo | Due dispositivi terminali sono uniti da un percorso censito completo. |
| Ambra / Percorso incompleto | Estremità non definita, passaggio sconosciuto, tratta interrotta o dato da verificare. |
| Rosso / Conflitto | Occupazione multipla, ciclo, ambiguità, riferimento mancante o conflitto documentato. |

Il verde indica continuità documentale, senza misure del traffico. La destinazione può restare non definita e il cavo può essere salvato. I selettori mostrano le porte effettive, filtrano BU/piano/rack/apparato e propongono attacchi liberi. La risorsa combo è condivisa fra le porte alternative. Un nuovo collegamento non sostituisce implicitamente un cavo occupante; una connessione identica viene riutilizzata. Differenze di connettore sono avvisi documentali. Un passaggio interno può essere lasciato sconosciuto; le permutazioni si assegnano ad attacchi liberi, liberando esplicitamente le precedenti assegnazioni. I dati importati ambigui restano segnalati come conflitto.

La configurazione delle porte si apre in una sottovista e consente etichetta, modulo, caratteristiche, destinazione, consultazione delle tratte, passaggi interni e apertura/creazione di apparati intermedi. Le operazioni annidate sono accumulate in una `ConfigurationSession`: il comando finale del contenitore UI le salva come una sola modifica annullabile. Annullare una sottoconfigurazione conserva il contesto precedente.

## Modelli e compatibilità

`DeviceModel` copre apparati, rack e cavi. La ricerca e la selezione del modello sono separate dal nome dell'istanza. «Salva come modello» conserva hardware, gruppi, disposizione e campi extra condivisibili; esclude posizione, seriale, IP/MAC, collegamenti e credenziali. I valori dei menu provengono dai preset e dal progetto e sono deduplicati senza riscrivere i testi personalizzati. I suggerimenti dei campi extra sono circoscritti al tipo di oggetto/campo/tipo di dato e alla classificazione, evitando di suggerire valori riservati in campi condivisibili.

Modificare il catalogo non aggiorna le istanze. L'applicazione esplicita mostra il numero delle porte aggiunte/rimosse e delle rimozioni con collegamenti. Le porte riconosciute per nome e lato conservano ID, etichetta, osservazioni, modulo e personalizzazioni individuali. Rimuovere porte collegate richiede una scelta esplicita; i cavi conservano ID e documentazione, con estremità da verificare. La scelta rimuove anche gli eventuali riferimenti VLAN/PoE/LAG delle porte eliminate, evitando riferimenti incoerenti. Le modifiche restano annullabili.

Formato corrente `.ofam` **1.11**, lettura **1.7–1.10** conservata. Hardware e modelli sono inclusi anche nella cifratura, nel confronto e nella fusione. Room **14** aggiunge colonne per rack/modelli/cavi e JSON hardware per apparati/porte con migrazione 13 → 14; `EncryptedDatabase` effettua checkpoint e backup cifrato prima dell'aggiornamento. È necessario aggiornare entrambe le app per lo scambio dei nuovi dati.

XLSX aggiunge dimensioni, budget e caratteristiche hardware all'inventario e una tabella delle porte con lato, connettore, velocità, ruolo/PoE, modulo, stato, destinazione e passaggio interno. Markdown documenta hardware, profondità rack e porte. Le restrizioni sui dati riservati restano applicate.

## Verifiche della sessione

Baseline del core e dello scambio superata prima delle modifiche. `ConfiguratorTest` verifica gruppi 24 rame + 4 SFP, connessione diretta/idempotente, disponibilità degli attacchi posteriori, catena con due pannelli e cavallotto, cicli, conflitti, combo, aggiornamento esplicito, conservazione degli ID e delle personalizzazioni, isolamento della sessione e contenimento annidato.

`ConfiguratorExchangeTest` verifica round-trip normale/cifrato, fusione e contenuti XLSX/Markdown. `ConfiguratorStorageTest` verifica round-trip Room 14 e migrazione da schema 13 conservando ID, porte e collegamenti senza inferire lati o passaggi. Lo schema 14 è generato da KSP. Build APK debug, APK delle prove native e portable Windows riuscite. Suite completa: 206 test (64 core, 37 exchange, 77 Windows, 28 Android JVM), zero fallimenti/errori/test saltati; nessuna deprecazione rilevata. Comando e risultati finali nella [roadmap](../roadmap.md).

Due prove Compose verificano 28 azioni di porta, clic e stati leggibili. La prova `selectingDestinationStagesConnectionAndSaveKeepsPortIds` passa: la porta viene portata nell’area visibile prima del clic, usando le coordinate non ritagliate come nella API Compose. La scheda porta è una sottovista con ritorno all’oggetto. La suite ha inoltre rilevato e permesso di correggere la segnalazione delle modifiche al contenitore UI: resta attiva la conferma prima di scartare la bozza su entrambe le app. Nessuna asserzione disabilitata. `ConfiguratorUndoTest` verifica salvataggio cifrato, riapertura e annullamento unico di rack, due switch e cavo. Nessuna prova manuale su telefono reale né esecuzione delle prove strumentali Android in questa sessione.

## Fonti consultate

- [Triton RMA](https://triton-racks.com/products/data-cabinets/free-standing-cabinets/rma/): altezze e profondità di armadi di catalogo.
- [APC NetShelter SX](https://www.apc.com/us/en/product/AR3100B2/taa-baacots-apc-netshelter-sx-server-rack-gen-2-42u-1991h-x-600w-x-1070d-mm-with-sides-black/): riferimento per rack 42U e profondità esterna 1070 mm; la profondità utile va censita separatamente.
- [HPE Aruba Instant On 1930](https://www.hpe.com/psnow/doc/a50002592enw): gruppi distinti di porte rame e uplink SFP, incluso il caso 24 + 4.
- [Semantica Compose](https://developer.android.com/develop/ui/compose/accessibility/semantics): azioni e descrizioni accessibili.
- [Grafica Compose](https://developer.android.com/develop/ui/compose/graphics/draw/overview): disegno parametrico.
- [Kotlin integrato in AGP](https://developer.android.com/build/migrate-to-built-in-kotlin): inclusione delle sorgenti Kotlin condivise nel source set Android.
- [Valori predefiniti Room](https://developer.android.com/reference/androidx/room/ColumnInfo): default JSON dichiarati nelle entità e nella migrazione per ottenere lo stesso schema sui database nuovi e aggiornati.
