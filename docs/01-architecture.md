# Architettura

Import Android (AUD-18): Job e identità per richiesta; proprietà del ProjectPackage trasferita dal worker alla Review e poi al comando di conferma/fusione. La cancellazione al ritorno del dispatcher chiude il risultato prima della pubblicazione; completamento del comando chiude anche i pacchetti accodati ma mai eseguiti. Nessun cambio allo schema o al formato.

## Moduli

| Modulo | Responsabilita |
| --- | --- |
| `:shared:core` | Modello, validazione, modifiche, form, mappa, continuita, localizzazione e wizard. |
| `:shared:exchange` | `.ofam`, cifratura, confronto/fusione, etichette QR, XLSX e Markdown. |
| `:mobile:app` | UI Android, Room/SQLCipher, SAF, fotocamera, scanner, PDF e stampa. |
| `:pc:app` | UI Desktop, storage portable, blocco concorrente, PDF e stampa. |
| `shared/configurator` | Sorgenti Compose compilate da entrambe le app. |

`core` ed `exchange` restano JVM puri e non dipendono da Android UI o `Context`. Le UI applicano modifiche attraverso `ProjectEdits`; i form partono dall'entita esistente per conservare i campi non esposti.

Aspetto comune in `shared/configurator`:

- `theme.OnlyFieldTheme`: colori chiaro/scuro e forme squadrate (2–8 dp). I pulsanti M3 hanno angoli pieni fissi, non derivati da `Shapes`; per questo `theme.Button`, `OutlinedButton` e `TextButton` li sostituiscono con angoli da 4 dp e margini interni ridotti. I file UI li importano esplicitamente.
- `ObjectIcons`: disegni a linee dei tipi predefiniti, scelti da `Glyph.typeId`; i tipi personalizzati mostrano il codice testuale.
- `SymbolIcons`: icone di navigazione Material Symbols.

## Confini

I progetti sono locali. La rete serve solo al download esplicito della cartografia; i dati restano utilizzabili offline. Android usa Room cifrato, Windows una working copy con salvataggio atomico e file `.lock`.

Le dipendenze e le versioni effettive sono nel catalogo Gradle [libs.versions.toml](../gradle/libs.versions.toml). Per contratto e persistenza vedi [dominio](02-domain-data-contract.md); per build vedi [rilascio](06-release-and-delivery.md).

## Operazioni e thread

Su Android `PackageExchange` e `DocumentExports` eseguono letture, ZIP/KDF, estrazione e documenti con un dispatcher I/O iniettabile. Apertura e chiusura degli stream ContentResolver avvengono fuori dal thread principale. La stampa genera il PDF sul worker e notifica il framework sul Main; il lavoro termina con `onFinish`. Cancellazione distinta da errore.

