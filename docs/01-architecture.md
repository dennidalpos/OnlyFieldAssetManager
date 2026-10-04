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

## Confini

I progetti sono locali. La rete serve solo al download esplicito della cartografia; i dati restano utilizzabili offline. Android usa Room cifrato, Windows una working copy con salvataggio atomico e file `.lock`.

Le dipendenze e le versioni effettive sono nel catalogo Gradle [libs.versions.toml](../gradle/libs.versions.toml). Per contratto e persistenza vedi [dominio](02-domain-data-contract.md); per build vedi [rilascio](06-release-and-delivery.md).
