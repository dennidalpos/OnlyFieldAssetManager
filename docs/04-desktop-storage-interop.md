# Architettura Storage Desktop e Interoperabilità Android <-> PC

Data: 3 ottobre 2026

## Storage Desktop (`DesktopStorageManager`)

L'editor Windows Desktop (`:pc:app`) salva i progetti in una cartella dati (`dataDir`) risolta da `PortablePaths`.

### 1. Cartella Dati Portable (`PortablePaths`)
- Avviato dall'eseguibile del pacchetto portable, i dati stanno in `data\` accanto a `OnlyFieldAssetManager.exe` (proprietà `jpackage.app-path`).
- Se quella cartella non è scrivibile, o se l'app è avviata da Gradle, si usa `%USERPROFILE%\.onlyfield_asset_manager`.
- Leggibilità, scrivibilità e spazio libero vengono verificati; il percorso in uso è mostrato nella barra di stato.
- Ogni modifica viene salvata automaticamente: non esiste un comando "Salva".

### 2. Salvataggio Transazionale Atomico
- La scrittura di un progetto avviene sempre prima su un file temporaneo `.tmp` nella stessa directory.
- A scrittura completata, il file temporaneo sostituisce atomicamente il file di destinazione finale (`StandardCopyOption.ATOMIC_MOVE` o `REPLACE_EXISTING`).
- Questo previene corruzioni dei dati in caso di interruzione improvvisa o riavvio.

### 3. Blocco Concorrente della Copia di Lavoro (`.lock`)
- Per evitare conflitti di modifica tra più istanze/processi, ogni progetto aperto acquisisce un blocco esclusivo tramite `FileChannel.tryLock()` su un file `.lock` dedicato.
- Tentativi di apertura contemporanea dello stesso file da un altro processo restituiscono un errore di accesso bloccato.

## Interoperabilità e Scambio Android <-> Windows (`BidirectionalInteropTest`)

1. **Contratto Pacchetto Omogeneo:**
   - Entrambe le piattaforme usano la libreria comune `:shared:exchange` per esportare ed importare pacchetti ZIP `.ofam` v1.8 (anche v1.7 in lettura).
2. **Supporto Cifratura Completo:**
   - Supporto identico per pacchetti cifrati con password mediante PBKDF2 (100.000 iterazioni) e AES-256-GCM.
3. **Confronto Semantico delle Versioni (`ProjectComparison`):**
   - Al momento dell'importazione, l'app confronta l'ID di progetto, l'ID di esportazione e i checksum per determinare lo stato del pacchetto rispetto alla copia locale:
     - `IDENTICAL`: Progetto identico, nessuna modifica.
     - `NEWER_REVISION`: Pacchetto con revisione o data aggiornata.
     - `OLDER_REVISION`: Pacchetto antecedente alla copia locale.
     - `DIVERGENT`: Modifiche locali e pacchetto importato sono disallineate.
     - `DIFFERENT_PROJECT`: Progetto con ID distinto.
4. **Collaudo Bidirezionale Completo (W05):**
   - La suite `BidirectionalInteropTest` collauda il ciclo di vita completo di uno scambio bidirezionale:
     - Generazione pacchetto `.ofam` v1.7 su Android (con entità A01-A12 e allegati media).
     - Importazione ed estrazione trasparente su Windows Desktop via `DesktopStorageManager`.
     - Modifica ed aggiornamento transazionale atomico su Windows.
     - Re-esportazione pacchetto da Windows ed importazione su Android con riscontro `NEWER_REVISION` e verifica dell'integrità del modello di dominio.
