# Persistenza, protezione e recupero

## Android

`EncryptedDatabase.open` apre Room **v3** tramite SQLCipher; la chiave del database è avvolta da Android Keystore. `ProjectStore` usa transazioni per letture coerenti e comandi; controlla il proprietario degli ID prima di cancellare o sostituire record. Un cambio di schema senza migrazione configurata ricrea le tabelle: esportare prima i progetti da conservare.

## Windows

`DesktopStorageManager` conserva progetti e preferenze sotto `data/` accanto all'EXE. La working copy, la base dell'ultimo scambio, il cestino e i media vengono salvati con operazioni reversibili; un file `.lock` evita l'uso concorrente della stessa copia. I media di un progetto protetto restano cifrati anche nello staging locale. “Esporta e apri” crea invece una copia in chiaro nella destinazione scelta dall'utente.

`DesktopIo` esegue il lavoro sul worker; il pannello occupato blocca modifiche concorrenti. Un save fallito mantiene la bozza per la riprova e non diventa un undo riuscito.

## Recupero

`FileRecovery` conserva journal, staging e backup cifrati. Prima del commit ripristina i file precedenti; dopo il commit completa i file nuovi e ritenta la pulizia. Un recupero non verificabile blocca il progetto e conserva le evidenze per una nuova prova; su Windows un progetto protetto richiede la password locale. Android confronta l'esito dei file con la transazione Room e usa una chiave di recupero distinta, avvolta dal Keystore.

Il cestino locale trattiene oggetti e media necessari al ripristino. Il ripristino rifiuta contesti originali mancanti, ID occupati, collocazioni incompatibili o cicli di alimentazione senza ricollocare automaticamente l'oggetto. La fusione può trasferire credenziali, configurazioni, alimentazioni e campi extra secondo le quattro opzioni esplicite; ciò che non viene trasferito resta nel cestino.

Le prove di interruzione del processo e di riavvio normale sono registrate nello [storico](../roadmap.md). Arresto forzato/perdita di alimentazione durante scrittura e focus del recupero nativo restano in [RES-13/19/23](../PROJECT_STATUS.json).

## Fonti

[Android Keystore](https://developer.android.com/privacy-and-security/keystore), [Room: migrazioni](https://developer.android.com/training/data-storage/room/migrating-db-versions), [SQLite: commit atomico](https://www.sqlite.org/atomiccommit.html), [Java FileChannel.force](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/channels/FileChannel.html#force(boolean)).
