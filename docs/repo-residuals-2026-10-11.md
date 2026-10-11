# Residui del repository — 11 ottobre 2026

Audit sulla baseline `a87f232` (`main`), inizialmente pulita. **6 nuovi rilievi: 1 P1, 3 P2, 2 P3.** Conservati RES-13/19/23/24: **10 attività aperte, 1 P1 / 6 P2 / 3 P3**. Stato canonico in [PROJECT_STATUS.json](../PROJECT_STATUS.json).

## Metodo e limiti

Inventario di 412 file versionati. Ricerca dei segnaposto e dei percorsi legacy, confronto di tracker, decisioni, audit precedenti e completamenti; revisione mirata di navigazione/editor/stato, chiamanti di fusione e base locale, esportazione/localizzazione, scanner, manifest e build/rilascio. Non è una certificazione di ogni riga o una nuova campagna di sicurezza delle dipendenze.

Nessun TODO/FIXME/HACK/NotImplemented applicativo individuato nella ricerca. I punti sospetti sono stati verificati nei consumatori: due difetti riprodotti con DesktopApp e dati sintetici; gli altri sono evidenze da codice/documentazione. Nessuna installazione, modifica di dati utente o pulizia storica. Nessuna remediation runtime in questo audit.

## Priorità

| Priorità | ID | Residuo | Evidenza |
| --- | --- | --- | --- |
| P1 | AUD-65 | Proteggere la bozza quando si nascondono i moduli opzionali | Riprodotto |
| P2 | AUD-66 | Completare la conservazione dello stato di consultazione | Riprodotto |
| P2 | AUD-67 | Conservare le opzioni Documenti Android durante la navigazione | Da codice/documentazione |
| P2 | AUD-68 | Segnalare la base di fusione Android assente o illeggibile | Da codice/documentazione |
| P3 | AUD-69 | Localizzare l’autore predefinito dei documenti Android | Da codice/documentazione |
| P3 | AUD-70 | Allineare workflow e perimetri delle verifiche allo stato corrente | Da codice/documentazione |

## AUD-65 — Proteggere la bozza quando si nascondono i moduli opzionali

**P1.** Windows: Nascondi i moduli non usati cambia showSecondary direttamente. Con Configurazioni vuote e una nuova configurazione modificata, la scheda viene rimossa e la bozza scompare senza conferma. Riprodotto su DesktopApp; Badge usa lo stesso percorso di rimozione, da coprire nella correzione.

**Chiusura:** Applicare il guard delle modifiche prima del cambio visibilità. Verificare Continua a modificare/Scarta su Configurazioni e Badge, finestra larga/stretta; nessuna perdita o salvataggio implicito.

Riferimenti: `pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopApp.kt:174`, `pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/ui/NetworkLogicalSection.kt:43`, `pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/ui/PowerBadgeSection.kt:41`.

## AUD-66 — Completare la conservazione dello stato di consultazione

**P2.** Riprodotto Windows: ricerca VLAN persa passando a Subnet e tornando. Il SaveableStateHolder copre AppSection, non le sottoschede condizionali. Da codice restano query Modelli e scheda Allegati Windows in remember semplice. La promessa di conservazione degli elenchi è quindi parziale.

**Chiusura:** Preservare ricerca, scheda e scroll nel perimetro approvato, con isolamento tra progetti e ripristino coerente dopo cancellazioni; prove di andata/ritorno fra sottoschede e sezioni, senza conservare editor dismessi.

Riferimenti: `pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopApp.kt:283`, `pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/ui/NetworkLogicalSection.kt:45`, `pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/ui/DeviceModelsSection.kt:29`, `pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/ui/FloorplanMediaSection.kt:50`.

## AUD-67 — Conservare le opzioni Documenti Android durante la navigazione

**P2.** Formato, autore, titolo, selezione sezioni e opzioni Documenti usano remember. Passando a un’altra area e tornando vengono reinizializzati: il SaveableStateProvider esterno non conserva remember semplice. La prova nativa precedente verificava il footer con tastiera, non questo percorso.

**Chiusura:** Conservare la bozza delle opzioni per progetto durante la sessione o proteggere esplicitamente lo scarto; verificare andata/ritorno e cambio progetto. Non ripristinare conferme di contenuti riservati/rivedibili contro un insieme di allegati cambiato.

Riferimenti: `mobile/app/src/main/java/com/onlyfield/assetmanager/ui/screens/DocumentsScreen.kt:33`, `mobile/app/src/main/java/com/onlyfield/assetmanager/ui/AppRoot.kt:83`.

## AUD-68 — Segnalare la base di fusione Android assente o illeggibile

**P2.** ProjectStore.loadBase converte qualsiasi errore di decodifica in null; startMerge usa il risultato senza segnalare la perdita della base. Windows mostra merge.baseUnavailable o errore di lettura. Il merger resta conservativo e richiede conflitti per differenze: non è dimostrata perdita automatica di dati.

**Chiusura:** Distinguere assenza e corruzione, rendere visibile il passaggio alla revisione completa e non cancellare la base illeggibile per sola lettura. Provare base valida/assente/corrotta, annullamento e riprova; conservare la politica di fusione confermata.

