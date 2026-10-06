# Localizzazione

Le app supportano Sistema, Italiano, English ed Espanol. Una lingua di sistema diversa ricade sull'italiano. Android conserva la scelta nelle preferenze; Desktop in `data/settings.properties`.

`core.i18n.Messages` carica cataloghi UTF-8 e passa l'istanza della lingua a validatori, wizard e generatori. UI e documenti acquisiscono la lingua all'avvio dell'operazione. Testo utente, codici e protocolli non vengono tradotti.

Le chiavi dei cataloghi devono essere presenti nelle tre lingue. Le credenziali restano escluse da PDF, XLSX e Markdown.

AUD-21: messaggi di lettura/salvataggio/pulizia delle preferenze Windows in it/en/es. All'avvio, se le preferenze sono illeggibili, si usa il messaggio italiano predefinito; il file originale viene conservato. Dopo il salvataggio riuscito la lingua scelta si applica anche all'eventuale avviso di pulizia.

AUD-26: messaggi it/en/es per contesto originale assente, tipo non supportato, ID già presente e voce assente/non valida. I messaggi delle nuove chiavi non interpolano contenuto JSON o segreti delle credenziali.

I conteggi visibili usano `Messages.plural(chiave, n)`: per n = 1 legge `chiave.one` (es. «1 cavo»), altrimenti la chiave base («3 cavi»). Basta la categoria CLDR *one*: in it/en/es vale solo per 1, mentre *many* riguarda i milioni. Enum interni (mezzo del cavo, tipo di mappatura) passano sempre da `core.display` e non compaiono come nomi di codice.

## Fonti

- [PropertyResourceBundle](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PropertyResourceBundle.html)
- [Regole di plurale CLDR](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html)
- [MessageFormat](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/MessageFormat.html)

AUD-17: intestazione document.warnings in it/en/es; stato effettivo del rilievo e avvisi acquisiscono gli stessi Messages della generazione. Note e nomi utente restano invariati. Verificati Markdown/XLSX, PDF Windows e PDF Android nativo API 37 nelle tre lingue; vedi documentazione di verifica.
