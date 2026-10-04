# Localizzazione

Le app supportano Sistema, Italiano, English ed Espanol. Una lingua di sistema diversa ricade sull'italiano. Android conserva la scelta nelle preferenze; Desktop in `data/settings.properties`.

`core.i18n.Messages` carica cataloghi UTF-8 e passa l'istanza della lingua a validatori, wizard e generatori. UI e documenti acquisiscono la lingua all'avvio dell'operazione. Testo utente, codici e protocolli non vengono tradotti.

Le chiavi dei cataloghi devono essere presenti nelle tre lingue. Le credenziali restano escluse da PDF, XLSX e Markdown.

## Fonti

- [PropertyResourceBundle](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PropertyResourceBundle.html)
- [MessageFormat](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/MessageFormat.html)