Riferimenti: `mobile/app/src/main/java/com/onlyfield/assetmanager/data/repository/ProjectStore.kt:330`, `mobile/app/src/main/java/com/onlyfield/assetmanager/ui/ProjectViewModel.kt:635`, `pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/DesktopAppState.kt:645`.

## AUD-69 — Localizzare l’autore predefinito dei documenti Android

**P3.** DocumentsScreen.filter usa il letterale italiano Tecnico quando autore è vuoto; Windows usa la chiave localizzata text.2bf644bed677. PDF e Markdown Android ricevono quindi il fallback italiano anche in inglese/spagnolo.

**Chiusura:** Usare la chiave esistente e verificare autore vuoto it/en/es e testo autore esplicito invariato nei documenti.

Riferimenti: `mobile/app/src/main/java/com/onlyfield/assetmanager/ui/screens/DocumentsScreen.kt:44`, `pc/app/src/main/kotlin/com/onlyfield/assetmanager/pc/ui/dialogs/ProjectDialogs.kt:278`.

## AUD-70 — Allineare workflow e perimetri delle verifiche allo stato corrente

**P3.** docs/07-workflows.md apre descrivendo AUD-46 e AUD-49 come ancora difettosi ma più avanti ne documenta la correzione. RES-19 mescola AppRoot indicata genericamente come aperta con prove già concluse. Le prove dell’11 ottobre non coprono wizard con tastiera, reset tra progetti o scroll; la chiusura UX-08 va letta nel perimetro effettivamente verificato.

**Chiusura:** Rimuovere affermazioni correnti contraddittorie preservando lo storico; distinguere scenari verificati e pendenti. Integrare prove mirate wizard/tastiera, scroll e isolamento progetto nella verifica della remediation, senza dichiarare tablet/rotazione/TalkBack dai test simulati.

Riferimenti: `docs/07-workflows.md:5`, `docs/07-workflows.md:141`, `docs/ui-ux-audit-2026-10-11.md`, `PROJECT_STATUS.json`.

## Riproduzioni

Probe temporaneo `ResidueAuditProbeTest`, eseguito con `.\gradlew.bat :pc:app:test --tests '*ResidueAuditProbeTest' --no-parallel --max-workers=1`: **BUILD SUCCESSFUL in 14s**, 2 test, zero fallimenti/errori/saltati. Le assertion confermano i comportamenti difettosi, quindi l’esito verde **non chiude** AUD-65/66.

- AUD-65: DesktopApp 1600×1000, progetto con un apparato e nessuna configurazione salvata. Mostra moduli → Rete → Configurazioni → Nuova configurazione → titolo modificato. `detailSlot.dirty == true`; clic reale su Nascondi i moduli non usati. Titolo della bozza assente, nessuna conferma, nessuna configurazione salvata.
- AUD-66: DesktopApp 1360×860. Ricerca VLAN compilata, selezione Subnet, ritorno VLAN: campo presente e testo di ricerca perso.

Sorgente e XML conservati in `build/reports/repo-residuals-20261011`, ignorato da Git. Probe rimosso dai sorgenti di test dopo l’archiviazione; cartelle sintetiche ripulite dai `finally`. Nessuna suite generale ripetuta: runtime non modificato. AUD-67/68/69 non riprodotti nativamente in questo audit.

## Legacy e lavoro già aperto

Compatibilità SHA-256 delle password (con needsRehash), percorsi allegati legacy e fallback dei tipi oggetto hanno chiamanti/prove; non sono codice morto da eliminare indiscriminatamente. `stub` della mappa indica un collegamento che continua fuori vista, non una funzione incompleta. Nessuna rimozione di dipendenze proposta senza prova di inutilizzo.

AUD-45–64 restano chiusi nei rispettivi perimetri: AUD-65 è un’uscita dalla bozza tramite visibilità dei moduli, non un errore di salvataggio AUD-46. AUD-68 riguarda base di fusione e segnalazione, non i mapper di entità già corretti. Il merger senza base genera conflitti sulle differenze; non si deduce una fusione distruttiva automatica.

RES-13 resta hardware/recupero improvviso; RES-19 matrice Android fisica e flussi esclusi; RES-23 EXE/dialoghi nativi e scenari esclusi; RES-24 sole risorse sintetiche inventariate, con rifiuti storici preservati. Evidenze e backup dell’11 ottobre non sono materiale da eliminare. La disponibilità del telefono del 9 ottobre è un dato storico, non una verifica della disponibilità attuale.

## Fonti primarie

Consultate l’11 ottobre 2026: [Stato in Compose](https://developer.android.com/develop/ui/compose/state) e [Salvataggio dello stato UI](https://developer.android.com/develop/ui/compose/state-saving). `remember` dura finché il relativo contenuto resta in composizione; il salvataggio va progettato per lo stato necessario. Questi contratti aiutano a spiegare AUD-66/67; la riproduzione del difetto specifico proviene dal repository e dai probe sopra.
