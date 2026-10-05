# Storage desktop e interoperabilita

Desktop conserva dati e impostazioni in `data/` accanto all'eseguibile portable. `DesktopStorageManager` usa una working copy, sostituzione atomica e `.lock` per evitare aperture concorrenti.

Android memorizza il progetto in Room cifrato; `EncryptedDatabase` protegge la chiave con Android Keystore. Lo schema è alla versione 1 senza migrazioni: una versione diversa viene ricreata vuota, mentre un'identità diversa a versione invariata blocca l'apertura (AUD-03 ancora aperto). Il backup locale non sostituisce l'export `.ofam` per trasferire un progetto.

Entrambe le app usano lo stesso serializer `.ofam`, inclusi allegati, cifratura e base di fusione. Le fixture e i test di interoperabilita verificano i round-trip tra le piattaforme.

Su Android un import nuovo o una sostituzione inizializza il verificatore locale con la password usata per decifrare il pacchetto; un pacchetto non protetto rimuove la protezione della copia sostituita. Progetto e verificatore sono salvati nella stessa transazione. La fusione conserva invece protezione e password della copia locale, anche quando il pacchetto ricevuto ha una protezione diversa. La password ricevuta resta solo in memoria durante la conferma e viene rilasciata su conferma, annullamento o avvio della fusione.

Gli allegati Android sono risolti esclusivamente in `files/attachments/<projectId>/<attachmentId>/<nome-sicuro>`. Il percorso dichiarato nel JSON non viene usato per leggere file locali: gli alias legacy sono accettati soltanto per trovare i byte già contenuti nel pacchetto e copiarli nel percorso del progetto. Un payload assente resta mancante anche se il suo percorso dichiarato corrisponde a un file in un'altra cartella dell'app.

Per il formato vedi [contratto](02-domain-data-contract.md); per il pacchetto Windows vedi [rilascio](06-release-and-delivery.md).

Fonti ufficiali consultate il 5 ottobre 2026: [migrazioni e fallback Room](https://developer.android.com/training/data-storage/room/migrating-db-versions), [confinamento dei percorsi Android](https://developer.android.com/privacy-and-security/risks/path-traversal).

## Media locali protetti su Windows

Per decisione del 5 ottobre 2026, la password protegge anche foto, planimetrie e altri allegati locali. La copia locale `.ofam` contiene i media cifrati con lo stesso AES-GCM del pacchetto: non viene introdotto un secondo formato crittografico. Dopo lo sblocco i byte rimangono in memoria; anteprime e PDF sono letti da byte array e ImageIO usa cache in memoria. Chiusura, cambio progetto e shutdown rilasciano il catalogo in memoria. Non vengono creati media temporanei in chiaro in `data/tmp` o `data/media`.

Attivare la password ingloba i media locali nel pacchetto cifrato e rimuove i file in chiaro soltanto dopo il salvataggio. Le copie protette create prima di questa correzione vanno aperte una volta con la password corretta: i media ancora in chiaro sono recuperati, salvati nel pacchetto cifrato e rimossi. Una password errata non modifica la copia né elimina file. Cambiare o rimuovere la password conserva i payload. I file sorgente scelti dall'utente e le copie esportate esplicitamente restano nelle destinazioni scelte.

Per aprire un allegato protetto in un programma esterno si usa **Esporta e apri**: l'utente sceglie dove salvare la copia in chiaro. La normale visualizzazione interna non produce quella copia. La prova manuale con i programmi esterni resta in RES-20.

Fonti: [PDFBox 3: caricamento da byte array e cache in memoria](https://pdfbox.apache.org/3.0/migration.html), [OWASP: protezione dei dati memorizzati](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html).

## Sostituzione Windows e password diverse

La password incoming protegge la nuova copia; quella locale serve a leggere lo stato precedente. Per una copia chiusa con password diversa l’app chiede la password locale e recupera cestino e relativi media prima del salvataggio atomico. Nessuna perdita implicita del cestino. Password errata permette un nuovo tentativo; annullamento, cestino corrotto e fallimento della scrittura conservano la copia precedente. Con il progetto già aperto si riusa lo stato sbloccato, senza ulteriore richiesta. Il progetto sostituito si riapre con la password incoming, oppure senza password se il pacchetto non è protetto.

## Elenco e operazioni in background

L’elenco legge solo `manifest.json` con `ZipFile`: non importa JSON del progetto né payload media. Nome e protezione sono visibili anche a copia chiusa; un archivio illeggibile resta elencato con il suo errore. La verifica completa avviene all’apertura.

Salvataggio, cifratura, import/export, media, validazione, fusione e documenti usano il worker `DesktopIo` quando chiamati dall’UI AWT. L’event loop resta attivo e le modifiche sono bloccate fino all’esito; non viene introdotta una coda di modifiche concorrenti. Cestino, undo e stato vengono confermati dopo il salvataggio. Prove con 500 apparati protetti e scrittura Windows bloccata superate; resa del pannello e dialoghi di stampa da collaudare (RES-23).
