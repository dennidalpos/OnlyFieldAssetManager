# Sintesi delle verifiche storiche — ottobre 2026

Questa pagina preserva i confini delle prove concluse senza ripetere il diario per task. Per lo stato corrente usare il [tracker](../../PROJECT_STATUS.json); per log, hash e comandi completi usare il commit pre-riordino `6ef0e6436185cd408bfcd4345813c85f157f27b6` (`git show <sha>:roadmap.md`). I report locali sotto `build/reports/` sono ignorati da Git e potrebbero non esistere in altri checkout.

## Scambio, storage e documenti

- Gli audit registrati fino ad AUD-71 sono stati corretti o chiusi secondo le decisioni registrate; AUD-23 ha confermato il budget Android protetto prudenziale. RES-24 è stato pulito su richiesta l'11 ottobre.
- Import grandi: una fixture valida da 511 MiB espansi causava OOM sul moto g86 prima della correzione. Dopo la lettura progressiva e lo staging cifrato, sette fixture valide sono passate su Windows e moto g86 con verifica SHA-256 degli allegati. Misure e limiti nel [benchmark](../testing/import-benchmark-2026-10-05.md).
- Crittografia, rollback, fusione, documenti e validazione hanno prove JVM/Compose; l'ultimo handoff registra 451 prove verdi e 19 prove demo verdi. Vedi [verifica](../05-testing-and-benchmarks.md).

## Android nativo

- Moto g86 API 36: SQLCipher/Keystore isolati, recupero in due processi e dopo riavvio fisico normale verificati; database, chiavi e backup dell'app principale risultavano invariati nei controlli registrati.
- Una matrice Compose con larghezze simulate, temi e testo ingrandito ha verificato parte della UI. Sul dispositivo sono stati visualizzati schema sedi, vista Apparati e alcune schermate AppRoot della demo.
- Restano scatti/scansioni reali, gesti, arresto forzato durante scrittura, rotazione reale, TalkBack, tablet e flussi elencati in [RES-13/19](../testing/hardware-checklist.md).

## EXE Windows nativo

- In un profilo sintetico sono stati verificati 11 editor, 22 salvataggi con guasto/riprova e 11 undo; `build/reports/res23-network-native-20261009/matrix-result.json` contiene il confronto dei progetti.
- Un EXE dell'11 ottobre ha importato la demo e mostrato schema sedi, Logica, Apparati, scheda porta e Home Progetto. Il difetto VLAN AUD-71 osservato lì è stato poi corretto nel codice e nel test Compose, ma la resa nell'EXE aggiornato resta da verificare.
- Varianti visive, focus, protezione, fusione complessa, spooler e stampa fisica restano nella [matrice RES-23](../testing/windows-native-matrix.md).

Le prove simulate e i log di compilazione non estendono il perimetro dei collaudi fisici.
