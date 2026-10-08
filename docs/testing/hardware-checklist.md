# Checklist hardware RES-13

Usare un progetto di prova senza credenziali reali e la sola fotocamera integrata Android; lettori USB e scanner esterni sono esclusi dal collaudo per decisione utente (8 ottobre 2026). Ogni esito deve indicare dispositivo, versione app, data ed evidenza; `NON ESEGUITO` non equivale a superato.

| ID | Scenario | Atteso |
| --- | --- | --- |
| FOTO-01 | Scatto e riapertura offline | Allegato e destinazione corretti. |
| FOTO-02 | Annullamento o permesso negato | Nessun allegato vuoto; nuovo tentativo possibile. |
| FOTO-03 | Foto, rotazione e scambio cifrato | Byte e destinazione conservati. |
| FOTO-04 | Foto porta e foto cavo dalla scheda rapida della porta | Allegato con destinazione PORT o CABLE, visibile dopo export/import. |
| QR-01 | QR OFAM con fotocamera | Si apre l'entita corretta. |
| QR-02 | Barcode tramite fotocamera integrata, sconosciuto e multiplo | Risposta esplicita, nessuna modifica automatica. |
| QR-03 | Luce bassa, permesso negato, uscita | Recupero e navigazione senza blocco. |
| TOUCH-01 | Pinch e panoramica | Oggetti e sfondo restano coerenti. |
| TOUCH-02 | Selezione e trascinamento | Salvataggio al rilascio e riapertura corretta. |

Registrare gli esiti in [roadmap.md](../../roadmap.md) e chiudere RES-13 nel [tracker](../../PROJECT_STATUS.json) solo quando tutti gli scenari applicabili sono documentati.

## Verifica visiva UI/UX RES-19

Stato al 4 ottobre 2026: **PARZIALE**.

- Eseguito su emulatore Pixel 9, Android API 37, APK debug dopo il restyling UX-R1…R8, app in italiano.
- Configurazione A: 411 dp (densità 420), tema chiaro, testo 1,0.
- Configurazione B: 360 dp (densità 480), tema scuro, testo 1,3.
- La matrice visiva resta parziale. Il 5 ottobre la suite strumentale di 13 test è passata su Pixel 9 API 37 e moto g86 API 36 (RES-17 chiuso), incluso tocco/trascinamento della mappa. Le prove automatiche non sostituiscono gli scenari hardware e visivi elencati sotto.

Eseguire ogni scenario a 360 e 412 dp, in tema chiaro e scuro, con testo standard e ingrandito. Registrare dispositivo/emulatore, API, dimensioni, scala testo e screenshot.

| ID | Scenario | Atteso |
| --- | --- | --- |
| UX-01 | Nuovo dispositivo dall'inventario | Nome e sede bastano; dettagli avanzati chiusi e salvataggio disponibile. |
| UX-02 | Nome o identificativo con tastiera aperta | Campo raggiungibile, titolo e azioni riconoscibili; nessun controllo tagliato. |
| UX-03 | Sezione chiusa con IP non valido | Sezione riaperta, errore presso il campo e nel footer. |
| UX-04 | Modello e gruppo di porte collegato | Ricerca utilizzabile; rimozione collegata esplicita; porte conservate con gli stessi ID. |
| UX-05 | Modifica, chiusura sezione e uscita | Valori conservati; conferma di scarto; annullamento coerente. |
| UX-06 | Menu, filtri e checkbox con testo ingrandito | Destinazioni nello stesso ordine, azioni a capo, bersagli almeno 48 dp e un solo annuncio per checkbox. |

Chiudere RES-19 nel [tracker](../../PROJECT_STATUS.json) solo con evidenze per tutta la matrice applicabile.

### Esiti su emulatore (4 ottobre 2026)

| ID | Conf. | Esito | Evidenza |
| --- | --- | --- | --- |
| UX-01 | A | Superato | Apparato da Dispositivi: Server → nome `SRV-01` e menu preset precompilati, Aggiungi attivo; con una sola business unit non viene chiesta. Salvato con «Aggiunto SRV-01». |
| UX-02 | A | Superato | Nome con tastiera aperta: campo, titolo «Modifica dispositivo · SRV-01», Annulla modifiche e Salva modifiche visibili; barra in basso nascosta durante la modifica. |
| UX-03 | A | Superato con nota | IP `999.1`: errore presso il campo, «!» e valore nel riepilogo della sezione chiusa, messaggio nel footer, Salva disattivato. La sezione chiusa a mano dall'utente non si riapre finché l'errore non cambia. |
| UX-04 | — | NON ESEGUITO | Servono più di sette modelli e un gruppo di porte collegato; coperto solo dai test condivisi Desktop. |
| UX-05 | A | Superato | Annulla modifiche chiede conferma; Continua a modificare conserva `999.1`; Scarta non salva nulla. |
| UX-06 | B | Superato con correzioni | Altro, Dispositivi con selezione multipla (checkbox per riga), mappa, inserimento rapido ed elevazione rack leggibili senza tagli né scorrimento orizzontale. Corretti durante la prova: etichette della barra in basso troncate (ora brevi, ad esempio «Apparati» e «Cavi»), pulsanti della finestra di inserimento impilati (Indietro e Aggiungi ora sulla stessa riga), righe del rack ad altezza fissa (ora crescono con il testo). |

