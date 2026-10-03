# Architettura del Sistema e Moduli

Data: 2 ottobre 2026

## Panoramica Architetturale

OnlyFieldAssetManager è un editor offline per censire e documentare infrastrutture di networking e telecomunicazioni. L'applicazione adotta un'architettura multi-modulo in Kotlin/JVM per garantire il massimo riutilizzo di codice tra l'app mobile Android e l'editor desktop Windows.

### Configurazione Moduli Gradle

```
OnlyFieldAssetManager/
├── mobile/app/       # Application module Android (Compose, Room, SAF, Camera, Stampa)
├── pc/app/           # Application module Windows Desktop (Compose Desktop, Java2D, Printing)
└── shared/
    ├── core/         # Modello di dominio e motore di validazione (Pura JVM)
    └── exchange/     # Serializzazione ZIP .ofam, cifratura, OpenXML XLSX, Markdown (Pura JVM)
```

1. **`:shared:core`**:
   - Pura libreria Kotlin/JVM a zero dipendenze UI o Android.
   - Definizione di tutte le entità di dominio (`Project`, `Device`, `Rack`, `Cable`, `Vlan`, ecc.).
   - Etichette QR proprie (`core.scan.LabelCode`, contenuto `ofam://<progetto>/<tipo>/<id>`) riconosciute da `CodeLookup`.
   - Ricerca di un codice scansionato (`core.scan.CodeLookup`): numero di serie, etichetta, alias e nome degli apparati, codice dei cavi, etichetta delle porte (corrispondenza esatta, senza maiuscole/minuscole).
   - Motore di validazione del modello (`ModelValidator`), separazione tra errori strutturali e avvisi documentali.
   - Procedura guidata «Nuovo sito» (`core.onboarding.NewSiteWizard`): passi progetto/cliente → sede → prima area → primo apparato (saltabile) → password (facoltativa), con validazione e creazione via `ProjectEdits`. Android la mostra come schermata (`NewSiteScreen`), Windows come finestra a passi; la password viene applicata da ciascuna app con il proprio meccanismo.

2. **`:shared:exchange`**:
   - Pura libreria Kotlin/JVM dipendente solo da `:shared:core`.
   - Serializzazione e deserializzazione del pacchetto ZIP `.ofam` v1.8 (`PackageSerializer`); i pacchetti 1.7 restano leggibili.
   - Cifratura simmetrica AES-256-GCM / PBKDF2.
   - Verifica della password di progetto (`PasswordHasher`): PBKDF2-HMAC-SHA256 con salt casuale da 16 byte e 600.000 iterazioni (OWASP Password Storage Cheat Sheet), formato `pbkdf2-sha256$<iterazioni>$<salt>$<hash>`; gli hash SHA-256 senza salt delle versioni precedenti sono accettati e ricalcolati al primo sblocco riuscito.
   - Foglio etichette QR A4 (`LabelSheetPdf`, ZXing core) condiviso da Android e Windows.
   - Fusione a tre vie all'import (`ProjectMerger`): il progetto è scomposto in elementi identificati per id (dati del progetto, business unit, sedi, aree, apparati con le loro porte, ogni voce delle altre liste) e confrontato con la base. Una modifica fatta da una parte sola viene applicata da sé; un elemento cambiato in entrambe le copie è un conflitto da risolvere con «tieni mio» o «tieni importato». Senza base ogni differenza è un conflitto. Gli elementi rimasti senza genitore (es. apparato spostato in una business unit eliminata dall'altra parte) vengono tenuti sotto la prima business unit.
   - Base della fusione = ultima istantanea sincronizzata, cioè la copia che l'altro dispositivo ha visto per ultima: si salva a ogni export (progetto esportato) e a ogni import (progetto del pacchetto). Android la tiene nella tabella `sync_snapshots` del DB cifrato (v11); Windows in `data/sync/<id>.ofam`, cifrato con la password del progetto se protetto.
   - Generatori di documentazione espostabile: OpenXML XLSX (`XlsxExportManager`), Markdown (`MarkdownExportManager`), confronto semantico (`ProjectComparison`).

3. **`:mobile:app`**:
   - Applicazione Android Jetpack Compose (minSdk 34, compileSdk 37, targetSdk 35).
   - Persistenza locale autonoma con Room Database (`AppDatabase`) e migrazioni (v1 -> v11; la 9 -> 10 aggiunge il numero di serie, la 10 -> 11 la base di sincronizzazione).
   - Livello dati diviso per area dietro la facciata `ProjectRepository`: `ProjectStore` (lettura/scrittura dell'albero del progetto), `DocumentExports` (PDF, Excel, Markdown, stampa), `PackageExchange` (pacchetti `.ofam` e file allegati), `InventorySearch`, `TrashOperations` (cestino, sostituzione, fusione, modifica in blocco), `CableTracer`; mapper Room ↔ dominio in `data.repository.mappers` (struttura, media, cablaggio, rete, alimentazione).
   - Database cifrato con SQLCipher (`EncryptedDatabase`): chiave casuale da 256 bit, conservata in `no_backup/db_key.bin` cifrata con una chiave AES-GCM del Keystore Android; il DB in chiaro delle versioni precedenti viene convertito una sola volta all'avvio (`sqlcipher_export`).
   - Backup automatico e trasferimento tra dispositivi disattivati (`allowBackup="false"`, `data_extraction_rules.xml`): i progetti escono dal telefono solo con l'export `.ofam`.
   - Scansione QR/barcode (`BarcodeScanner`): CameraX `LifecycleCameraController` + `MlKitAnalyzer` con il modello ML Kit incluso nell'APK (funziona offline); foto con l'app fotocamera di sistema e `FileProvider`.
   - Mappe su richiesta (`CartographicMapManager`, «Allegati › Mappa…»): unico uso della rete, con il permesso `INTERNET` dichiarato solo per questo; scarica 3 × 3 tessere attorno a un punto e le salva come allegato immagine con attribuzione. Senza rete l'app spiega che la mappa non si scarica e continua a funzionare offline. Fonti: OpenTopoMap (predefinita), CARTO Positron/Voyager.
   - Generazione report PDF composti e adattatore per la stampa Android (`ProjectPrintDocumentAdapter`).

4. **`:pc:app`**:
   - Applicazione Windows Desktop portabile basata su Compose Multiplatform Desktop (1.7.3) e JetBrains Runtime JDK 21.
   - Gestore storage esplicito (`DesktopStorageManager`) con salvataggio atomico e blocco file concorrente (`.lock`).

## Toolchain e Dipendenze

- **JDK Daemon Gradle:** JetBrains Runtime JDK 21
- **Gradle:** 9.7.1
- **Android Gradle Plugin (AGP):** 9.4.1
- **Kotlin / Compose Plugin:** 2.4.20
- **Room:** 2.8.5
- **CameraX:** 1.6.2 · **ML Kit barcode-scanning (modello incluso):** 17.3.0
- **SQLCipher for Android:** 4.19.1 (`net.zetetic:sqlcipher-android`, richiede `androidx.sqlite` 2.7 e quindi compileSdk ≥ 36)
- **Compose Multiplatform (Desktop):** 1.12.1
- **Min SDK / Target SDK / Compile SDK (Android):** 34 / 35 / 37
