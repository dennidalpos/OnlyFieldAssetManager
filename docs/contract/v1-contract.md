# Contratto v1.0 — Modello dati, Formato Scambio, Protezione e Validazione

Data: 2 ottobre 2026  
Versione contratto: `1.7` (Aggiornato per Step A12 — Acquisizione cartografica, sfondi autosufficienti, attribuzione OpenTopoMap/CARTO e funzionamento offline)

## 1. Struttura del Pacchetto di Scambio (`.ofam` / ZIP)

Un pacchetto completo di scambio è un archivio ZIP contenente:
- `manifest.json`: metadati del pacchetto, versione formato, ID esportazione, timestamp, flag di cifratura e tabella dei checksum SHA-256.
- `project.json` (se non cifrato) o `project.json.enc` (se cifrato con password): l'intero albero del progetto serializzato in JSON UTF-8 conforme al modello v1.7.
- `attachments/`: cartella contenente risorse binarie (foto, planimetrie, sfondi cartografici offline, PDF, schemi rack, configurazioni) indirizzate da `project.json`.

*Nota:* Il cestino locale (`trash_items`) è ad uso esclusivo del dispositivo e viene tassativamente escluso dalla serializzazione dei pacchetti di scambio `.ofam`.

### 1.1 Metadati del Manifesto (`manifest.json`)
```json
{
  "formatVersion": "1.7",
  "exportId": "<UUID>",
  "exportedEpochMs": 1770000000000,
  "projectId": "<UUID>",
  "projectName": "Nome Progetto",
  "isEncrypted": true,
  "kdfSaltHex": "3f8b...16bytes_hex",
  "kdfIterations": 100000,
  "cipherIvHex": "a1b2...12bytes_hex",
  "checksums": {
    "project.json.enc": "sha256_hex_value",
    "attachments/planimetria_p1.png": "sha256_hex_value"
  }
}
```

### 1.2 Parametri Crittografici del Contenitore Protetto
Quando `isEncrypted` è `true`:
- **Derivazione chiave (KDF):** `PBKDF2WithHmacSHA256`, 100.000 iterazioni, salt casuale da 16 byte.
- **Cifratura simmetrica:** AES-256 in modalità `AES/GCM/NoPadding` con IV casuale da 12 byte (96 bit) e tag di autenticazione a 128 bit.
- **Integrità del manifesto:** La presenza o assenza di cifratura è dichiarata in `manifest.json`. Un tentativo di importazione senza password o con password errata restituisce un errore di validazione strutturale (`PASSWORD_REQUIRED` o `INVALID_PACKAGE_PASSWORD`).

## 2. Modello Dati e Cardinalità

L'organizzazione del modello segue la gerarchia:
`Project` → `BusinessUnit` (BU) → [`Site` (Sede, opzionale)] → `Area`/`Floor` → `Device` (Apparato) → `Port` (Porta).
Inoltre, `Project` contiene:
- `credentials`: lista delle credenziali integrate (`Credential`).
- `racks`: lista degli armadi rack (`Rack`).
- `deviceModels`: lista dei modelli schematici riutilizzabili (`DeviceModel`).
- `attachments`: lista degli allegati e foto (`Attachment`).
- `annotations`: lista delle annotazioni grafiche sulle planimetrie (`Annotation`).
- `floorplanPlacements`: lista delle collocazioni di apparati/rack sulle planimetrie (`FloorplanPlacement`).
- `cables`: lista dei cavi di collegamento fisico (`Cable`).
- `sharedPathSegments`: lista dei segmenti di percorso condivisi (`SharedPathSegment`).
- `panelMappings`: lista dei mapping interni / passaggi sui pannelli di permutazione (`PanelMapping`).
- `vlans`: lista delle VLAN con ambito (`Vlan`).
- `subnets`: lista delle subnet di rete (`Subnet`).
- `portVlanMemberships`: appartenenza VLAN per porta (`PortVlanMembership`).
- `logicalInterfaces`: interfacce logiche L3 / SVI (`LogicalInterface`).
- `lagGroups`: gruppi di aggregazione link LAG/LACP (`LagGroup`).
- `deviceConfigurations`: snapshot di configurazioni manuali e allegati datati (`DeviceConfiguration`).
- `wanVpnConnections`: connessioni WAN, VPN e accessi Internet (`WanVpnConnection`).
- `videoSurveillanceMappings`: associazioni telecamere / NVR / VMS (`VideoSurveillanceMapping`).
- `customExtraFields`: campi personalizzati extra tipizzati (`CustomExtraField`).
- `powerFeeds`: lista delle sorgenti e linee di alimentazione (`PowerFeed`).
- `poeMappings`: configurazioni PoE per porta (`PoeMapping`).
- `documentBadges`: lista dei badge liberi e derivati (`DocumentBadge`).