Windows conserva gli esiti sincroni dei comandi: `DesktopIo` esegue I/O/calcolo sul worker mentre un `SecondaryLoop` AWT continua a distribuire gli eventi. Stato occupato, blocco delle modifiche e menu disabilitati impediscono salvataggi concorrenti. Il pannello occupato usa un dialogo non chiudibile durante l’operazione, composto dopo form e conferme: rimane visibile sopra i dialoghi già aperti e trattiene tastiera e mouse senza smontare la bozza sottostante (AUD-31). Gli aggiornamenti del progetto/storia tornano al chiamante UI dopo il salvataggio riuscito. Il worker appartiene a `DesktopAppState` e termina allo shutdown. [Android Developers](https://developer.android.com/kotlin/coroutines/coroutines-best-practices), [Java SecondaryLoop](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/SecondaryLoop.html).

AUD-15 (6 ottobre): ProjectViewModel esegue i comandi in ordine con Mutex, senza pubblicare edit prima del salvataggio. Ogni edit legge la versione persistita dopo il comando precedente; un save fallito non ripristina snapshot obsoleti. Sessione di navigazione e revisione proteggono pubblicazione e undo: chiusura/cambio progetto conserva gli edit già richiesti, ma impedisce riaperture tardive. Gli snapshot mappa obsoleti sono rifiutati. ProjectStore.load legge progetto e collezioni nella stessa transazione Room; export e letture non combinano righe di versioni diverse. AppRoot mostra un dialogo occupato che blocca gli input mentre i comandi sono pendenti; il collaudo nativo/accessibilità resta in RES-19. Fonti: [Mutex FIFO](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.sync/-mutex/lock.html), [Room withTransaction](https://developer.android.com/reference/androidx/room/RoomDatabaseKt), [Compose Dialog](https://developer.android.com/develop/ui/compose/components/dialog).

AUD-16 (6 ottobre): sostituzione e fusione Android delegano a ProjectEdits. Lettura, modifica, salvataggio completo e inserimento nel cestino condividono una transazione Room; errori di inserimento ripristinano progetto e cestino. Nessuna modifica allo schema. Semantica delle transazioni: [Room withTransaction](https://developer.android.com/reference/androidx/room/RoomDatabaseKt), consultata il 6 ottobre 2026.

AUD-21 (6 ottobre): il cambio password Android restituisce al dialogo l'errore di persistenza, una sola volta e soltanto nella sessione ancora aperta. La cancellazione viene rilanciata, senza callback di successo. Le preferenze Windows sono lette una sola volta all'avvio; errori I/O o sintassi non valida sono visibili e il file originale resta conservato. Tema e lingua cambiano dopo il salvataggio reversibile sul worker, con eventuali avvisi distinti per la pulizia dopo commit. Fonti consultate: [cancellazione Kotlin](https://kotlinlang.org/docs/cancellation-and-timeouts.html), [Properties Java 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Properties.html).

## Confine delle prove — AUD-22

Core ed Exchange non contengono segnaposto di modulo. La facade Android espone solo servizi usati dall’app: ricerca e tracciamento passano dal core condiviso; modifiche di allegati, piani e apparati sono ProjectEdits persistiti con saveProject. Il loader della fixture Android vive soltanto in DesktopStorageTest e usa il classpath delle risorse di test.

AUD-30: il modello condiviso contiene un solo controllo delle alimentazioni per validazione, fusione e ripristino. Visita iterativa di tutti gli archi con ArrayDeque della libreria standard, senza ricorsione o nuove dipendenze. Fonte API consultata il 7 ottobre 2026: [Kotlin ArrayDeque](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-array-deque/). La correttezza sui grafi descritti è verificata dalle regressioni del repository, non dalla sola fonte API.

AUD-35 (7 ottobre): gli edit della mappa Android richiedono l’esito del save tramite overload compatibile di edit con onResult(String?). Null conferma il successo; il guasto viene mostrato nel form ancora composto. Sessione e ensureActive impediscono callback/errori verso un progetto chiuso; lo scope Compose evita modifiche a una schermata smontata. No-op confermato senza nuovo undo. Le firme precedenti restano compatibili; i form fuori mappa sono estesi in AUD-37 e i comandi specializzati in AUD-39. Fonti [eventi UI](https://developer.android.com/topic/architecture/ui-layer/events) e [ensureActive](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html), consultate il 7 ottobre; esiti nelle prove della roadmap.


AUD-39 (7 ottobre): sostituzione/fusione, import allegato e download espongono overload compatibili con onResult(String?). Null conferma successo; errore e callback vengono pubblicati solo nella sessione attiva, dopo ensureActive e cleanup dei media. EditSave.submit lega la chiusura al successo e alla durata della composizione. Il costruttore applicativo ProjectViewModel(repository) conserva l’acquisizione cartografica reale; il costruttore interno consente una sorgente isolata per le regressioni senza rete pubblica. Credenziali/classificazione riusano edit. Room v2 e .ofam v1 invariati; evidenze nella roadmap.

AUD-40 (8 ottobre): le schede Android di apparato e rack restano aperte fino al commit della cancellazione. Il ritorno usa la scomparsa dell'oggetto dal progetto persistito, senza un nuovo callback; l'effetto controlla progetto e destinazione correnti. Sul guasto rimangono scheda/selezioni ed errore, con riprova. moveToTrash verifica la cancellazione coroutine e pubblica messaggio/undo solo nella sessione di origine ancora attiva. Le prove native usano AppRoot e SQLCipher non viene coinvolto: database Room e media sintetici isolati. Evidenze nella roadmap.

AUD-41 (8 ottobre): la continuazione della serie foto Android attende la fine del comando tramite Job.join, dopo commit, cleanup e rilascio dello stato occupato. La foto pendente conserva la sessione di origine; risultati di una precedente apertura dello stesso progetto, errori tardivi e callback cancellati vengono ignorati. Il callback Compose controlla lo scope della schermata prima di riaprire il launcher. Nove prove JVM mirate e cinque prove native con camera/permesso sintetici, FileProvider e repository reali verificano DEVICE/PORT/CABLE, annullamento, guasto/riprova, undo e scambio cifrato. Scatti reali rinviati per decisione utente: RES-13 resta parziale.
