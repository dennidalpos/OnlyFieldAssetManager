# Collaudi Android — RES-13 e RES-19

Usare un progetto di prova senza credenziali reali. Registrare dispositivo, API, versione APK, scala testo, tema, dimensioni e risultato; “non eseguito” non equivale a superato. Lavoro aperto e criteri di chiusura nel [tracker](../../PROJECT_STATUS.json).

## RES-13 — hardware e recupero

| Caso | Prova richiesta |
| --- | --- |
| FOTO-01–04 | Scatto e riapertura offline, annullamento/permesso negato, serie, rotazione, export/import cifrato, foto di porta e cavo con target corretto. |
| QR-01–03 | QR e barcode con fotocamera integrata; codice ignoto/multiplo, luce bassa, permesso negato e uscita. |
| TOUCH-01–02 | Pinch/panoramica e selezione/trascinamento con salvataggio e riapertura. |
| Recupero | Arresto forzato e interruzione improvvisa durante scrittura su SQLCipher/Keystore isolati, conservando backup e dati utente. |

Lettori USB e scanner esterni sono esclusi dal collaudo per decisione dell'utente. Il riavvio fisico normale con journal pendenti è già stato verificato l'8 ottobre 2026; non sostituisce l'interruzione durante la scrittura. Le prove di fotocamera con sorgente sintetica non verificano gli scatti reali, rinviati dall'utente.

## RES-19 — UI sul dispositivo

Riprendere gli scenari UX-01–06 a 360/412 dp, chiaro/scuro, testo 1,0/1,3, tastiera aperta e rotazione reale. Controllare campi/errori raggiungibili, conferme di scarto, ricerca modello/porte, bersagli e annunci TalkBack. Includere tablet fisico quando disponibile.

Verificare inoltre: livelli della Mappa e ritorno Indietro; vista Apparati/scheda porta; configuratore denso; riquadri Home Progetto e form Rete/Alimentazione/Cablaggio/Media; dialoghi a schermo intero; Documenti dopo navigazione; wizard protetto; salvataggi asincroni, import con avvisi, fusione/rifiuti e recupero. Usare i dettagli del [tracker](../../PROJECT_STATUS.json) per non saltare casi.

Esiti parziali precedenti: su moto g86 API 36 sono stati visualizzati schema sedi e vista Apparati della demo; prove Compose a densità simulate e alcune schermate AppRoot sono riuscite. Rotazione fisica, audio/focus TalkBack, tablet e i flussi sopra non risultano completi. Non dedurre il loro esito dalle suite JVM. Fonte operativa: [Android accessibility testing](https://developer.android.com/guide/topics/ui/accessibility/testing), [ADB](https://developer.android.com/tools/adb).
