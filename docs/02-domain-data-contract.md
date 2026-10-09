# Dominio e contratto `.ofam`

AUD-19: schema Room v2 e formato `.ofam` v1 invariati. Il pacchetto scambiato esclude media recuperabili solo localmente e metadati del cestino; la working copy Windows conserva i payload dell'undo/cestino, protetti dalla stessa password. La base rappresenta il catalogo realmente inviato, con data di revisione invariata per il solo export. Import/sostituzione conservano i riferimenti delle foto del cestino locale.

## Modello

AUD-49 completato: form e validatore rifiutano decimali non finiti senza cambiare il formato JSON; evidenze nell’[audit del 9 ottobre](repo-residuals-2026-10-09.md).

Un progetto contiene sedi, piani, apparati e porte, rack, cavi e percorsi disegnati, passaggi interni, rete logica, alimentazione, media, campi extra, badge, modelli e cestino locale. Il contenimento associa un solo genitore a un apparato o rack; cavi e cicli non sono ammessi.

La gerarchia geografica è Sede → Piano (`Site.areas`). Una sede (`Site`) ha nome, codice, gruppo facoltativo (raggruppamento libero, ad esempio «Scuole») e indirizzo; contiene i propri piani e apparati. Il piano dell'apparato (`Device.areaId`) è facoltativo: senza piano né rack il validatore produce l'avviso `UNPOSITIONED_DEVICE`. Gli elenchi delle sedi sono ordinati per gruppo e poi per nome.

Ogni apparato ha uno stato operativo (`OperationalStatus`: in servizio, spento, dismesso, da verificare; predefinito in servizio), distinto dallo stato del rilievo (`Observation`).

## Tracciamento fisico

Il collegamento fisico è registrato solo dai cavi (`Cable`): estremità A e B sono porte, oppure un apparato quando la porta non è nota. Un cavo conserva etichetta, mezzo, colore, lunghezza, rilievo e note.

I passanti sono porte accoppiate fronte/retro (stesso `passageKey`) di un oggetto passivo: patch panel, presa dati, scatola di giunzione. Il passaggio interno è un `PanelMapping` con le due porte, oppure senza la seconda e `isUnknownPassage` quando è ignoto. Un cavallotto è un cavo tra due porte frontali; una giunta è una scatola di giunzione inserita nel cavo. Il ponte radio è un apparato passante: LAN (fronte, PoE) e RF (retro); la tratta in aria è un cavo con mezzo `RADIO` tra le porte RF delle due unità, così il percorso resta completo tra le sedi.

`ConnectionGraph` percorre cavi e passaggi: un percorso è completo con due apparati attivi agli estremi, incompleto se termina su un passante o un passaggio ignoto, in conflitto con cicli o più cavi sulla stessa porta. `HardwareConfigurator.insertPassage` divide un cavo attraverso un passante libero: il primo tratto conserva id, etichetta e foto. Eliminare un apparato rimuove passaggi, VLAN, PoE e LAG delle sue porte; il ripristino ricrea i passaggi interni.

Gli allegati si collegano a progetto, rack, apparato, piano, cavo o porta (`AttachmentTargetType.PORT`).

`ModelValidator` distingue `STRUCTURAL_ERROR`, che blocca l'import, da `DOCUMENTARY_WARNING`, che segnala dati incompleti senza bloccare il salvataggio.

Le connessioni WAN/VPN (`WanVpnConnection`) sono agganciate ai dispositivi, non alle porte. Un riferimento a un apparato inesistente è un errore strutturale; una connessione senza alcuna estremità (apparato o sede) produce `WAN_VPN_WITHOUT_ENDPOINTS` e una con le due estremità sullo stesso apparato `WAN_VPN_SAME_DEVICE`, entrambi avvisi documentali.

## Presentazione hardware facoltativa

`HardwareSpec.portLayouts` conserva per lato e gruppo una/due righe e l’ordine delle chiavi tecniche delle porte (posizione nel gruppo, nome come fallback per porte legacy). Non cambia l’ordine tecnico né gli ID usati dai cavi. `portPoeOverrides` conserva supporto PoE personalizzato per le stesse chiavi, anche quando è nullo per togliere il supporto del preset. Entrambi i campi hanno lista vuota come valore predefinito, sono riutilizzabili nei modelli e vengono riconciliati con aggiunte/rimozioni di porte.

