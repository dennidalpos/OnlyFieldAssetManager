# OnlyFieldAssetManager

Editor offline per censimento e documentazione di infrastrutture di networking e telecomunicazioni.

## Piattaforme e Moduli
- **Android 14+ (`:mobile:app`)**: App mobile Compose con persistenza Room cifrata (SQLCipher), scansione QR/barcode, acquisizione fotocamera/mappe, export e stampa.
- **Windows 11 x64 (`:pc:app`)**: Editor portable Compose Desktop: `dist\OnlyFieldAssetManager\OnlyFieldAssetManager.exe`, dati in `data\` accanto all'eseguibile (`.\gradlew.bat :pc:app:packagePortable`).
- **Moduli Comuni (`:shared:core`, `:shared:exchange`)**: Modello dati pure JVM, operazioni di modifica e form condivisi tra le due app (`ProjectEdits`, `core.forms`), etichette localizzate (`core.display`, `core.i18n`), procedura guidata «Nuovo sito» (`core.onboarding`), ricerca dei codici scansionati (`core.scan`), motore di validazione, contenitori annidati, serializzazione pacchetti ZIP `.ofam` v1.11 (legge anche 1.7–1.10), cifratura AES-256-GCM / PBKDF2, fusione a tre vie all'import, foglio etichette QR ed esportazione OpenXML XLSX / Markdown.

## Documentazione di Dominio
- [01-architecture.md](docs/01-architecture.md) — Architettura del sistema, moduli e toolchain
- [02-domain-data-contract.md](docs/02-domain-data-contract.md) — Modello dati, formato pacchetto `.ofam` v1.11, cifratura e validazione
- [03-export-and-documents.md](docs/03-export-and-documents.md) — Esportazione XLSX, Markdown, PDF composti, Stampa e privacy
- [04-desktop-storage-interop.md](docs/04-desktop-storage-interop.md) — Storage Desktop, salvataggio atomico e interoperabilità Android ↔ PC
- [05-testing-and-benchmarks.md](docs/05-testing-and-benchmarks.md) — Suite, migrazioni e limiti delle prove
- [06-release-and-delivery.md](docs/06-release-and-delivery.md) — Comandi di build, programma portable Windows e artefatti di rilascio
- [07-ux-audit.md](docs/07-ux-audit.md) — Audit critico UI/UX e interventi di rework

- [10-object-configurator.md](docs/10-object-configurator.md) — Configurazione hardware, schemi interattivi, disponibilità e modelli
- [09-localization.md](docs/09-localization.md) — Lingue, preferenze, cataloghi e documenti
- [08-floor-map.md](docs/08-floor-map.md) — Navigazione BU/piano, catalogo, foto, mappa e PDF multipagina

## Stato del Progetto
- **Fase Android (A00–A13)**: Completata al 100% (14 step su 14) con validazione Room DB, export e stampa.
- **Fase Windows (W00–W05)**: Completata al 100% (6 step su 6) con Compose Desktop, storage atomico, interoperabilità bidirezionale e pacchetto portable x64.
- **Rework UX (U01–U02)**: Audit critico e rework dell'interfaccia di entrambe le app (navigazione, selettori al posto degli ID, conferme, eseguibile portable).
- **Fase v1.1 (S/O/R/F)**: Completata: DB Android cifrato, password PBKDF2, procedura «Nuovo sito», editor a pagina intera/pannello laterale, tema scuro, foto, scansione QR/barcode, etichette QR, fusione all'import, mappe su richiesta.
- **Manutenzione**: RES-08 risolto; cartografia Windows con download OpenTopoMap, anteprima e salvataggio negli allegati; CARTO rimosso da entrambe le app (RES-09).
- **Navigazione e mappa (MAP02/MAP03)**: progetto → BU → piano; contenitori annidati, sottomodali, cavi aggregati per radice e selezione del cavo reale; sfondi immagine/PDF offline; migrazione Android 12 → 13 e formato 1.10.
- **Configuratore grafico**: schema parametrico, porte e destinazioni, modelli di rack/apparati/cavi, motore di continuità comune; Room 14 e scambio 1.11. [Funzionamento e verifiche](docs/10-object-configurator.md).
- **Stato Complessivo**: evidenze e risultati delle prove sono in `roadmap.md`; soltanto lavoro aperto in `PROJECT_STATUS.json`.

## Manutenzione del 3 ottobre 2026

Chiusi RES-15/16 (blocco concorrente e base di fusione), RES-14 (cestino locale cifrato), RES-11 (deprecazioni) e RES-02 (italiano/inglese/spagnolo per UI e documenti). Evidenze aggiornate in [roadmap](roadmap.md); solo attività aperte nel [tracker](PROJECT_STATUS.json).
