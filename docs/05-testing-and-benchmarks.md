# Test di Unità, Benchmarks e Volumi di Carico

Data: 2 ottobre 2026

## Suite di Test Unitari

L'intero progetto include 73 test unitari eseguiti e superati con successo su tutti e 4 i moduli:

```
Total Passed Unit Tests: 73
├── :mobile:app        (20 passed)
├── :shared:core       (12 passed)
├── :shared:exchange   (12 passed)
└── :pc:app            (29 passed)
```

### Copertura dei Test per Modulo
- **`:shared:core` (`ModelValidatorTest`, `NewSiteWizardTest`, `CodeLookupTest`, `CoreModuleTest`)**: Validazione completa di tutte le entità, errori strutturali, avvisi documentali, sovrapposizioni slot rack, cicli di alimentazione, derivazione badge e passi della procedura «Nuovo sito».
- **`:shared:exchange` (`PackageSerializerTest`, `DocumentExportTest`, `FixtureTest`, `DeviceModelSerializerTest`, `PasswordHasherTest`, `ContractVersionTest`, `LabelSheetPdfTest`, `ProjectMergerTest`)**: Round-trip di pacchetti `.ofam` v1.7 liberi e cifrati AES-GCM, verifica checksum SHA-256, hash PBKDF2 delle password di progetto e migrazione degli hash SHA-256, test di integrità OpenXML XLSX, Markdown e fixtures sintetiche.
- **`:mobile:app` (`ProjectRepositoryTest`, `CartographicMapManagerTest`, `PilotBenchmarkTest`, `EncryptedDatabaseTest`)**: Persistenza Room, riconoscimento del DB in chiaro da convertire, migrazioni DB v1->v11, repository, base di sincronizzazione, numero di serie, gestione tessere cartografiche offline e benchmark pilota.
- **`:pc:app` (`DesktopStorageTest`, `DesktopToolchainTest`, `DesktopDomainLogicTest`, `DesktopDocumentAndCartographyTest`, `BidirectionalInteropTest`, `MasterDetailTest`)**: Gestore storage desktop, pannello laterale degli editor (test UI Compose), salvataggio atomico, blocco `.lock`, password, interoperabilità bidirezionale Android ↔ Windows, UI rack elevation, cablaggio, rete logica, alimentazione A/B, badge documentali e cartografia.

## Benchmarks di Carico Pilota (`PilotBenchmarkTest`)

### Volume di Carico Testato
- 1 Business Unit con 100 Apparati di rete (2.400 porte complessive)
- 5 Armadi Rack (42U)
- 20 VLAN e 5 Gruppi Credenziali
- 50 Cavi di collegamento
- Sfondo cartografico offline con attribuzione

### Risultati delle Prestazioni Misurate
| Operazione | Tempo Rilevato | Soglia Richiesta | Esito |
| :--- | :--- | :--- | :--- |
| **Validazione Strutturale Modello** | ~18 ms | < 500 ms | Superato |
| **Esportazione Pacchetto Cifrato (.ofam v1.7)** | ~120 ms | < 2.000 ms | Superato |
| **Importazione e Decifratura Pacchetto** | ~110 ms | < 2.000 ms | Superato |
| **Ricerca Inventario (Nome, IP, Etichetta)** | ~15 ms | < 500 ms | Superato |
| **Memoria Heap Occupata** | < 45 MB | Non critico | Stabile |
