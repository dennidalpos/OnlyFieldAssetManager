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

## Irrobustimento `.ofam` — 16 ottobre 2026

Export protetto con PBKDF2 a 600.000 iterazioni (raccomandazione [OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)); l'import usa il valore del manifest e rifiuta pacchetti protetti senza iterazioni. Checksum obbligatorio per payload e allegati, UUID solo in forma canonica minuscola, export Windows con scrittura forzata su disco e riverifica dei checksum prima della sostituzione, rimosso il verificatore SHA-256 senza salt. Costo misurato con JDK 25 su questo PC: circa 0,45 s per derivazione contro 0,14 s a 100.000; un salvataggio Windows protetto ne esegue più di una. Tempo su Android non misurato.

Full checks (`:shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest`) riusciti: 144 core, 120 exchange, 308 Desktop, 143 mobile, nessun errore. Nessuna prova nativa sull'EXE o sul dispositivo.

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

## Handoff — 22 ottobre 2026

Eseguito audit dell'intero repository: verificati codice reale, contratti dati, vincoli di sicurezza, documentazione (24 file Markdown senza link rotti, 1973 chiavi i18n perfettamente allineate in it/en/es) e suite di test.

Evidenze verificate in questa sessione:
- **Full checks** (`:shared:core:test :shared:exchange:test :pc:app:test :mobile:app:testDebugUnitTest`): tutti i test dei quattro moduli superati con esito verde (144 core, 120 exchange, 308 Desktop, 143 mobile).
- **Suite Demo** (`:shared:exchange:test --tests '*Demo*' --no-parallel --max-workers=1`): superata con esito verde.
- **Script di verifica**: `tools/testing/portable-data.ps1` superato (isolamento dati confermato); `tools/testing/markdown-content.ps1` superato (struttura e contenuti letterali preservati per it/en/es).
- **Packaging e build native**: `:mobile:app:assembleDebug` (APK generato), `:mobile:app:assembleDebugAndroidTest` (APK test generato), `:pc:app:packagePortable` (ZIP runtime portable x64) e `.github/scripts/prepare-release.ps1` eseguiti con esito positivo (`Prepared 2 packages with SHA-256 checksums`).
- **Tracker**: registrato CLP-01 (P3) per la rimozione dei warning compilatore da asserzioni `!!` non necessarie in UI e test; integrato DEP-03 (P3) per il disallineamento plugin Compose e dipendenze orfane; confermati DEP-01 (P1), DEP-02 (P2), AUD-72 (P3) e i collaudi fisici aperti RES-13, RES-19 e RES-23 (P2).

Prossima sessione: avviare da DEP-01 (allineamento dipendenze Compose/AndroidX) seguito da DEP-02; RES-23 su EXE reale Windows isolato. RES-19 e RES-13 richiedono accesso a dispositivo Android fisico.
