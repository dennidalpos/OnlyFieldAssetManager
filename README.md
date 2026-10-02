# OnlyFieldAssetManager

## Avvio dello sviluppo

1. Leggere [regole del repository](AGENTS.md) e [piano operativo](plan.md).
2. Aprire la cartella del repository in Android Studio, verificare Gemini ed eseguire soltanto A00 del piano.
3. In A00 generare il progetto Gradle e le cartelle separate mobile/ e pc/, con i moduli comuni in shared/. Continuare A01–A13 uno step alla volta.
4. Registrare comandi, risultati e limiti in [roadmap.md](roadmap.md).
5. Dopo l'APK Android verificato di A13, eseguire W00–W05 in un IDE desktop con altri strumenti AI.

## Contesto da fornire all'agente

Editor offline per documentazione di infrastrutture networking. Android 14+ su smartphone/tablet prima; Windows 11 x64 portable dopo. Gli editor scambiano un pacchetto completo documentato e condividono modello/regole dove possibile. Password del progetto opzionale; uscite documentali senza campi segreti. Il piano specifica domini, integrità, protezione, formati e criteri di verifica.

Usare il prompt di plan.md §3 per lo step selezionato. Istruzioni operative in italiano; codice, identificatori e commenti in inglese. Usare soltanto dati sintetici nelle prove e nelle conversazioni AI.

## Struttura da generare

| Cartella | Responsabilità |
| :--- | :--- |
| mobile/ | App Android e test specifici |
| pc/ | App Windows e test specifici; implementazione da W00 |
| shared/ | Modello, regole e scambio senza API Android |
| docs/ | Contratto, decisioni tecniche, verifiche e rilascio |
| fixtures/ | Campioni sintetici comuni e risultati attesi |

La struttura del progetto e lo step A00 sono stati completati e verificati: generato il wrapper Gradle, la struttura dei moduli (`mobile/app`, `shared/core`, `shared/exchange`, `pc/` riservato), la documentazione tecnica e l'APK debug compilato. Consultare [roadmap.md](roadmap.md) per lo stato di avanzamento corrente.