Restano da eseguire:

- TalkBack: annunci non verificabili sull'emulatore senza audio; controllato solo l'albero semantico.
- Le combinazioni 360 dp chiaro e 411 dp scuro.
- Zoom a due dita, moncone e Vai a, Collegamenti logici (in Altri dettagli), scheda rapida della porta (Collega a…, Inserisci passaggio con nuova scatola di giunzione, Scollega, Foto porta e cavo).
- La prova sul moto g86.

### Collaudi aggiunti dall’audit del 6 ottobre — non eseguiti su hardware

Le prove JVM e la compilazione non chiudono queste voci. Usare progetti e storage isolati, conservando app, demo, media e backup reali.

| Riferimento | Prova | Risultato atteso |
| --- | --- | --- |
| AUD-24 / RES-13 | Riavvio Android con recupero pendente e chiave Keystore, su SQLCipher isolato | Esito coerente con il commit Room; backup conservati su errore, riprova disponibile. |
| AUD-24 / RES-19 | Avvio Android con recupero riuscito o bloccato | Comandi ordinati, errore leggibile e annunciato con TalkBack. |
| AUD-24 / RES-23 | EXE con recupero protetto, password errata e riprova | Dialogo e focus utilizzabili, progetto bloccato fino al recupero riuscito. |
| AUD-25 / RES-19/23 | Quattro checkbox della fusione, testo ingrandito e tastiera | Scelte raggiungibili, descrizione leggibile e nessun controllo tagliato. |
| AUD-25 / RES-19/23 | Fusione rifiutata per ciclo; correzione e riprova | Errore leggibile, progetto e cestino conservati; operazione successiva utilizzabile. |
| AUD-28 / RES-19 | Import/ripristino con ID già usato da altro progetto | Errore localizzato e nessuna sostituzione; comando ripetibile dopo correzione. |

AUD-29 / RES-19/23: collaudare il nuovo messaggio di ciclo durante il ripristino e il ritorno al comando dopo correzione. Prove JVM verdi, resa nativa non verificata.

AUD-30 / RES-19: verificare su Android nativo il messaggio di ciclo del grafo nel rifiuto di un import. Plain/protetto e ordine delle sorgenti verificati in JVM; resa nativa non verificata.

Perimetro dei collaudi hardware (decisione utente, 8 ottobre 2026): usare la fotocamera integrata Android; lettori USB e scanner esterni sono esclusi definitivamente dalle prove richieste. Le funzionalità applicative restano disponibili; questa è una decisione sul collaudo. RES-13 conserva fotocamera/gesti e recupero SQLCipher/Keystore isolato, con distinzione fra riavvio del processo e del dispositivo.

RES-13 (8 ottobre): recupero SQLCipher/Keystore verificato su moto g86 API 36 in due processi distinti, con database/media/chiavi avvolte isolati. Cinque scenari di commit/rollback e blocco/riprova conservano l'esito Room e i backup; test roundtrip ordinario verde. Riavvio fisico, arresto forzato e perdita di alimentazione non eseguiti. Restano fotocamera integrata (anche porta/cavo e serie), scansione tramite fotocamera e gesti; lettori USB/scanner esterni esclusi dalle prove per decisione utente. Dettagli e comandi nella roadmap; nessuna modifica al runtime.

AUD-41 (8 ottobre): la continuazione della serie foto Android attende la fine del comando tramite Job.join, dopo commit, cleanup e rilascio dello stato occupato. La foto pendente conserva la sessione di origine; risultati di una precedente apertura dello stesso progetto, errori tardivi e callback cancellati vengono ignorati. Il callback Compose controlla lo scope della schermata prima di riaprire il launcher. Nove prove JVM mirate e cinque prove native con camera/permesso sintetici, FileProvider e repository reali verificano DEVICE/PORT/CABLE, annullamento, guasto/riprova, undo e scambio cifrato. Scatti reali rinviati per decisione utente: RES-13 resta parziale.

### Matrice nativa del configuratore — 8 ottobre 2026

Moto g86 API 36, host Compose isolato con LocalDensity a 360/412 dp, entrambi i temi e scale testo 1,0/1,3. Le impostazioni del sistema non cambiano. Tre prove native passano in 51,805s, con 56 screenshot del dialogo e alberi semantici; campione verificato visivamente. Questa matrice riguarda i componenti, non tutta l'app.

| Scenario | Esito e limite |
| --- | --- |
| Footer scheda porta collegata, con/senza Dettagli | 16 combinazioni verdi, nessuna sovrapposizione dei controlli. AUD-42 corretto. |
| Griglia 48 porte e disposizione tramite due tocchi | Ultima porta raggiungibile; ordine modificato solo con Salva, ID conservati. Trascinamento non eseguito. |
| Supporto PoE e scarto modifiche | P3 configurata dopo Salva; conferma di scarto conserva lo stato precedente. |
| Percorso tra due switch | Stazioni ed etichette raggiungibili mediante scorrimento. Passanti, monconi e ponti radio non eseguiti. |
| Foto porta/cavo | Callback sul target corretto, nessuna apertura della fotocamera. |
| Collegamento e scollegamento | Conferma/annullamento e collegamento alla porta P48 corretti nel progetto in memoria. Nessuna persistenza verificata. |

