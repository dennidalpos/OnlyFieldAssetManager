# Checklist hardware RES-13

Usare un progetto di prova senza credenziali reali. Ogni esito deve indicare dispositivo, versione app, data ed evidenza; `NON ESEGUITO` non equivale a superato.

| ID | Scenario | Atteso |
| --- | --- | --- |
| FOTO-01 | Scatto e riapertura offline | Allegato e destinazione corretti. |
| FOTO-02 | Annullamento o permesso negato | Nessun allegato vuoto; nuovo tentativo possibile. |
| FOTO-03 | Foto, rotazione e scambio cifrato | Byte e destinazione conservati. |
| QR-01 | QR OFAM con fotocamera | Si apre l'entita corretta. |
| QR-02 | Barcode, sconosciuto e multiplo | Risposta esplicita, nessuna modifica automatica. |
| QR-03 | Luce bassa, permesso negato, uscita | Recupero e navigazione senza blocco. |
| TOUCH-01 | Pinch e panoramica | Oggetti e sfondo restano coerenti. |
| TOUCH-02 | Selezione e trascinamento | Salvataggio al rilascio e riapertura corretta. |
| USB-01 | Lettore HID e terminatore | Codice completo e una sola ricerca. |
| USB-02 | Ricollegamento e scansioni consecutive | Focus e messaggi prevedibili. |

Registrare gli esiti in [roadmap.md](../../roadmap.md) e chiudere RES-13 nel [tracker](../../PROJECT_STATUS.json) solo quando tutti gli scenari applicabili sono documentati.

## Verifica visiva UI/UX RES-19

Stato al 4 ottobre 2026: **NON ESEGUITO su Android nativo**. Nessun dispositivo collegato nella sessione; il problema strumentale API 37 resta separato in RES-17. I test Compose Desktop del configuratore condiviso a 360 e 412 dp verificano i flussi, ma non sostituiscono tastiera, inset, font di sistema e TalkBack Android.

Eseguire ogni scenario a 360 e 412 dp, in tema chiaro e scuro, con testo standard e ingrandito. Registrare dispositivo/emulatore, API, dimensioni, scala testo e screenshot.

| ID | Scenario | Atteso |
| --- | --- | --- |
| UX-01 | Nuovo dispositivo dall'inventario | Nome e business unit bastano; dettagli avanzati chiusi e salvataggio disponibile. |
| UX-02 | Nome o identificativo con tastiera aperta | Campo raggiungibile, titolo e azioni riconoscibili; nessun controllo tagliato. |
| UX-03 | Sezione chiusa con IP non valido | Sezione riaperta, errore presso il campo e nel footer. |
| UX-04 | Modello e gruppo di porte collegato | Ricerca utilizzabile; rimozione collegata esplicita; porte conservate con gli stessi ID. |
| UX-05 | Modifica, chiusura sezione e uscita | Valori conservati; conferma di scarto; annullamento coerente. |
| UX-06 | Menu, filtri e checkbox con testo ingrandito | Destinazioni nello stesso ordine, azioni a capo, bersagli almeno 48 dp e un solo annuncio per checkbox. |

Chiudere RES-19 nel [tracker](../../PROJECT_STATUS.json) solo con evidenze per tutta la matrice applicabile.