Sono campi JSON dentro le colonne hardware già esistenti: nessuna tabella o identità Room modificata, nessun cambio di versione `.ofam`. Un pacchetto 1 precedente viene letto con disposizione predefinita. Le vecchie applicazioni ignorano i campi e possono perderli quando risalvano. Dimensioni e profondità esistenti restano nel modello, benché non siano più esposte dalla UI.

## Pacchetto

`.ofam` e uno ZIP con `manifest.json`, `project.json` oppure `project.json.enc` e `attachments/`. Il manifest contiene versione, checksum SHA-256 e, quando necessario, parametri di cifratura.

La versione `.ofam` è 1 (ripartenza greenfield del 5 ottobre 2026): l'import rifiuta ogni altra versione, compresi i pacchetti 1.7--1.11 precedenti. Il decoder ignora chiavi JSON sconosciute. Room è alla versione 2: il fallback generale ricrea il database in upgrade e downgrade, come approvato per i dati di prova; nessuna migrazione o conservazione automatica. I dati da mantenere vanno esportati prima dell'aggiornamento. Modificare lo schema richiede sempre una nuova versione Room; il controllo di identità a versione invariata rimane attivo.

L'import confronta il catalogo degli allegati con i payload disponibili dopo la decifratura. Ogni allegato assente genera `MISSING_ATTACHMENT_PAYLOAD` con il suo ID; una entry attesa dai checksum e assente dal catalogo genera `MISSING_PACKAGE_ENTRY`. Sono avvisi documentali: il progetto resta importabile e conserva i riferimenti ai file mancanti. I byte presenti sono distinguibili dai riferimenti tramite `AttachmentFiles.bytesIn`; gli alias legacy del pacchetto sono supportati.

## Limiti di import/export

Limiti comuni approvati il 5 ottobre 2026: ZIP **256 MiB**, singolo file in chiaro **32 MiB**, totale delle entry decompresse **512 MiB**, **10.000 entry** (directory comprese), PBKDF2 da **1 a 1.000.000** iterazioni. Il costo predefinito di export resta invariato. Per entry cifrate sono ammessi i soli byte aggiuntivi AES-GCM: 16 per il JSON e 28 per un allegato (IV + tag). Salt di 16 byte e IV di 12 byte devono essere esadecimali validi.

Android e Windows leggono il pacchetto da stream e verificano i limiti durante la lettura, prima di accumulare byte eccedenti; anche directory e dati finali concorrono ai limiti. Il rifiuto è strutturale e non modifica la copia locale. Gli export e i salvataggi applicano gli stessi limiti per non produrre copie non riapribili. I payload piccoli usano fino a 8 MiB di cache; oltre 1 MiB per file o esaurita la cache, lo staging usa file AES-256-GCM con chiave e IV effimeri in memoria. JSON e singoli byte array richiesti dai viewer conservano il limite per file. `ProjectPackage` è `AutoCloseable`: chiuderlo dopo consumo o annullamento, senza materializzare l’intera mappa. Import, estrazione ed export applicativi lavorano progressivamente. Formato `.ofam` e limiti invariati; misure su moto g86 e Windows nel [report](testing/import-benchmark-2026-10-05.md). [OWASP: dimensione dopo decompressione](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html).

## Protezione e fusione

Ripristino AUD-26: DEVICE/RACK/CREDENTIAL condividono le regole core. La voce deve appartenere al progetto, il suo ID deve corrispondere al JSON e l'ID dell'entità non deve essere già attivo nello stesso catalogo. Sito originale richiesto per DEVICE; per RACK restano richiesti sito originale registrato e piano associato, quando presenti. Le nuove voci rack registrano originalSiteId nel campo opzionale esistente, senza cambiare Room v2 o `.ofam` v1. Tipo non supportato genera errore prima di rimuovere il cestino. Android salva risultato e rimozione nella stessa transazione; Windows aggiorna il cestino dopo il calcolo riuscito e conserva il rollback di persistenza. AUD-27 (6 ottobre): il ripristino richiede i piani originali nella stessa sede, contenitori/figli e montaggi ancora disponibili. Un contenitore spostato su un altro piano o un figlio ricollocato bloccano il ripristino. ID di porte attive, porte duplicate nel JSON e ID di collocazioni già presenti sono rifiutati; nessuna collocazione saltata o sostituita. Progetto, credenziali, base di scambio, cestino e media restano invariati su rifiuto; si può riprovare dopo aver ripristinato il contesto. Le foto delle porte di un apparato nel cestino restano locali anche quando un apparato attivo riusa l’ID della porta e non vengono esportate. Controlli nel progetto corrente; collisioni tra progetti Android rifiutate dalla persistenza con AUD-28.

