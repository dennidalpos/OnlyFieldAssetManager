# OnlyFieldAssetManager

Editor offline per censimento e documentazione di infrastrutture di networking e telecomunicazioni.

## Piattaforme e Moduli
- **Android 14+ (`:mobile:app`)**: App mobile Compose con persistenza Room cifrata (SQLCipher), scansione QR/barcode, acquisizione fotocamera/mappe, export e stampa.
- **Windows 11 x64 (`:pc:app`)**: Editor portable Compose Desktop: `dist\OnlyFieldAssetManager\OnlyFieldAssetManager.exe`, dati in `data\` accanto all'eseguibile (`.\gradlew.bat :pc:app:packagePortable`).
- **Moduli Comuni (`:shared:core`, `:shared:exchange`)**: Modello dati pure JVM, operazioni di modifica e form condivisi tra le due app (`ProjectEdits`, `core.forms`), etichette italiane (`core.display`), procedura guidata «Nuovo sito» (`core.onboarding`), ricerca dei codici scansionati (`core.scan`), motore di validazione, serializzazione pacchetti ZIP `.ofam` v1.8 (legge anche la v1.7), cifratura AES-256-GCM / PBKDF2, fusione a tre vie all'import, foglio etichette QR ed esportazione OpenXML XLSX / Markdown.

## Documentazione di Dominio
- [01-architecture.md](docs/01-architecture.md) — Architettura del sistema, moduli e toolchain
- [02-domain-data-contract.md](docs/02-domain-data-contract.md) — Modello dati, formato pacchetto `.ofam` v1.8, cifratura e validazione
- [03-export-and-documents.md](docs/03-export-and-documents.md) — Esportazione XLSX, Markdown, PDF composti, Stampa e privacy
- [04-desktop-storage-interop.md](docs/04-desktop-storage-interop.md) — Storage Desktop, salvataggio atomico e interoperabilità Android ↔ PC
- [05-testing-and-benchmarks.md](docs/05-testing-and-benchmarks.md) — Suite di test (117 unit test) e benchmark di carico pilota
- [06-release-and-delivery.md](docs/06-release-and-delivery.md) — Comandi di build, programma portable Windows e artefatti di rilascio
- [07-ux-audit.md](docs/07-ux-audit.md) — Audit critico UI/UX e interventi di rework

## Stato del Progetto
- **Fase Android (A00–A13)**: Completata al 100% (14 step su 14) con validazione Room DB, export e stampa.
- **Fase Windows (W00–W05)**: Completata al 100% (6 step su 6) con Compose Desktop, storage atomico, interoperabilità bidirezionale e pacchetto portable x64.
- **Rework UX (U01–U02)**: Audit critico e rework dell'interfaccia di entrambe le app (navigazione, selettori al posto degli ID, conferme, eseguibile portable).
- **Fase v1.1 (S/O/R/F)**: Completata: DB Android cifrato, password PBKDF2, procedura «Nuovo sito», editor a pagina intera/pannello laterale, tema scuro, foto, scansione QR/barcode, etichette QR, fusione all'import, mappe su richiesta.
- **Stato Complessivo**: 36/36 step completati (v1.0 e fase v1.1); 117/117 unit test passati. Dettagli in `roadmap.md`.
