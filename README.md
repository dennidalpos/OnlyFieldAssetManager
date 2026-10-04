# OnlyFieldAssetManager

Editor offline per censire e documentare infrastrutture di rete e telecomunicazioni.

## Piattaforme

- Android 14+ (`:mobile:app`): Compose, Room cifrato, fotocamera, scansione e stampa.
- Windows 11 x64 (`:pc:app`): Compose Desktop portable; dati in `data/` accanto all'eseguibile.
- Comune: `:shared:core` contiene dominio e regole; `:shared:exchange` gestisce scambio e documenti.

## Documentazione

- [Architettura](docs/01-architecture.md)
- [Dominio e contratto `.ofam`](docs/02-domain-data-contract.md)
- [Documenti ed export](docs/03-export-and-documents.md)
- [Storage e interoperabilita](docs/04-desktop-storage-interop.md)
- [Verifica](docs/05-testing-and-benchmarks.md)
- [Build e rilascio](docs/06-release-and-delivery.md)
- [Flussi utente](docs/07-workflows.md)
- [Mappa e planimetrie](docs/08-floor-map.md)
- [Localizzazione](docs/09-localization.md)
- [Configuratore](docs/10-object-configurator.md)

Le decisioni e i limiti di prodotto sono in [plan.md](plan.md); le evidenze e i residui sono in [roadmap.md](roadmap.md) e [PROJECT_STATUS.json](PROJECT_STATUS.json).
