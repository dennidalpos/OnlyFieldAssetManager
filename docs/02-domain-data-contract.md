# Dominio e contratto `.ofam`

## Modello e validazione

Un progetto contiene sedi (`Site`), piani (`Area`), apparati/porte, rack, cavi, passaggi, rete logica, alimentazioni, media, modelli, campi extra e badge. La gerarchia è **progetto → sede → piano**; il piano dell'apparato è facoltativo e la sua assenza produce un avviso documentale. Stato operativo e stato del rilievo sono distinti.

`ConnectionGraph` segue cavi e passaggi di porte; la topologia logica usa VLAN, subnet, interfacce e WAN/VPN. I riferimenti di rete impediscono la cancellazione o fusione che li lascerebbe pendenti; occorre rimuoverli esplicitamente. Il controllo delle alimentazioni considera tutti gli archi e rifiuta i cicli.

`ModelValidator` produce `STRUCTURAL_ERROR` (blocca import/salvataggio) oppure `DOCUMENTARY_WARNING` (consente il salvataggio). Numeri non finiti, ID estranei o ZIP incoerenti sono rifiutati; allegati mancanti e rilievi incompleti restano avvisi quando il catalogo è valido.

## Pacchetto

`.ofam` **v1** è uno ZIP con manifest, progetto JSON e allegati. Il progetto può essere cifrato con PBKDF2-HMAC-SHA256 e AES-256-GCM; anche gli allegati sono protetti. `PackageSerializer` ignora campi JSON sconosciuti ma rifiuta versioni non supportate, entry duplicate e metadati incoerenti. `ProjectPackage` è `AutoCloseable` e i payload grandi vengono elaborati progressivamente.

| Limite | Valore |
| --- | ---: |
| ZIP | 256 MiB |
| File in chiaro | 32 MiB |
| Totale decompresso | 512 MiB |
| Entry ZIP, directory comprese | 10.000 |
| Iterazioni PBKDF2 massime | 1.000.000 |

L'export Android protetto usa un budget preventivo prudenziale: vicino al limite ZIP può rifiutare dati che entrerebbero nel pacchetto effettivo. È una decisione di prodotto, non un collaudo esatto del confine.

## Scambio, fusione e compatibilità

La base dell'ultimo scambio permette il confronto a tre vie. Le modifiche concorrenti richiedono una scelta esplicita; una base assente/illeggibile viene segnalata. Import e fusione non rimappano ID di un altro progetto. Il cestino e i media recuperabili solo localmente non vengono inviati nel pacchetto.

`Device.management` registra Gestito dal tecnico, Fornitore, Cliente o Sconosciuto; contatto e riferimento contratto sono facoltativi. Il dato è nel JSON `.ofam` v1 e nelle colonne Android Room **v3**. `HardwareSpec.portLayouts` e `portPoeOverrides` restano nel JSON hardware senza nuove tabelle. Le versioni precedenti possono ignorare e perdere questi campi quando risalvano.

Lo schema Android corrente è Room **v3**. `EncryptedDatabase.open` usa il fallback distruttivo se manca una migrazione; esportare i progetti da conservare prima di installare una versione con schema diverso. Vedi [storage](04-desktop-storage-interop.md) e [guida allo scambio](guide/exchange-and-documents.md).

## Fonti

[Room: migrazioni e fallback distruttivo](https://developer.android.com/training/data-storage/room/migrating-db-versions), [OWASP: limiti di upload](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html), [OWASP: password storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html). Valori e compatibilità dell'app sono verificati in `PackageImportLimits`, `PackageSerializer`, `Models`, `ModelValidator` e `AppDatabase`.
