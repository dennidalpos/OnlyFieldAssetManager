# Evidenze e stato

Questa pagina registra esiti sintetici; il lavoro aperto e i criteri di chiusura sono in [PROJECT_STATUS.json](PROJECT_STATUS.json). Gli esiti elencati sono storici finché non vengono rieseguiti nel checkout corrente.

## Verifica automatica più recente registrata

L'handoff dell'11 ottobre 2026 registra **451 test unit/Compose verdi** (307 Desktop, 144 mobile/condivisi) dopo la correzione AUD-71 e l'aggiunta di `FieldValidatorsTest`/test `HierarchyIndex`. La suite demo mirata registra **19 test verdi** dopo la rigenerazione di `fixtures/demo/onlyfield-demo.ofam`. L'handoff precedente aveva registrato 701 prove nei quattro moduli e compilazione dei test Android; i conteggi non sono sommabili perché appartengono a esecuzioni diverse.

Comandi registrati:

```powershell
.\gradlew.bat :shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest :mobile:app:compileDebugAndroidTestKotlin --no-parallel --max-workers=1
.\gradlew.bat :shared:exchange:test --tests '*Demo*' --no-parallel --max-workers=1
```

Queste prove non verificano rotazione fisica, TalkBack, fotocamera, tablet, stampa né tutti i dialoghi dell'EXE.

## Completamenti per ambito

| Ambito | Esito confermato nel registro storico |
| --- | --- |
| Scambio e persistenza | Import protetto, limiti ZIP/KDF, staging cifrato, rollback, collisioni ID, base e recupero durevole corretti; `.ofam` resta v1. |
| Android | Room v3 con gestione di terzi; comandi ordinati, editor e import mantengono bozze dopo errori; prove SQLCipher/Keystore isolate e riavvio normale eseguite. |
| Windows | Storage portable e recupero cifrato; 11 editor verificati nella matrice base nativa con 22 guasti/riprova e 11 undo. |
| UI e mappa | Quattro aree, viste a livello progetto/sede/piano, vista Logica e Apparati; AUD-71 corretto in Compose. |
| Demo e risorse | Fixture aggiornata e 19 prove demo; pulizia RES-24 completata su richiesta l'11 ottobre, preservando report, backup e demo. |

Il [registro storico consolidato](docs/history/verification-2026-10.md) distingue i principali esiti nativi e i report locali. Il commit precedente alla riorganizzazione, `6ef0e6436185cd408bfcd4345813c85f157f27b6`, conserva il diario completo (`git show 6ef0e6436185cd408bfcd4345813c85f157f27b6:roadmap.md`). Non usarlo come stato corrente.

## Collaudi ancora aperti

| ID | Perimetro |
| --- | --- |
| RES-13 | Fotocamera/scanner integrati, gesti fisici e interruzione improvvisa durante scrittura. |
| RES-19 | Matrice visiva Android, rotazione reale, TalkBack, tablet e flussi non ancora provati. |
| RES-23 | Matrice EXE Windows, varianti editor/focus, protezione, fusione, PDF e stampa fisica. |

Vedi [checklist Android](docs/testing/hardware-checklist.md) e [matrice Windows](docs/testing/windows-native-matrix.md). Nessuna delle tre è chiusa dalle sole prove automatiche.

## Revisione documentale — 13 ottobre 2026

Confrontati documenti, tracker, build Gradle e percorsi applicativi. Corretti Room v2 → v3, stato RES-24, descrizione delle viste di mappa e affermazioni promozionali non supportate; separate guide d'uso, dominio, storage, test e storico. Le vecchie pagine di audit/UX sono recuperabili dal commit sopra. La rimozione del manuale PDF storico è stata respinta dal controllo automatico (`blocked by policy`); il file resta nel repository ma non è più la guida corrente. Sono controlli documentali: non è stata eseguita una nuova prova dell'app.

Controlli statici della revisione: parser JSON riuscito con tre ID residui univoci; 23 file Markdown e 91 collegamenti locali validi; nessun BOM; ricerca dei nomi dei documenti ritirati senza link residui; `git diff --check` riuscito. Questi controlli verificano la documentazione, non il comportamento runtime.

## Handoff — 13 ottobre 2026

La documentazione riorganizzata è pronta per il cambio sessione. Avviare dalla [README](README.md) e dal [tracker](PROJECT_STATUS.json). Come primo lavoro autonomo, completare i casi fattibili di RES-23 sull'EXE Windows in un profilo sintetico isolato, conservando `dist/**/data`. RES-19 e RES-13 richiedono accesso al dispositivo e prove fisiche; `adb` non era nel PATH al controllo del 13 ottobre. Il manuale PDF storico rimane perché la rimozione è stata respinta dal controllo automatico (`blocked by policy`). I tre collaudi restano parziali.
