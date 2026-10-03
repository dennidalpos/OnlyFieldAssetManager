# OnlyFieldAssetManager — Piano Operativo e Fasi di Sviluppo

Data aggiornamento: 3 ottobre 2026

## 1. Contesto Operativo e Moduli

Editor offline per tecnici di networking e telecomunicazioni.

- **`shared/core/`**: Modello di dominio, regole di validazione, senza dipendenze Android UI.
- **`shared/exchange/`**: Serializzazione pacchetti `.ofam` v1.7, cifratura AES-GCM, OpenXML XLSX, Markdown.
- **`mobile/app/`**: App Android 14+ Jetpack Compose, Room DB, fotocamera, mappe offline, stampa.
- **`pc/app/`**: Editor Windows 11 x64 Compose Desktop, storage esplicito, salvataggio atomico e blocco `.lock`.

## 2. Sintesi delle Fasi di Sviluppo

### Fase Android (A00–A13) — COMPLETATA (14/14)
- **A00 [COMPLETATO]**: Struttura multi-modulo, Gradle wrapper e Compose minima.
- **A01 [COMPLETATO]**: Modello core e contratto pacchetto `.ofam` v1.0.
- **A02 [COMPLETATO]**: Persistenza Room, DAO, Repository e ricerca inventario.
- **A03 [COMPLETATO]**: Export/import SAF, valutatore di confronto pacchetti.
- **A04 [COMPLETATO]**: Credenziali integrate, cifratura PBKDF2/AES-GCM e gestione password.
- **A05 [COMPLETATO]**: Armadi rack, modelli apparati e export PDF scheda rack.
- **A06 [COMPLETATO]**: Planimetrie, foto, allegati media e annotazioni grafiche.
- **A07 [COMPLETATO]**: Cablaggio fisico, percorsi condivisi e mapping pannelli.
- **A08 [COMPLETATO]**: Rete logica, VLAN, CIDR subnet, SVI L3, LAG, WAN/VPN, videosorveglianza, campi extra.
- **A09 [COMPLETATO]**: Alimentazione A/B, PDU, UPS, PoE e derivazione badge.
- **A10 [COMPLETATO]**: Cestino locale, undo, sostituzione apparati, fusione duplicati e batch edit.
- **A11 [COMPLETATO]**: Esportazione XLSX OpenXML (`t="inlineStr"`), Markdown, PDF composti e Stampa Android.
- **A12 [COMPLETATO]**: Acquisizione mappe cartografiche offline con attribuzione (OpenTopoMap/CARTO).
- **A13 [COMPLETATO]**: Pilota Android (100 apparati), benchmarking e consegna contratto v1.7.

### Fase Windows (W00–W05) — COMPLETATA (6/6)
- **W00 [COMPLETATO]**: Configurazione `:pc:app`, Compose Desktop 1.7.3, JDK 21 e integrazione shared core/exchange.
- **W01 [COMPLETATO]**: Storage Desktop, salvataggio atomico, blocco concorrente `.lock`, dialoghi password e verifica fixtures Android.
- **W02 [COMPLETATO]**: Adattamento UI Desktop (mouse/tastiera), vista rack elevation 2D, modelli, planimetrie canvas, media, cestino e modifiche batch.
- **W03 [COMPLETATO]**: Porting Desktop di cablaggio fisico, percorsi condivisi, permutazioni, rete logica, VLAN, CIDR subnet, SVI L3, LAG, WAN/VPN, videosorveglianza, configurazioni, campi extra, alimentazione A/B, PoE e badge documentali.
- **W04 [COMPLETATO]**: Anteprima e stampa nativa Windows 11 (`PrinterJob`), esportazione XLSX/PDF/Markdown, cartografia Desktop e risoluzione anomalie UI Android/Desktop (`UI-01` .. `UI-07`).
- **W05 [COMPLETATO]**: Collaudo finale di interoperabilità bidirezionale Android ↔ Windows e pacchettizzazione portable x64 (`BidirectionalInteropTest`, `createDistributable`, 73 unit test passati).