Decisione AUD-23 del 6 ottobre 2026: capacità preventiva Android protetta mantenuta prudenziale, senza ulteriore richiesta o persistenza della password. Il budget superiore comprende cifratura, compressione e metadati ZIP; può rifiutare contenuto ancora esportabile vicino a 256 MiB. Limiti e formato restano invariati. La verifica esatta al confine non è un requisito corrente; prova del margine con limite ridotto e pacchetto cifrato reale, nessun nuovo collaudo reale a 256 MiB.

Un export protetto usa PBKDF2-HMAC-SHA256, AES-256-GCM e IV casuali; anche gli allegati sono cifrati. La verifica della password di progetto supporta gli hash legacy e li aggiorna al formato PBKDF2 dopo uno sblocco valido.

La fusione a tre vie confronta gli elementi per ID contro la base dell'ultimo scambio. Modifiche concorrenti restano conflitti da scegliere; non esiste una fusione automatica silenziosa.

Riferimenti implementativi: `PackageSerializer`, `PasswordHasher`, `ProjectMerger` e `ModelValidator`.

## Fonte

- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)

AUD-17: Observation.effectiveStatus risolve null come TO_VERIFY senza creare un rilievo fittizio o modificare il dato persistito. ModelValidator segnala anche l'apparato senza rilievo con UNVERIFIED_DEVICE_OBSERVATION, sempre DOCUMENTARY_WARNING e non bloccante. Gli stati espliciti restano invariati. Nessuna modifica a schema Room, versione del pacchetto o serializzazione.

## Fusione dei dati associati — AUD-25

Le quattro opzioni, attive per impostazione iniziale e selezionabili in entrambe le app, trasferiscono credenziali, configurazioni, alimentazioni e campi extra DEVICE al superstite. Un’opzione disattivata conserva i record nel cestino del duplicato: non restano riferimenti attivi verso l’apparato rimosso. Nei conflitti si conservano entrambi i record con ID, segreti, note e classificazione, senza sovrascrittura o deduplicazione. Le alimentazioni trasferiscono anche il riferimento al duplicato come sorgente di altri apparati.

I file delle configurazioni trasferite conservano ID, classificazione e byte: il destinatario diventa il superstite o la porta copiata; senza copia delle porte il file viene associato al superstite. Foto e file esclusi dal trasferimento restano recuperabili nel cestino e non sono esportati come dati attivi. La fusione che crea un auto-riferimento o un ciclo di alimentazione viene rifiutata prima di modificare progetto/cestino; si corregge il collegamento e si riprova, secondo decisione confermata.

Le nuove voci DEVICE conservano i record associati nel JSON locale esistente. Il ripristino richiede ID liberi e riferimenti ancora disponibili; un rifiuto conserva la voce. Vecchie voci senza dati associati restano leggibili. Per ripristinare i nuovi record occorre questa versione dell’app: una versione precedente ignora il campo opzionale. Nessuna modifica allo schema Room v2 o allo scambio .ofam v1; il cestino locale non viene scambiato.

## Identità tra progetti Android — AUD-28

Room conserva chiavi globali per ciascuna tabella del catalogo. ProjectStore verifica il proprietario degli ID ricevuti nella stessa transazione del salvataggio, prima di aggiornare il progetto o cancellarne l’albero. ID appartenenti a un altro progetto, o con proprietario non risolvibile, producono un errore localizzato. Il controllo copre le 24 tabelle di inventario, inclusi siti/piani/apparati/porte e record associati; valori SQL parametrizzati, query in blocchi di 900 ID. Le normali modifiche dello stesso progetto restano ammesse.

Import nuovo, sostituzione, fusione e ripristino non rimappano implicitamente gli ID e non sostituiscono record di altri progetti. Il rifiuto conserva cataloghi, verificatori, basi, cestino e file. Schema Room v2 e .ofam v1 invariati. Le prove usano database isolati; nessun dato utente modificato.

