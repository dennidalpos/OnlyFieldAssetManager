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
   - Entrambe le piattaforme usano la libreria comune `:shared:exchange` per esportare ed importare pacchetti ZIP `.ofam` v1.11 (1.7–1.10 in lettura, versioni successive rifiutate).
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

Le nuove tipologie, posizioni, pagine di planimetria, percorsi, foto dei cavi e relazioni di contenimento sono inclusi anche nei pacchetti cifrati. Per scambiarli occorrono entrambe le app aggiornate. `ContainmentExchangeTest`, `ObjectMapStorageTest` e `FloorMediaTest` verificano rispettivamente contratto/fusione, persistenza Android e archiviazione Windows.

## Fusione all'import (fase v1.1, F04)

Quando si importa un pacchetto dello stesso progetto, oltre a «Sostituisci» è disponibile «Unisci…» (Android: revisione dell'import; Windows: finestra di confronto). Le modifiche fatte da una sola parte dopo l'ultimo scambio vengono applicate da sole; per ogni elemento modificato in entrambe le copie l'app mostra il conflitto (tipo, nome, campi diversi) e chiede «Tieni mio» o «Tieni importato», uno alla volta. La base è l'ultima istantanea sincronizzata (`data/sync/<id>.ofam` su Windows, tabella `sync_snapshots` su Android), aggiornata a ogni export e import. Su Windows l'unione è una modifica come le altre e si annulla con Ctrl+Z.

## Bozze Windows e azioni globali

L’autosave registra le modifiche confermate, non la bozza del form. Le azioni che cambiano contesto o chiudono l’app chiedono «Scarta» oppure «Continua a modificare». La seconda scelta mantiene bozza e progetto aperti; lock e shutdown vengono eseguiti soltanto dopo la conferma.

Il cestino Windows persiste nella copia locale .ofam, nella voce interna `attachments/local/trash.json`, anche nei progetti protetti. Usa la stessa cifratura del progetto, con IV distinto. Non viene estratto fra gli allegati, esportato o incluso nella base di fusione. I vecchi JSON vengono migrati e rimossi dopo il salvataggio. Una copia locale con cestino danneggiato non viene sovrascritta.

Il lock viene acquisito anche dai flussi UI (apertura locale, creazione e sostituzione). Un errore mantiene la copia precedente aperta. Il marcatore .lock puo restare dopo la chiusura: non equivale a un blocco attivo.

La base di fusione segue attivazione, cambio e rimozione della password, mantenendo i contenuti dell'ultimo scambio. Le sostituzioni dei due file sono individualmente atomiche: dopo un arresto fra le sostituzioni, una base con password non corrispondente viene segnalata e il merge richiede revisione completa. I fallimenti intercettati ripristinano la base precedente; nessuna copia di rollback è scritta sul disco.

## Preferenza linguistica

`data/settings.properties` conserva Sistema/Italiano/English/Español. Il cambio passa dalla protezione delle bozze e aggiorna validazioni e messaggi solo dopo il salvataggio della preferenza. Gli export catturano la lingua all'avvio. Vedi [09-localization.md](09-localization.md).

Verifica finale Windows: i lock preesistenti sopravvivono ai tentativi falliti; file non sostituibili secondo le regole di condivisione Windows impediscono cambio password e svuotamento senza alterare UI/progetto/cestino. La base è ripristinata sul fallimento della scrittura principale; se manca all'import, l'avviso di riesame è esplicito.

## Configuratore e Room 14

Il configuratore usa lo stesso schema e le stesse operazioni nei due client. `.ofam` 1.11 conserva hardware, modelli e ID delle porte anche cifrati e durante la fusione. Room 13 → 14 è additiva e preceduta da checkpoint/backup cifrato; schema generato da KSP. Verificati round-trip Room e scambio normale/cifrato. [Dettagli](10-object-configurator.md).
