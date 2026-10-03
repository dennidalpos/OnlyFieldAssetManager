# OnlyFieldAssetManager — Piano Operativo e Fasi di Sviluppo

Data aggiornamento: 3 ottobre 2026

## 1. Contesto Operativo e Moduli

Editor offline per tecnici di networking e telecomunicazioni.

- **`shared/core/`**: Modello di dominio, regole di validazione, senza dipendenze Android UI.
- **`shared/exchange/`**: Serializzazione pacchetti `.ofam` v1.8 (legge la v1.7), fusione all'import, etichette QR, cifratura AES-GCM, OpenXML XLSX, Markdown.
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

### Fase v1.1 (S/O/R/F) — COMPLETATA (14/14)
- **S01 [COMPLETATO]**: Database Android cifrato con SQLCipher, chiave protetta dal Keystore, migrazione del DB in chiaro, backup disattivato.
- **S02 [COMPLETATO]**: Password di progetto con PBKDF2-HMAC-SHA256 e salt; hash SHA-256 legacy ricalcolati al primo sblocco.
- **O01 [COMPLETATO]**: Procedura guidata «Nuovo sito» condivisa (`core.onboarding`) su Android e Windows.
- **O02 [COMPLETATO]**: Schermata iniziale con azioni grandi e «Continua: «ultimo progetto»».
- **O03 [COMPLETATO]**: Home progetto Android con ricerca apparati e azioni rapide (Aggiungi apparato, Foto, Scansiona).
- **R01 [COMPLETATO]**: Editor a pagina intera su Android (`EditScreen`), rotazione e Indietro senza perdita di dati.
- **R02 [COMPLETATO]**: Editor nel pannello laterale su Windows (`MasterDetailHost`/`EditPanel`), Ctrl+S / Esc.
- **R03 [COMPLETATO]**: Icone Material Symbols e tema scuro su entrambe le app.
- **R04 [COMPLETATO]**: Livello dati Android diviso per area dietro `ProjectRepository`; mapper per area.
- **F01 [COMPLETATO]**: Foto dalla fotocamera collegate ad apparato, rack, area o progetto.
- **F02 [COMPLETATO]**: Scansione QR/barcode offline (CameraX + ML Kit), numero di serie (contratto 1.8), lettore USB su Windows.
- **F03 [COMPLETATO]**: Etichette QR `ofam://` e foglio etichette PDF condiviso.
- **F04 [COMPLETATO]**: Fusione a tre vie all'import con scelta per elemento.
- **F05 [COMPLETATO]**: Mappe su Android scaricate su richiesta (chiude RES-07).