RES-19 resta aperto per il resto della matrice UX-01–06, TalkBack, tastiera/rotazione, topologia/Altri moduli, mappa densa, passanti e nuovi flussi asincroni. Dettagli in roadmap e tracker; questi esiti non chiudono RES-13.

### UX-04 — Componente Android, 8 ottobre 2026

Superato nelle otto combinazioni LocalDensity: ricerca fra 12 modelli, filtro 12, bozza invariata fino alla scelta; riduzione 2 → 1 di un gruppo RJ45 con P2 collegata, errore bloccante prima della conferma, unico nodo Checkbox e ruolo corretto, ID P1/cavo conservati, capo rimosso aperto e apparato remoto invariato. Dopo l'approvazione il controllo può scomparire: verificata la bozza, non un nodo obsoleto.

Keyboard chiusa prima degli screenshot, host con WindowInsets.safeDrawing. Non sono tastiera aperta, voce TalkBack, editor completo o persistenza. UX-04 componente completato; RES-19 resta parziale. Classe finale: OK (5 tests), 87,536s, log build/reports/res19-native/complete.txt. 80 coppie finali PNG/albero completo generate sul telefono; copia, verifica SHA-256 e pulizia completate come documentato sotto. Le copie dei tentativi precedenti sono conservate e distinte dalle evidenze finali.

### Disponibilità Android — 8 ottobre 2026, sessione successiva

Indisponibilità ADB iniziale risolta: dispositivo riconnesso, evidenze copiate e verificate, pulizia completata nel seguito. Database/WAL/chiavi e backup conservati. Le prove Windows non estendono gli esiti RES-13/19.

### RES-19/24 — Evidenze Android recuperate e pulizia telefono, 8 ottobre 2026

Moto g86 API 36 nuovamente autorizzato via ADB. Copiati 176 file (11.234.842 byte) in build/reports/res19-native/complete-evidence-20261008, verificando ogni SHA-256 contro il telefono: 80 coppie finali PNG/albero completo e otto coppie footer precedenti, distinte nel manifest complete-evidence-integrity-20261008.json. Ogni nome finale e intestazione API/modello verificati; due campioni finali controllati visivamente. Non ripetuta la suite già verde; questi screenshot documentano host Compose isolati, non AppRoot, tastiera/TalkBack o persistenza completa.

Database, WAL, db_key.bin e recovery_key.bin coincidono con data-before.txt; entrambi i backup storici SHA-256 invariati. Riverificati percorso fisico e 176 hash immediatamente prima della rimozione: eliminati solo i file della cartella configurator-matrix-evidence e la directory vuota; disinstallato esclusivamente com.onlyfield.assetmanager.test (Success). App principale conservata. Hash dati/chiavi nuovamente invariati prima della riapertura; MainActivity riaperta con Status: ok. Report data-before-cleanup-20261008.txt e data-after-cleanup-20261008.txt.

Recupero evidenze e pulizia del telefono completati e rimossi dalle descrizioni aperte. RES-19 conserva la matrice UX non eseguita; RES-24 conserva solo le risorse Windows storiche respinte e gli eventuali scratch nuovi. Fonte ufficiale [ADB](https://developer.android.com/tools/adb), consultata l’8 ottobre. Nessun commit/push.

### RES-13 — Recovery dopo riavvio fisico, completato l’8 ottobre 2026

Sul moto g86 API 36, l’utente ha riavviato dal menu e sbloccato il telefono. Boot ID diverso verificato prima di recoveryPhase=recover, stesso recoveryRun=b91d2730-9698-4936-a271-3460d7f0c5cc: OK (1 test), 14,046s. Cinque scenari SQLCipher/Keystore isolati verificati dopo il riavvio: rollback/commit di update e delete, più rollback bloccato da modifica esterna e riprova; chiavi wrapped conservate, journal recuperati secondo l’esito Room. Preparazione precedente: OK (1 test), 20,781s.

Root recovery assente dopo il finally del test; database/WAL/chiavi principali e due backup storici SHA-256 invariati. Rimosso solo com.onlyfield.assetmanager.test (Success); hash principali ancora invariati prima della riapertura di MainActivity (Status: ok). Report boot-before/after, prepare/recover, data-after-recover/cleanup e run.json in build/reports/res13-reboot-20261008.

Riavvio normale con journal pendenti completato e rimosso dalle attività aperte. Restano arresto forzato/perdita improvvisa di alimentazione durante scrittura, fotocamera e gesti; questa prova non verifica UX AppRoot o TalkBack. Fonte primaria [Android Keystore](https://developer.android.com/privacy-and-security/keystore), consultata l’8 ottobre; esiti specifici dal test nativo.
