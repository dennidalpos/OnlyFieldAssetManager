# Test di Unità, Benchmarks e Volumi di Carico

Data: 3 ottobre 2026

## Suite di Test Unitari

L'intero progetto include 149 test unitari/Compose sui 4 moduli e 3 test strumentali Android, eseguiti e superati (152 complessivi):

```
Total Passed Tests: 152
├── :mobile:app        (26 unit + 3 native passed)
├── :shared:core       (40 passed)
├── :shared:exchange   (28 passed)
└── :pc:app            (55 passed)
```

### Copertura dei Test per Modulo
- **`:shared:core` (`ModelValidatorTest`, `NewSiteWizardTest`, `CodeLookupTest`, `CoreModuleTest`)**: Validazione completa di tutte le entità, errori strutturali, avvisi documentali, sovrapposizioni slot rack, cicli di alimentazione, derivazione badge e passi della procedura «Nuovo sito».
- **`:shared:exchange` (`PackageSerializerTest`, `DocumentExportTest`, `FixtureTest`, `DeviceModelSerializerTest`, `PasswordHasherTest`, `ContractVersionTest`, `LabelSheetPdfTest`, `ProjectMergerTest`)**: Round-trip di pacchetti `.ofam` v1.7 liberi e cifrati AES-GCM, verifica checksum SHA-256, hash PBKDF2 delle password di progetto e migrazione degli hash SHA-256, test di integrità OpenXML XLSX, Markdown e fixtures sintetiche.
- **`:mobile:app` (`ProjectRepositoryTest`, `CartographicMapManagerTest`, `PilotBenchmarkTest`, `EncryptedDatabaseTest`)**: Persistenza Room, riconoscimento del DB in chiaro da convertire, migrazioni DB v1->v12, repository, base di sincronizzazione, numero di serie, gestione tessere cartografiche offline e benchmark pilota.
- **`:pc:app` (`DesktopStorageTest`, `DesktopToolchainTest`, `DesktopDomainLogicTest`, `DesktopDocumentAndCartographyTest`, `BidirectionalInteropTest`, `MasterDetailTest`)**: Gestore storage desktop, pannello laterale degli editor (test UI Compose), salvataggio atomico, blocco `.lock`, password, interoperabilità bidirezionale Android ↔ Windows, UI rack elevation, cablaggio, rete logica, alimentazione A/B, badge documentali e cartografia.

## Verifiche di manutenzione (3 ottobre 2026)

- **RES-08:** baseline `:pc:app:test --tests '*MasterDetailTest'` verde; 4 scenari finali: chiusura, editor con titolo identico, inventario reale (cambio elemento/creazione/scarto sullo stesso elemento/salvataggio) e tab interne. La bozza resta finché si conferma lo scarto; i campi nascosti restano al salvataggio.
- **MAP01:** `DesktopMapDownloadTest`, 5 casi: mosaico e attribuzione, tessera non valida, HTTP 503 e servizio irraggiungibile, coordinate limite e zoom, riapertura offline e trasferimento cifrato con byte e metadati invariati. Le risposte di rete dei test sono servite localmente; nessun dato di prova nel codice di produzione.
- **Download reale:** nove tessere OpenTopoMap a Roma (12,4964; 41,9028; zoom 15), PNG 768 × 800, 645.178 byte; immagine e attribuzione ispezionate. Script e immagine temporanei rimossi dopo la verifica.
- **Build:** suite dei quattro moduli, APK debug e pacchetto portable Windows compilati con successo. Il collaudo manuale dell'interazione cartografica nell'eseguibile Windows non è stato eseguito; verificati backend reale, persistenza, scambio e compilazione UI.
- **Residui:** RES-10 per le azioni globali Windows; RES-11 per deprecazioni della toolchain; RES-12 per il piano dettagliato precedente a v1.1.

## MAP02: verifiche della navigazione e della mappa

- Baseline dei quattro moduli: BUILD SUCCESSFUL, 125 test già verdi; conservate le modifiche presenti nel checkout.
- `NewSiteWizardTest`: elenchi multipli, obblighi minimi, BU vuote aggiuntive, password facoltativa e ID conservati.
- `ObjectMapTest`: geometria con margini/zoom/panoramica, isolamento, 100 posizioni iniziali distinte, campi nascosti, cavi fuori piano, eliminazione di apparato/rack/porta e punti non validi.
- `ObjectMapStorageTest`: persistenza Room delle nuove proprietà e migrazione 11 → 12 su database precedente separato, senza perdita di BU/apparati/seriali.
- `ContractVersionTest` e `ObjectMapExchangeTest`: lettura 1.7/1.8, round-trip 1.9 cifrato, foto dei cavi, tipologie/percorsi nella fusione e rifiuto delle versioni successive.
- `FloorMapUiTest`: clic, trascinamento con un salvataggio al rilascio, cavi selezionabili e punti liberi; aggiunte successive di BU/piani, ritorno agli elenchi, isolamento, ricerca/annullamento del catalogo e credenziali mascherate Windows.
- `FloorMediaTest`: PDF con proporzioni opposte, file illeggibile/protetto, pagina fuori intervallo, singolo PDF condiviso fra piani, sfondo rimosso senza perdita di coordinate, riapertura delle foto, rollback dei file se una seconda foto fallisce e foto dei cavi nello scambio cifrato.
- Telefono Android API 36, app QA separata: `FloorNativeTest` (2 test) verifica PdfRenderer multipagina e SQLCipher con migrazione, backup cifrato e ripristino; `FloorGestureNativeTest` (1 test) verifica clic e trascinamento del canvas Android. Dati sintetici in un archivio isolato, applicazioni QA rimosse dal runner.
- Ultima suite e build: `:shared:core:test :shared:exchange:test :mobile:app:testDebugUnitTest :pc:app:test :mobile:app:assembleDebug :pc:app:packagePortable`, BUILD SUCCESSFUL. Comandi strumentali in [06-release-and-delivery.md](06-release-and-delivery.md).
- Non verificati in questa consegna: scatto e QR con fotocamera reale, gesture multitouch manuali e lettore USB fisico. Le deprecazioni preesistenti della toolchain restano in RES-11.

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