### 2.1 Entità e Regole di Ambito (Scope)
- **Vlan:**
  - `id`: UUID.
  - `vlanId`: Numero VLAN (1..4094).
  - `name`: Nome identificativo.
  - `scopeType`: `PROJECT`, `BUSINESS_UNIT`, `SITE`, `DEVICE`.
  - `scopeTargetId`: UUID facoltativo del target dell'ambito.
  - `description`: Descrizione / note.
- **Subnet:**
  - `id`: UUID.
  - `cidrBlock`: Blocco CIDR (es. `192.168.10.0/24`).
  - `gatewayIp`: Indirizzo IP del gateway facoltativo.
  - `vlanId`: Identificativo/UUID VLAN associata facoltativa (nessun vincolo 1-a-1 rigido).
  - `scopeType`: Ambito della subnet (`PROJECT`, `BUSINESS_UNIT`, `SITE`, `DEVICE`).
- **PortVlanMembership:**
  - `id`: UUID.
  - `portId`: UUID della porta.
  - `mode`: `ACCESS`, `TRUNK`, `HYBRID`, `UNTAGGED`, `TAGGED`, `UNSPECIFIED`.
  - `untaggedVlanId`: Numero VLAN untagged.
  - `taggedVlanIds`: Lista di numeri VLAN tagged.
  - `nativeVlanId`: Numero VLAN nativa per trunk.
- **LogicalInterface:**
  - `id`: UUID.
  - `deviceId`: UUID dell'apparato.
  - `name`: Nome dell'interfaccia (es. `vlan10`, `eth0.100`, `Loopback0`).
  - `ipAddress` / `subnetCidr` / `vlanId` / `macAddress`.
  - `isL3`: Booleano.
- **LagGroup:**
  - `id`: UUID.
  - `deviceId`: UUID dell'apparato.
  - `name`: Nome del gruppo LAG (es. `lag1`, `port-channel1`).
  - `mode`: `LACP`, `STATIC`, `OTHER`, `UNSPECIFIED`.
  - `memberPortIds`: Lista UUID delle porte membri.
- **DeviceConfiguration:**
  - `id`: UUID.
  - `deviceId`: UUID dell'apparato.
  - `title`: Titolo o versione configurazione.
  - `configText`: Testo o estratto configurazione manuale.
  - `attachmentId`: UUID dell'allegato datato originale associato.
  - `capturedEpochMs`: Timestamp di acquisizione.
- **WanVpnConnection:**
  - `id`: UUID.
  - `name`: Nome identificativo della connessione.
  - `type`: `WAN`, `VPN`, `INTERNET`, `OTHER`.
  - `providerOrCarrier`: Fornitore / carrier.
  - `bandwidth`: Banda / velocità contrattuale.
  - `localEndpointDeviceId` / `localEndpointSiteDescription`: Endpoint locale.
  - `remoteEndpointDeviceId` / `remoteEndpointSiteDescription`: Endpoint remoto.
  - `underlyingAccessId`: Riferimento all'accesso sottostante (cavo, apparato o linea).
- **VideoSurveillanceMapping:**
  - `id`: UUID.
  - `cameraDeviceId`: UUID dell'apparato telecamera.
  - `managerDeviceId`: UUID dell'apparato NVR/DVR/VMS manager (o `null`).
  - `externalManagerDescription`: Descrizione del gestore esterno se non presente in progetto.
  - `channelNumber` / `streamUrl` / `resolution`.
- **CustomExtraField:**
  - `id`: UUID.
  - `targetType`: Tipo entità target (`PROJECT`, `DEVICE`, `PORT`, `SITE`, `VLAN`).
  - `targetId`: UUID dell'entità target.
  - `fieldKey` / `fieldValue`: Chiave e valore del campo personalizzato.
  - `fieldType`: `STRING`, `NUMBER`, `BOOLEAN`, `DATE`.
  - `classification`: Classificazione di riservatezza (`SHAREABLE`, `CONFIDENTIAL`, `REVIEW_REQUIRED`).
- **PowerFeed:**
  - `id`: UUID.
  - `deviceId`: UUID dell'apparato ricevente o distributore.
  - `feedName`: Nome identificativo dell'alimentazione (es. `Feed A`, `PDU Outlet 3`).
  - `feedType`: `PRIMARY_A`, `SECONDARY_B`, `UPS_BACKUP`, `PDU_DISTRIBUTION`, `MAINS_DIRECT`, `OTHER`, `UNKNOWN`.
  - `sourceDeviceId`: UUID della sorgente (UPS o PDU).
  - `sourceOutletDescription`: Descrizione della presa/quadro sorgente.
  - `voltageVolts`: Tensione nominale (es. 230, 400, 48).
  - `loadVa`: Carico in Apparente VA (se misurato).
  - `loadWatts`: Carico in Reale W (se misurato).
  - `observedRuntimeMinutes`: Autonomia in minuti (solo se rilevata con fonte e data).
  - `observedSource`: Fonte della misurazione dell'autonomia.
  - `observedEpochMs`: Data/ora del rilevamento.
