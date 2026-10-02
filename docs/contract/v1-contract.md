# Contratto v1.0 — Modello dati, Formato Scambio e Validazione

Data: 2 ottobre 2026  
Versione contratto: `1.0`

## 1. Struttura del Pacchetto di Scambio (`.ofam` / ZIP)

Un pacchetto completo di scambio è un archivio ZIP contenente:
- `manifest.json`: metadati del pacchetto, versione formato, ID esportazione, timestamp e tabella dei checksum SHA-256.
- `project.json`: l'intero albero del progetto serializzato in JSON UTF-8 conforme al modello v1.0.
- `attachments/`: cartella contenente risorse binarie (foto, planimetrie, PDF, schemi rack) indirizzate da `project.json`.

### 1.1 Metadati del Manifesto (`manifest.json`)
```json
{
  "formatVersion": "1.0",
  "exportId": "<UUID>",
  "exportedEpochMs": 1770000000000,
  "projectId": "<UUID>",
  "projectName": "Nome Progetto",
  "checksums": {
    "project.json": "sha256_hex_value",
    "attachments/rack_1.png": "sha256_hex_value"
  }
}
```

## 2. Modello Dati e Cardinalità

L'organizzazione del modello segue la gerarchia:
`Project` → `BusinessUnit` (BU) → [`Site` (Sede, opzionale)] → `Area`/`Floor` → `Device` (Apparato) → `Port` (Porta).

### 2.1 Entità e Regole di Ambito (Scope)
- **Project (Progetto):** Identificativo globale UUIDv4. Contiene una o più Business Unit.
- **BusinessUnit (BU):** Ambito organizzativo autonomo. I nomi tecnici e gli indirizzi IP sono univoci all'interno della stessa BU; duplicati in BU differenti sono ammessi e considerati separati per ambito.
- **Site (Sede, Opzionale):** Appartiene a una BU. Può contenere più Aree. Un apparato può non essere associato ad alcuna Sede/Area (apparato non localizzato).
- **Area / Floor (Area/Piano):** Appartiene a una Sede o direttamente a una Business Unit.
- **Device (Apparato):**
  - `id`: UUID.
  - `technicalName`: Nome tecnico (es. `sw-access-01`).
  - `physicalLabel`: Etichetta fisica o targhetta leggibile.
  - `alias`: Denominazione colloquiale/secondaria.
  - `ipAddress` / `macAddress`: Parametri di rete.
  - `siteId` / `areaId`: Riferimenti di posizione opzionali.
- **Port (Porta):**
  - `id`: UUID.
  - `deviceId`: UUID dell'apparato padrone.
  - `connectedPortId`: UUID della porta remota collegata (opzionale/null se scollegata).
  - `endpointStatus`: `CONNECTED`, `DETACHED_TO_VERIFY`, `DISCONNECTED`, `UNKNOWN`.
- **Observation (Osservazione):**
  - Contiene fonte (`source`), data/ora (`timestampEpochMs`), stato (`VERIFIED`, `TO_VERIFY`, `CONFLICT`, `NOT_DETECTED`) e note libere.

## 3. Validazione e Classificazione dei Problemi

La validazione separa rigorosamente i problemi strutturali dagli avvisi documentali.

### 3.1 Errori Strutturali (`STRUCTURAL_ERROR`)
Impediscono l'importazione o il salvataggio poiché corrompono l'integrità dei dati:
- Formato UUID invalido o ID duplicato.
- Pacchetto ZIP non valido o file `manifest.json`/`project.json` mancante.
- Incongruenza del checksum SHA-256 per `project.json` o allegati.
- Porta il cui `deviceId` non corrisponde all'apparato contenitore.
- Collegamento porta (`connectedPortId`) verso un ID porta inesistente.

### 3.2 Avvisi Documentali (`DOCUMENTARY_WARNING`)
Mantenuti ed evidenziati all'utente senza bloccare il salvataggio o l'importazione:
- Indirizzo IP o nome tecnico duplicato all'interno della stessa Business Unit.
- Apparato senza posizione assegnata (`siteId` e `areaId` nulli).
- Osservazione in stato `TO_VERIFY` o `CONFLICT`.
- Estremità porta in stato `DETACHED_TO_VERIFY` (es. a seguito dell'eliminazione dell'apparato remoto).

## 4. Evoluzione del Contratto e Compatibilità Futura
- Le librerie di scambio e storage ignorano le chiavi sconosciute (`ignoreUnknownKeys = true`).
- I campi obbligatori di nuova introduzione negli step futuri forniranno un valore predefinito retrocompatibile.
