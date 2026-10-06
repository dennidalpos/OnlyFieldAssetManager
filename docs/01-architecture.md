# Architettura

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

Windows conserva gli esiti sincroni dei comandi: `DesktopIo` esegue I/O/calcolo sul worker mentre un `SecondaryLoop` AWT continua a distribuire gli eventi. Stato occupato, blocco delle modifiche, overlay e menu disabilitati impediscono salvataggi concorrenti. Gli aggiornamenti del progetto/storia tornano al chiamante UI dopo il salvataggio riuscito. Il worker appartiene a `DesktopAppState` e termina allo shutdown. [Android Developers](https://developer.android.com/kotlin/coroutines/coroutines-best-practices), [Java SecondaryLoop](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/SecondaryLoop.html).

AUD-15 (6 ottobre): ProjectViewModel esegue i comandi in ordine con Mutex, senza pubblicare edit prima del salvataggio. Ogni edit legge la versione persistita dopo il comando precedente; un save fallito non ripristina snapshot obsoleti. Sessione di navigazione e revisione proteggono pubblicazione e undo: chiusura/cambio progetto conserva gli edit già richiesti, ma impedisce riaperture tardive. Gli snapshot mappa obsoleti sono rifiutati. ProjectStore.load legge progetto e collezioni nella stessa transazione Room; export e letture non combinano righe di versioni diverse. AppRoot mostra un dialogo occupato che blocca gli input mentre i comandi sono pendenti; il collaudo nativo/accessibilità resta in RES-19. Fonti: [Mutex FIFO](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.sync/-mutex/lock.html), [Room withTransaction](https://developer.android.com/reference/androidx/room/RoomDatabaseKt), [Compose Dialog](https://developer.android.com/develop/ui/compose/components/dialog).
