# Lingua dell'app e dei documenti

Data: 3 ottobre 2026

## Scelta e persistenza

Entrambe le app offrono Sistema, Italiano, English ed Español. Sistema usa la lingua del dispositivo; lingue diverse da italiano, inglese e spagnolo ricadono sull'italiano. Android conserva la preferenza nelle SharedPreferences; Windows in `data/settings.properties`. La documentazione del repository resta italiana.

Su Android il selettore è disponibile nell'elenco progetti e negli Strumenti, fuori dagli editor e dal wizard. Su Windows è nel menu Visualizza: il cambio passa dalla stessa conferma delle bozze usata dalla navigazione. Il testo inserito dall'utente resta invariato.

## Cataloghi condivisi

`core.i18n.Messages` è un'istantanea immutabile della lingua. I tre cataloghi `messages_it.properties`, `messages_en.properties` e `messages_es.properties` sono caricati da `PropertyResourceBundle` tramite Reader UTF-8 esplicito, senza dipendenze Android o UI. `MessageFormat` sostituisce i parametri; gli apostrofi letterali nei pattern sono raddoppiati. Le chiavi `text.*` identificano stabilmente i messaggi, quelle nominate le preferenze e le azioni distinte.

Validatori, etichette di dominio, wizard, errori, dialoghi e generatori ricevono `Messages` esplicitamente; le API esistenti mantengono italiano come valore predefinito. Le UI usano `LocalMessages`. La localizzazione conserva codici strutturali, ID e protocolli. Il configuratore successivo estende il contratto `.ofam` a 1.11. Solo i nomi originali del catalogo incorporato sono tradotti; le tipologie personalizzate e i nomi modificati dall'utente sono conservati.

Riferimenti: [ResourceBundle Android](https://developer.android.com/reference/java/util/ResourceBundle), [PropertyResourceBundle con Reader](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/PropertyResourceBundle.html), [MessageFormat](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/text/MessageFormat.html).

## Export e stampa

PDF, XLSX, Markdown, etichette QR e stampa ricevono la lingua catturata all'avvio della generazione. Un cambio successivo non modifica un documento già in lavorazione. Intestazioni, fogli, etichette di dominio e messaggi seguono la lingua; contenuti utente, codici tecnici e attribuzioni cartografiche conservano il loro testo. Le credenziali continuano a essere escluse dai documenti.

Durante la verifica è stata corretta l'esclusione degli apparati collocati sui piani diretti della BU: Markdown, XLSX e PDF Android includono ora anche questi apparati, rispettando i filtri di sede/piano/categoria.

## Verifica

`MessagesTest` controlla parità delle chiavi, parametri e formattazione dei tre cataloghi, fallback e codici dei validatori. `LocalizedExportsTest` verifica Markdown/XLSX nelle tre lingue, testi utente, segreti esclusi e piani diretti. `LanguageTest` verifica preferenza Windows, protezione delle bozze e PDF letti con PDFBox. `LocalizedUiTest` verifica le azioni dei form nelle tre lingue su entrambe le piattaforme; `LocalizedPdfTest` apre i PDF Android reali con PdfRenderer.

I PDF Android sono verificati con strumentazione nativa: Robolectric non implementa PdfDocument. Rimosso il precedente ripiego che produceva un PDF fittizio in caso di errore: ora il fallimento viene propagato. Le prove su emulatore non chiudono il collaudo hardware RES-13.
