# Test di Unità, Benchmarks e Volumi di Carico

Data: 3 ottobre 2026

## Suite di Test Unitari

L’ultima suite completa ha eseguito 166 test unitari/Compose sui quattro moduli. Le prove Android QA registrate sono 4: i 3 casi `FloorNativeTest` sono stati ripetuti per MAP03, mentre `FloorGestureNativeTest` resta una prova separata.

```
Ultima suite JVM/Compose: 166
├── :mobile:app        27
├── :shared:core       46
├── :shared:exchange   32
└── :pc:app            61
```

### Copertura dei Test per Modulo
- **`:shared:core` (`ModelValidatorTest`, `NewSiteWizardTest`, `CodeLookupTest`, `CoreModuleTest`)**: Validazione completa di tutte le entità, errori strutturali, avvisi documentali, sovrapposizioni slot rack, cicli di alimentazione, derivazione badge e passi della procedura «Nuovo sito».
- **`:shared:exchange` (`PackageSerializerTest`, `DocumentExportTest`, `FixtureTest`, `DeviceModelSerializerTest`, `PasswordHasherTest`, `ContractVersionTest`, `LabelSheetPdfTest`, `ProjectMergerTest`)**: Round-trip di pacchetti `.ofam` v1.7 liberi e cifrati AES-GCM, verifica checksum SHA-256, hash PBKDF2 delle password di progetto e migrazione degli hash SHA-256, test di integrità OpenXML XLSX, Markdown e fixtures sintetiche.
- **`:mobile:app` (`ProjectRepositoryTest`, `CartographicMapManagerTest`, `PilotBenchmarkTest`, `EncryptedDatabaseTest`, `ObjectMapStorageTest`)**: Persistenza Room, riconoscimento del DB in chiaro da convertire, migrazioni fino a v13, repository, base di sincronizzazione, numero di serie, contenimento, gestione tessere cartografiche offline e benchmark pilota.
- **`:pc:app` (`DesktopStorageTest`, `DesktopToolchainTest`, `DesktopDomainLogicTest`, `DesktopDocumentAndCartographyTest`, `BidirectionalInteropTest`, `MasterDetailTest`)**: Gestore storage desktop, pannello laterale degli editor (test UI Compose), salvataggio atomico, blocco `.lock`, password, interoperabilità bidirezionale Android ↔ Windows, UI rack elevation, cablaggio, rete logica, alimentazione A/B, badge documentali e cartografia.

## Verifiche di manutenzione (3 ottobre 2026)

- **RES-08:** baseline `:pc:app:test --tests '*MasterDetailTest'` verde; 4 scenari finali: chiusura, editor con titolo identico, inventario reale (cambio elemento/creazione/scarto sullo stesso elemento/salvataggio) e tab interne. La bozza resta finché si conferma lo scarto; i campi nascosti restano al salvataggio.
- **MAP01:** `DesktopMapDownloadTest`, 5 casi: mosaico e attribuzione, tessera non valida, HTTP 503 e servizio irraggiungibile, coordinate limite e zoom, riapertura offline e trasferimento cifrato con byte e metadati invariati. Le risposte di rete dei test sono servite localmente; nessun dato di prova nel codice di produzione.
- **Download reale:** nove tessere OpenTopoMap a Roma (12,4964; 41,9028; zoom 15), PNG 768 × 800, 645.178 byte; immagine e attribuzione ispezionate. Script e immagine temporanei rimossi dopo la verifica.
- **Build:** suite dei quattro moduli, APK debug e pacchetto portable Windows compilati con successo. Il collaudo manuale dell'interazione cartografica nell'eseguibile Windows non è stato eseguito; verificati backend reale, persistenza, scambio e compilazione UI.
- **Residui:** RES-11 per deprecazioni della toolchain, RES-13 per prove hardware manuali e RES-14 per il cestino cifrato Windows.

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

## RES-10: protezione delle bozze Windows

`DetailChangeTest` verifica cancellazione dell’uscita, scarto con azione eseguita una sola volta e protezione di navigazione, undo, nuovo sito e chiusura. Suite Windows: 57 test, BUILD SUCCESSFUL; portable ricostruito.

## MAP03: contenitori, persistenza e mappa

- `ObjectHierarchyTest`: conversione di `rackId`, annidamento, cicli, spostamento della radice, distacco, eliminazione/ripristino con U e proiezione/aggregazione dei cavi interni ed esterni.
- `ContainmentExchangeTest` e `ContractVersionTest`: round-trip 1.10 cifrato, pacchetto 1.9 invariato all’import, conflitto di genitore sul figlio e rifiuto di un ciclo.
- `ObjectMapStorageTest`: migrazione Room 12 → 13, riapertura, conversione dell’associazione rack e conservazione del cestino durante salvataggi successivi.
- `ContainerUiTest` e `FloorMapUiTest`: navigazione multilivello, lettura dei figli aggiornati, assegnazione/rimozione, scarto di una bozza annidata, linea aggregata e scelta del cavo reale, incluso indicatore interno.
- Telefono Android API 36, app QA separata: `FloorNativeTest` ha superato 3 casi, compresi backup SQLCipher e ripristino delle basi 11 e 12 fino a Room 13. Il test di gesture precedente copre clic e trascinamento, non multitouch manuale.

Le prove manuali non eseguite restano RES-13: foto/QR da fotocamera reale, multitouch e lettore USB fisico.
