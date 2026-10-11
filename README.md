# OnlyFieldAssetManager

Editor offline per censire e documentare infrastrutture di rete e telecomunicazioni.

## Piattaforme

- **Android 14+** (`:mobile:app`): Compose, Room/SQLCipher, fotocamera, scansione e stampa.
- **Windows 11 x64** (`:pc:app`): Compose Desktop portable; dati in `data/` accanto all'eseguibile.
- **Moduli Condivisi**: `:shared:core` (dominio e regole), `:shared:exchange` (scambio `.ofam` e documenti) e `shared/configurator` (UI Compose).

## Documentazione di Dominio

- [01 - Architettura](docs/01-architecture.md)
- [02 - Dominio e Contratto `.ofam`](docs/02-domain-data-contract.md)
- [03 - Esportazione e Documenti](docs/03-export-and-documents.md)
- [04 - Storage Desktop e Interoperabilità](docs/04-desktop-storage-interop.md)
- [05 - Esecuzione Test e Pacchetto Demo](docs/05-testing-and-benchmarks.md)
- [06 - Build e Rilascio](docs/06-release-and-delivery.md)
- [07 - Flussi Utente e Interfaccia](docs/07-workflows.md)
- [08 - Mappa Visiva e Planimetrie](docs/08-floor-map.md)
- [09 - Localizzazione (i18n)](docs/09-localization.md)
- [10 - Configuratore Oggetti e Porte](docs/10-object-configurator.md)

## Matrici di Collaudo e Stato

- [Checklist Hardware Android](docs/testing/hardware-checklist.md)
- [Matrice Nativi Windows](docs/testing/windows-native-matrix.md)
- [Decisioni e Limiti Prodotto](plan.md)
- [Storico Completamenti ed Evidenze](roadmap.md)
- [Stato Attuale e Task Aperti](PROJECT_STATUS.json)