### Fase v1.1 (S/O/R/F) — PIANIFICATA (0/14)
Ordine di esecuzione: Sicurezza → Avvio → Refactor UI → Funzioni. Utente tipo: tecnico singolo con telefono; il PC serve per il censimento completo e i documenti.
- **S01 [DA FARE]** (Sicurezza) Database Android cifrato: Room con SQLCipher (SupportOpenHelperFactory), chiave casuale protetta dal Keystore Android; migrazione una tantum del DB in chiaro v9; allowBackup="false" e dataExtractionRules che escludono database e allegati. *Accettazione:* Il file del database non è leggibile senza chiave; i progetti esistenti si aprono dopo l'aggiornamento.
- **S02 [DA FARE]** (Sicurezza) Password del progetto con PBKDF2 e salt: Sostituire hashPassword SHA-256 (ProjectRepository) con la derivazione PBKDF2 di :shared:exchange; gli hash vecchi vengono ricalcolati al primo sblocco riuscito. *Accettazione:* Test su verifica password, migrazione degli hash e password errata.
- **O01 [DA FARE]** (Avvio) Procedura guidata "Nuovo sito" condivisa: Passi e stato in :shared:core (core.onboarding, creazione via ProjectEdits): progetto/cliente → sede (BU) → prima area → primo apparato (saltabile) → password (facoltativa). Sostituisce il dialog Nuovo progetto su Android e AppDialog.NewProject su Windows. *Accettazione:* Stessa procedura su entrambe le app; alla fine si apre la home del progetto.
- **O02 [DA FARE]** (Avvio) Schermata iniziale chiara: Senza progetti: due azioni grandi "Inizia un nuovo sito" e "Apri un pacchetto ricevuto (.ofam)" con una riga di spiegazione; con progetti: "Continua: «ultimo progetto»" in cima. Android ProjectsScreen, Windows WelcomeCard. *Accettazione:* Al primo avvio un utente nuovo arriva in un progetto in pochi tocchi, senza istruzioni esterne.
- **O03 [DA FARE]** (Avvio) Home progetto Android: ricerca e azioni rapide: Barra di ricerca in alto (nome, IP, etichetta, alias; InventoryDao.searchDevices), azioni rapide Aggiungi apparato / Foto (F01) / Scansiona (F02); sotto scheda di controllo e griglia sezioni. *Accettazione:* Un apparato si trova dalla home senza entrare in Inventario.
- **R01 [DA FARE]** (Refactor UI) Android: modifiche a pagina intera: Componente EditScreen (barra Annulla/Salva, imePadding, rememberSaveable, conferma su modifiche non salvate) e route per entità in Screen; conversione per area: Inventario, Sedi/Rack, Cablaggio, Rete/Alimentazione, Media. Dialog solo per conferme, password e scelte brevi. *Accettazione:* Nessun FormDialog per gli editor di entità; rotazione e Indietro non perdono i dati inseriti.
- **R02 [DA FARE]** (Refactor UI) Windows: pannello laterale (master-detail): Componente MasterDetail: lista con ricerca a sinistra, editor a destra con core.forms, Ctrl+S / Esc, avviso su modifiche non salvate. Restano dialog: documenti, password, validazione, confronto import. *Accettazione:* Nessun FormDialog nelle sezioni; i form continuano a preservare i campi nascosti.
- **R03 [DA FARE]** (Refactor UI) Icone Material e tema scuro: Icone Material al posto delle emoji (SectionTile Android, AppSection Windows); schemi Material3 chiaro e scuro (Android segue il sistema, Windows opzione nel menu); eliminati i colori fissi. *Accettazione:* Entrambe le app leggibili in tema chiaro e scuro, senza emoji nella navigazione.
- **R04 [DA FARE]** (Refactor UI) Refactor repository Android: Dividere ProjectRepository (~1.100 righe) ed EntityMappers (~950) per area funzionale, senza cambiare comportamento. *Accettazione:* Tutti i test esistenti passano invariati.
- **F01 [DA FARE]** (Funzioni) Foto dalla fotocamera: Scatto con ActivityResultContracts.TakePicture e FileProvider nella cartella allegati del progetto; collegamento automatico all'apparato, rack o area aperti; le foto viaggiano nel .ofam. *Accettazione:* Una foto scattata dalla scheda di un apparato compare tra i suoi allegati e arriva sul PC.
- **F02 [DA FARE]** (Funzioni) Scansione QR/barcode: CameraX + ML Kit barcode con modello incluso (offline). Il codice cerca tra seriali, etichette, alias, etichette di cavi e porte, oppure compila il numero di serie. Nuovo campo serialNumber (ed etichette cavi/porte se mancano): contratto .ofam v1.8 con default nulli, pacchetti 1.7 leggibili. Windows: lettore USB come tastiera nel campo di ricerca. *Accettazione:* La scansione di un seriale noto apre l'apparato; i pacchetti 1.7 si importano senza errori.
- **F03 [DA FARE]** (Funzioni) Etichette QR proprie: QR con contenuto ofam://<progetto>/<tipo>/<id> e foglio etichette PDF (PdfExportManager Android, SimplePdfWriter Windows); la scansione apre la scheda. *Accettazione:* Un'etichetta stampata e scansionata apre l'entità corretta.
- **F04 [DA FARE]** (Funzioni) Fusione all'import con scelta per elemento: Fusione a tre vie in :shared:exchange estendendo ProjectComparisonEvaluator (caso DIVERGENT), con base = ultima istantanea sincronizzata salvata a ogni export/import. Modifiche senza conflitto applicate da sole; per ogni conflitto "tieni mio" / "tieni importato"; senza base ogni differenza è un conflitto. UI su entrambe le app. *Accettazione:* Due copie modificate in parti diverse si fondono senza perdite; i conflitti sono mostrati uno per uno.
- **F05 [DA FARE]** (Funzioni) Mappe su Android (chiude RES-07): Permesso INTERNET usato solo per scaricare le mappe su richiesta esplicita; riattivare la UI di CartographicMapManager con messaggio chiaro senza rete; documentare che l'app resta offline-first. *Accettazione:* Download delle mappe funzionante con rete; senza rete l'app funziona e spiega perché la mappa non si scarica.
