# Evidenze e residui

Aggiornato al 4 ottobre 2026 dopo confronto con il codice e il tracker.

## Evidenze disponibili

- I moduli Gradle sono `:shared:core`, `:shared:exchange`, `:mobile:app` e `:pc:app`.
- Il codice implementa `.ofam` 1.11, Room 14, storage desktop atomico, database Android cifrato, import/export, documenti, mappe e configuratore condiviso.
- La suite locale registrata prima di questa revisione copre core, exchange, Android JVM e Desktop; le prove strumentali e manuali hanno limiti espliciti sotto.
- Il workflow GitHub esegue le suite JVM/Compose, genera APK debug e ZIP portable, calcola SHA-256 e pubblica soltanto su tag `v*`.

## Residui aperti

| ID | Stato | Evidenza richiesta |
| --- | --- | --- |
| RES-01 | Parziale | Esecuzione remota del workflow su un tag reale. |
| RES-13 | Aperto | Checklist foto, scansione, multitouch e lettore USB su hardware reale. |
| RES-17 | Aperto | Suite UI Android verde su API 37 senza disabilitare controlli. |
| RES-18 | Aperto | Rimozione mirata degli scratch preesistenti sotto `build/`, dopo verifica dei percorsi. |
| RES-19 | Aperto | Matrice visiva Android 360/412 dp, tastiera, testo ingrandito e temi chiaro/scuro. |

Il dettaglio operativo e il criterio di chiusura sono in [PROJECT_STATUS.json](PROJECT_STATUS.json). La checklist hardware e in [docs/testing/hardware-checklist.md](docs/testing/hardware-checklist.md).

## UI/UX — 4 ottobre 2026

- Configuratori essenziali prima dei dettagli; gruppi di porte riepilogati e modificabili singolarmente, ricerca nei selettori, errori presso i campi e il salvataggio. Titoli operativi e azioni specifiche; sezioni richiuse conservano la bozza.
- Navigazione comune per gruppi; sidebar desktop 208 dp adattiva, scorciatoie numeriche conservate. Liste nominali ordinate solo nella presentazione, righe selezionate evidenti e azioni a capo.
- Editor della mappa allineati alla gerarchia degli altri editor. Nessuna modifica a database, contratto `.ofam` o regole del dominio; conservate le modifiche già presenti nel checkout.
- Baseline mirata: 7 test superati. Verifica finale: `BUILD SUCCESSFUL in 34s`; 214 test superati (Core 64, Exchange 37, Desktop 85, Android JVM 28). APK debug e modulo Desktop compilati; test strumentali Android compilati.
- Rendering Compose Desktop ispezionati a 1360×860 e 1024×768; catture in `pc/app/build/reports/ux/`. Verificati inserimento essenziale, modello, porte, gruppi collegati, scarto della bozza e localizzazione. Comando e perimetro in [docs/05-testing-and-benchmarks.md](docs/05-testing-and-benchmarks.md).
- Android nativo con tastiera, scala testo e temi: non eseguito, RES-19. Il rendering condiviso a 360/412 dp non chiude questa verifica; RES-17 resta aperto.
- Pulizia degli scratch della revisione sotto `build/ux-audit/` bloccata dalla policy di esecuzione: due rifiuti `blocked by policy`, anche con percorso assoluto verificato e `Remove-Item -LiteralPath`. Nessun file eliminato; report e catture conservati. Rimozione da completare in RES-18.

## Consegna per cambio sessione — 4 ottobre 2026

- Ramo di consegna: `main`, verificato allineato a `origin/main` prima del salvataggio. Il commit raccoglie revisione UI/UX, manutenzione e consolidamento documentale già presenti nel checkout.
- Verifica disponibile: build Android/Desktop riuscite, 214 test superati e `git diff --check` senza errori. Nessuna modifica al codice dopo questa verifica.
- Tracker senza attività in corso. Ripartenza: checklist Android RES-19, suite UI RES-17, pulizia scratch RES-18; restano collaudo hardware RES-13 e CI su tag RES-01.