AUD-29: se nuove alimentazioni tra gli apparati rimasti rendono ciclici i record da ripristinare, il ripristino viene rifiutato prima di ricrearli, anche con più sorgenti. Progetto, cestino e file restano conservati; si correggono i collegamenti e si riprova. Controllo condiviso con la fusione, schema e formato invariati.

## Controllo completo delle alimentazioni — AUD-30

Validazione del catalogo, fusione e ripristino usano un unico controllo del grafo nel modello condiviso. Ogni sorgente concorre al controllo, indipendentemente dall’ordine; nessuna ricorsione e nessuna deduplicazione dei record. Più alimentazioni verso la stessa sorgente non costituiscono da sole un ciclo. Il controllo riguarda tutti i collegamenti, compresi auto-riferimenti e cicli nascosti dietro sorgenti alternative.

Un catalogo ciclico genera POWER_FEED_CYCLE_DETECTED come STRUCTURAL_ERROR, con un messaggio sul grafo del progetto; l’import non restituisce un pacchetto utilizzabile. Fusione e ripristino conservano progetto/cestino sul rifiuto e permettono riprova dopo correzione. Eliminato il precedente percorso che seguiva la sola prima sorgente; nessuna modifica allo schema Room v2 o allo scambio .ofam v1.

### AUD-49 — 9 ottobre 2026

Numeri non finiti e overflow rifiutati dai form e dal modello per carichi W/VA, potenze PoE, budget hardware apparati/modelli, lunghezze, coordinate di posizionamenti/annotazioni/tratte. Errori it/en/es prima della scrittura: guard nei salvataggi Room, storage Windows ed export .ofam; validazioni strutturali indipendenti mantengono i rilievi documentali ammessi. JSON continua a rifiutare i numeri speciali. Il campo budget PoE usa il parser decimale condiviso e finito.

## Integrità di modifica e scambio — 9 ottobre 2026

AUD-52: un piano con annotazioni è referenziato e non può essere eliminato. AUD-57: CIDR IPv4 coerenti con i form (prefisso 0–32); UUID VLAN inesistenti e target di ambito espliciti inesistenti sono errori strutturali. Target sede/apparato omessi sono avvisi documentali. Ambito progetto senza target o con ID del progetto è valido. AUD-63: una VLAN usata da subnet non è eliminabile; per procedere occorre modificare esplicitamente le subnet.

AUD-55: import `.ofam` rifiuta entry ZIP duplicate, ID progetto incoerente e metadati/payload di protezione incompatibili. La cifratura esplicita di un progetto originariamente semplice resta valida; compatibilità degli allegati nei pacchetti protetti preesistenti conservata. Payload temporanei chiusi anche in caso di rifiuto.

AUD-56: enum o liste JSON corrotte nelle righe Room interrompono la lettura; nessuna sostituzione silenziosa con valori predefiniti. Il comando mostra l’errore e non salva, preservando i dati originali. Schema Room v2 e `.ofam` v1 invariati. Fonti primarie: [enumValueOf](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/enum-value-of.html), [ZIP JDK 21](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/zip/ZipInputStream.html), [CIDR RFC 4632](https://www.rfc-editor.org/rfc/rfc4632), consultate il 9 ottobre 2026.

### Ambiti di rete e operazioni distruttive — AUD-64

Policy confermata dall’utente: sedi referenziate da VLAN/subnet non eliminabili; apparati referenziati nell’ambito DEVICE non eliminabili, sostituibili o fondibili. La fusione controlla entrambi gli apparati prima di creare porte, cestino o copie. Le reti restano invariate; occorre rimuovere esplicitamente i riferimenti prima di riprovare. Sedi vuote/apparati non referenziati continuano a seguire i flussi ordinari, incluso ripristino e undo. Rifiuti visibili in inventario/mappa Windows e comandi Android; nessun nuovo schema o formato del cestino.
AUD-62: rimosso il serializer autonomo dei modelli, privo di consumatori applicativi. Il catalogo DeviceModel continua a viaggiare nel progetto attraverso PackageSerializer, con ID, hardware, layout/override PoE, template porte e campi extra conservati nello scambio `.ofam` semplice/protetto. Nessun formato aggiuntivo o migrazione.
