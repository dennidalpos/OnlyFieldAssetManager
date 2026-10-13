# OnlyFieldAssetManager

Editor per censire reti, apparati, cablaggi e sedi. I progetti restano locali; lo scambio fra Android 14+ e Windows 11 x64 avviene manualmente con pacchetti `.ofam` v1. Solo il download esplicito di una cartografia richiede rete.

## Per iniziare

- [Guida Android](docs/guide/android.md) e [guida Windows](docs/guide/windows.md).
- [Scambio e documenti](docs/guide/exchange-and-documents.md); [demo e verifiche](docs/testing/demo.md).
- [Decisioni di prodotto](plan.md), [stato aperto](PROJECT_STATUS.json) e [verifiche registrate](roadmap.md).

## Documentazione tecnica

| Ambito | Documento |
| --- | --- |
| Moduli e confini | [Architettura](docs/01-architecture.md) |
| Dati, validazione, `.ofam` | [Dominio e contratto](docs/02-domain-data-contract.md) |
| PDF, XLSX, Markdown, filtri | [Documenti](docs/03-export-and-documents.md) |
| Persistenza e recupero | [Storage e interoperabilità](docs/04-desktop-storage-interop.md) |
| Test e demo | [Verifica](docs/05-testing-and-benchmarks.md) |
| Distribuzione | [Build e rilascio](docs/06-release-and-delivery.md) |
| Operazioni applicative | [Flussi](docs/07-workflows.md) |
| Scene e planimetrie | [Mappa](docs/08-floor-map.md) |
| Lingue | [Localizzazione](docs/09-localization.md) |
| Oggetti e porte | [Configuratore](docs/10-object-configurator.md) |

Le [matrici Android](docs/testing/hardware-checklist.md) e [Windows](docs/testing/windows-native-matrix.md) distinguono le prove native ancora aperte dai controlli automatici. Il [materiale grafico](assets/README.md) è separato dalle istruzioni d'uso.

Il PDF `OnlyFieldAssetManager_Manuale_Utente.pdf` è una versione storica; per i flussi correnti usare le guide Android, Windows e scambio qui sopra.