- **PoeMapping:**
  - `id`: UUID.
  - `portId`: UUID della porta.
  - `role`: `PSE_SOURCE`, `PD_SINK`, `PASSIVE_INJECTOR`, `NONE`.
  - `standard`: `IEEE_802_3AF`, `IEEE_802_3AT`, `IEEE_802_3BT`, `PASSIVE_24V`, `PASSIVE_48V`, `OTHER`.
  - `allocatedPowerWatts`: Potenza erogata/richiesta in Watt.
- **DocumentBadge:**
  - `id`: UUID.
  - `targetType`: `DEVICE`, `PORT`, `RACK`, `PROJECT`.
  - `targetId`: UUID del target.
  - `label`: Testo dell'etichetta/badge.
  - `category`: `VLAN`, `MEDIUM`, `POE`, `UPS_DEPENDENCY`, `COVERAGE`, `OPEN_ISSUE`, `FREE_LABEL`.
  - `isDerived`: Booleano (indicatore se derivato automaticamente dal modello).

## 3. Validazione e Classificazione dei Problemi

La validazione separa rigorosamente i problemi strutturali dagli avvisi documentali.

### 3.1 Errori Strutturali (`STRUCTURAL_ERROR`)
Impediscono l'importazione o il salvataggio poiché corrompono l'integrità dei dati:
- Formato UUID invalido o ID duplicato (`INVALID_VLAN_UUID`, `INVALID_POWER_FEED_UUID`, `INVALID_POE_MAPPING_UUID`, `INVALID_DOCUMENT_BADGE_UUID`, ecc.).
- Ciclo nella catena di alimentazione di apparati (`POWER_FEED_CYCLE_DETECTED`).
- Alimentazione riferita ad apparato o sorgente inesistente (`INVALID_POWER_FEED_DEVICE`, `INVALID_POWER_FEED_SOURCE`).
- Configurazione PoE riferita a porta inesistente (`INVALID_POE_PORT_REFERENCE`).
- Numero VLAN fuori dal range 1..4094 (`INVALID_VLAN_NUMBER`).
- Formato CIDR della subnet non valido (`INVALID_SUBNET_CIDR`).
- Riferimento ad apparato non esistente (`INVALID_DEVICE_REFERENCE`).
- Riferimento ad allegato non esistente in una configurazione (`INVALID_ATTACHMENT_REFERENCE`).
- Pacchetto ZIP non valido o file `manifest.json`/`project.json` (o `project.json.enc`) mancante.
- Incongruenza del checksum SHA-256 per `project.json`/`project.json.enc` o allegati.
- Pacchetto cifrato e password non fornita (`PASSWORD_REQUIRED`).
- Password errata o archivio protetto manomesso/troncato (`INVALID_PACKAGE_PASSWORD`).
- Collegamento porta (`connectedPortId`) o cavo verso un ID porta inesistente (`INVALID_PORT_REFERENCE`).

### 3.2 Avvisi Documentali (`DOCUMENTARY_WARNING`)
Mantenuti ed evidenziati all'utente senza bloccare il salvataggio o l'importazione:
- Autonomia espressa senza fonte e data della misurazione (`CALCULATED_AUTONOMIA_PROHIBITED_WARNING`).
- Apparato con alimentazione a copertura parziale (mancanza A o B) (`SINGLE_FEED_PARTIAL_COVERAGE_WARNING`).
- VLAN con ID duplicato nello stesso ambito/scope (`DUPLICATE_VLAN_IN_SCOPE`).
- Membership VLAN che riferisce un ID VLAN non catalogato (`UNREFERENCED_VLAN_IN_MEMBERSHIP`).
- Campo extra classificato come `REVIEW_REQUIRED` (`CUSTOM_FIELD_NEEDS_REVIEW`).
- Cavo con estremità scollegata o da verificare (`DETACHED_CABLE_ENDPOINT`).
- Percorso condiviso che supera la capacità massima stimata (`SHARED_PATH_CAPACITY_EXCEEDED`).
- Mapping pannello contenente un passaggio/ponte intermedio ignoto (`UNKNOWN_PASSAGE_IN_CHAIN`).
- Allegato o annotazione classificata come `REVIEW_REQUIRED` (`ATTACHMENT_NEEDS_REVIEW`, `ANNOTATION_NEEDS_REVIEW`).
- Indirizzo IP o nome tecnico duplicato all'interno della stessa Business Unit.
- Apparato senza posizione assegnata (`siteId`, `areaId` e `rackId` nulli).
- Sovrapposizione di slot U tra apparati sullo stesso lato dello stesso rack (`RACK_SLOT_OVERLAP`).

