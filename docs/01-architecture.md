# Architettura

OnlyFieldAssetManager conserva i progetti localmente. Non usa server o sincronizzazione; soltanto il download esplicito della cartografia usa rete.

| Percorso | Responsabilità |
| --- | --- |
| `:shared:core` | Modello, validazione, modifiche, form, grafi, localizzazione. |
| `:shared:exchange` | Pacchetto `.ofam`, protezione, confronto/fusione, XLSX, Markdown, etichette. |
| `:mobile:app` | Android Compose, Room/SQLCipher, SAF, fotocamera, scanner, PDF e stampa. |
| `:pc:app` | Compose Desktop, storage portable, PDFBox e stampa Windows. |
| `shared/configurator` | UI Compose compilata nelle due app; non è un modulo Gradle separato. |

`core` ed `exchange` sono JVM e non dipendono da Android `Context`. `ProjectEdits` applica le modifiche di dominio; `ModelValidator` distingue errori strutturali e avvisi documentali. I form iniziano dall'entità esistente per conservare i campi non esposti.

## Operazioni e stato

- Android: `ProjectViewModel` ordina i comandi, `ProjectStore` legge e salva con transazioni Room; I/O e generazione documenti usano worker. Gli esiti di una schermata/progetto dismessi non riaprono bozze vecchie.
- Windows: `DesktopIo` usa un worker e un `SecondaryLoop` AWT per mantenere la UI attiva. Il pannello occupato blocca gli edit concorrenti; `DesktopStorageManager` salva in modo reversibile.
- La mappa usa `MapUiState` dell'host: vista, camera e selezione sono stato UI, non dati Room o `.ofam`. Le viste sono scelte da `MapView.forLevel`.

Le versioni effettive sono nel [catalogo Gradle](../gradle/libs.versions.toml). Vedi [dominio](02-domain-data-contract.md), [storage](04-desktop-storage-interop.md) e [verifica](05-testing-and-benchmarks.md).

## Fonti

[Room e transazioni](https://developer.android.com/training/data-storage/room), [stato Compose](https://developer.android.com/develop/ui/compose/state-saving), [SecondaryLoop Java](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/java/awt/SecondaryLoop.html). Le proprietà specifiche dell'app derivano dai sorgenti sopra e dai test del repository.
