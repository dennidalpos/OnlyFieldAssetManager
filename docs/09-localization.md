# Localizzazione

Le UI offrono Sistema, Italiano, English ed Español; la lingua predefinita/fallback è l'italiano. Android salva la scelta nelle preferenze, Windows in `data/settings.properties`. `core.i18n.Messages` carica i cataloghi UTF-8 in tre lingue.

Validazione, wizard, UI e generatori ricevono la lingua dell'operazione. Etichette e avvisi sono tradotti; testo inserito dall'utente, codici, password e protocolli restano invariati. L'autore predefinito dei documenti usa una chiave localizzata.

Le chiavi devono esistere in it/en/es. `Messages.plural` sceglie la forma singolare per 1 e quella base negli altri casi supportati. Le etichette degli enum passano dai formattatori in `core.display`, non dai nomi dei simboli Kotlin.

Verificare localizzazione e impaginazione nei [test](05-testing-and-benchmarks.md); gli annunci TalkBack audio/focus restano nella [matrice Android](testing/hardware-checklist.md).

## Fonti

[PropertyResourceBundle Java](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PropertyResourceBundle.html), [regole plurali CLDR](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html).