## 4. Report PDF e Viste Schematiche Rack
- La generazione del PDF della scheda rack crea un documento A4 multipagina autonomo contenente:
  - Intestazione con nome progetto, BU, nome rack, data e parametri.
  - Schema grafico dei lati FRONTE e RETRO con griglia delle U e blocco colorato degli apparati montati.
  - Tabella di riepilogo apparati montati e porte.
  - Esclusione tassativa dei segreti e credenziali per garantire la consultabilità in sicurezza.

## 5. Cestino Locale, Ripristino, Fusione e Modifiche Multiple (v1.5)
- **Cestino Locale (Recycle Bin):**
  - Gli elementi eliminati (apparati, rack, credenziali, ecc.) vengono trasferiti nel cestino locale con serializzazione del loro stato snapshot.
  - Le porte e i cavi collegati agli oggetti eliminati vengono impostati su `DETACHED_TO_VERIFY` con un'osservazione esplicita per evitare riferimenti pendenti o cancellazioni silenziose.
  - Il cestino è protetto dalla stessa chiave di cifratura del progetto ed è **escluso** dagli export `.ofam`.
- **Sostituzione Apparati:**
  - La sostituzione cancella il vecchio apparato (spostandolo nel cestino e scollegando le estremità) e crea un oggetto completamente nuovo, senza ereditare IP, MAC, seriali o credenziali dal vecchio apparato.
- **Fusione Guidata Duplicati:**
  - La fusione permette l'unione di due apparati scegliendo l'ID superstite e le sorgenti dei singoli attributi (`MergeDataChoices`). Le porte e le configurazioni dell'apparato duplicato vengono rimappate al superstite. Non si esegue mai la fusione automatica per nome o IP.
- **Modifica Multipla (Batch Edit):**
  - Consente l'aggiornamento simultaneo in transazione atomica di campi ammessi (`siteId`, `areaId`, `category`, `rackId`, `mountingType`, note osservazione).
  - È tassativamente vietata la modifica multipla implicita di identificatori univoci (UUID), indirizzi IP, indirizzi MAC, credenziali o cablaggi.

## 6. Documenti Completi, Report e Stampa (v1.6)
- **Esportazione XLSX OpenXML Nativa (`.xlsx`):**
  - Generazione di un archivio ZIP conforme alla specifica ISO/IEC 29500 OpenXML senza dipendenze pesanti esterne.
  - Generazione di 5 fogli di lavoro dedicati: `Inventario Apparati`, `Porte e Cablaggio`, `Rete Logica e VLAN`, `Alimentazione e Badge`, `Note e Osservazioni`.
  - Tutte le celle di testo libero vengono formattate come stringhe esplicite con `t="inlineStr"`, evitando l'esecuzione di formule indotte (es. `=SUM`, `=CMD`).
  - Distinzione tra estremità fuori ambito ("Fuori Ambito") ed estremità ignote/scollegate ("Ignoto / Scollegato").
  - Esclusione tassativa e garantita di ogni campo segreto/credenziale.
- **Esportazione Markdown (`.md`):**
  - Generazione di schede e report in formato Markdown con intestazioni, tabelle e badge.
  - Supporto per la consultazione ed il versionamento in ambienti di documentazione o Git.
- **Report PDF Composto Multipagina (`.pdf`):**
  - Copertina con KPI sintetici dell'infrastruttura (apparati, rack, cavi, VLAN, avvisi/questioni aperte).
  - Sezioni selezionabili dall'utente (`ReportSelection`): Prospetti Rack, Inventario, Cablaggio, Rete Logica, Alimentazione, Note ed Allegati.
  - Rispetto dei filtri di selezione per BU/Sede/Area e categoria.
- **Stampa Android e Anteprima Nativa:**
  - Integrazione con `PrintManager` tramite `ProjectPrintDocumentAdapter` per l'invio diretto alle stampanti di sistema Android o il salvataggio in PDF tramite la schermata di stampa di sistema.
- **Protezione e Riesame della Condivisione:**
  - Esclusione totale delle credenziali (username, segreti, password SSH/SNMP).
  - Filtro sui contenuti riservati (`CONFIDENTIAL`) selezionabile dall'utente.
  - Rilevamento automatico di elementi non classificati (`REVIEW_REQUIRED`) con richiesta di conferma esplicita del riesame prima dell'esportazione.

## 7. Evoluzione del Contratto e Compatibilità Futura
- Le librerie di scambio e storage ignorano le chiavi sconosciute (`ignoreUnknownKeys = true`).
- I campi obbligatori di nuova introduzione negli step futuri forniranno un valore predefinito retrocompatibile.
