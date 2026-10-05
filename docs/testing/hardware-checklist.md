# Checklist hardware RES-13

Usare un progetto di prova senza credenziali reali. Ogni esito deve indicare dispositivo, versione app, data ed evidenza; `NON ESEGUITO` non equivale a superato.

| ID | Scenario | Atteso |
| --- | --- | --- |
| FOTO-01 | Scatto e riapertura offline | Allegato e destinazione corretti. |
| FOTO-02 | Annullamento o permesso negato | Nessun allegato vuoto; nuovo tentativo possibile. |
| FOTO-03 | Foto, rotazione e scambio cifrato | Byte e destinazione conservati. |
| FOTO-04 | Foto porta e foto cavo dalla scheda rapida della porta | Allegato con destinazione PORT o CABLE, visibile dopo export/import. |
| QR-01 | QR OFAM con fotocamera | Si apre l'entita corretta. |
| QR-02 | Barcode, sconosciuto e multiplo | Risposta esplicita, nessuna modifica automatica. |
| QR-03 | Luce bassa, permesso negato, uscita | Recupero e navigazione senza blocco. |
| TOUCH-01 | Pinch e panoramica | Oggetti e sfondo restano coerenti. |
| TOUCH-02 | Selezione e trascinamento | Salvataggio al rilascio e riapertura corretta. |
| USB-01 | Lettore HID e terminatore | Codice completo e una sola ricerca. |
| USB-02 | Ricollegamento e scansioni consecutive | Focus e messaggi prevedibili. |

Registrare gli esiti in [roadmap.md](../../roadmap.md) e chiudere RES-13 nel [tracker](../../PROJECT_STATUS.json) solo quando tutti gli scenari applicabili sono documentati.

## Verifica visiva UI/UX RES-19

Stato al 4 ottobre 2026: **PARZIALE**.

- Eseguito su emulatore Pixel 9, Android API 37, APK debug dopo il restyling UX-R1…R8, app in italiano.
- Configurazione A: 411 dp (densità 420), tema chiaro, testo 1,0.
- Configurazione B: 360 dp (densità 480), tema scuro, testo 1,3.
- Il telefono reale (moto g86) non è stato usato. Il problema strumentale API 37 resta separato in RES-17.

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
