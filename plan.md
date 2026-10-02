# OnlyFieldAssetManager — Piano Operativo e Fasi di Sviluppo

Data aggiornamento: 2 ottobre 2026

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
