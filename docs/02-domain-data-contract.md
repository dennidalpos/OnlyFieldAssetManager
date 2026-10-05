# Dominio e contratto `.ofam`

## Modello

Un progetto contiene sedi, piani, apparati e porte, rack, cavi e percorsi disegnati, passaggi interni, rete logica, alimentazione, media, campi extra, badge, modelli e cestino locale. Il contenimento associa un solo genitore a un apparato o rack; cavi e cicli non sono ammessi.

La gerarchia geografica è Sede → Piano (`Site.areas`). Una sede (`Site`) ha nome, codice, gruppo facoltativo (raggruppamento libero, ad esempio «Scuole») e indirizzo; contiene i propri piani e apparati. Il piano dell'apparato (`Device.areaId`) è facoltativo: senza piano né rack il validatore produce l'avviso `UNPOSITIONED_DEVICE`. Gli elenchi delle sedi sono ordinati per gruppo e poi per nome.

## Tracciamento fisico

Il collegamento fisico è registrato solo dai cavi (`Cable`): estremità A e B sono porte, oppure un apparato quando la porta non è nota. Un cavo conserva etichetta, mezzo, colore, lunghezza, rilievo e note.

I passanti sono porte accoppiate fronte/retro (stesso `passageKey`) di un oggetto passivo: patch panel, presa dati, scatola di giunzione. Il passaggio interno è un `PanelMapping` con le due porte, oppure senza la seconda e `isUnknownPassage` quando è ignoto. Un cavallotto è un cavo tra due porte frontali; una giunta è una scatola di giunzione inserita nel cavo. Il ponte radio è un apparato passante: LAN (fronte, PoE) e RF (retro); la tratta in aria è un cavo con mezzo `RADIO` tra le porte RF delle due unità, così il percorso resta completo tra le sedi.

`ConnectionGraph` percorre cavi e passaggi: un percorso è completo con due apparati attivi agli estremi, incompleto se termina su un passante o un passaggio ignoto, in conflitto con cicli o più cavi sulla stessa porta. `HardwareConfigurator.insertPassage` divide un cavo attraverso un passante libero: il primo tratto conserva id, etichetta e foto. Eliminare un apparato rimuove passaggi, VLAN, PoE e LAG delle sue porte; il ripristino ricrea i passaggi interni.

Gli allegati si collegano a progetto, rack, apparato, piano, cavo o porta (`AttachmentTargetType.PORT`).

`ModelValidator` distingue `STRUCTURAL_ERROR`, che blocca l'import, da `DOCUMENTARY_WARNING`, che segnala dati incompleti senza bloccare il salvataggio.

Le connessioni WAN/VPN (`WanVpnConnection`) sono agganciate ai dispositivi, non alle porte. Un riferimento a un apparato inesistente è un errore strutturale; una connessione senza alcuna estremità (apparato o sede) produce `WAN_VPN_WITHOUT_ENDPOINTS` e una con le due estremità sullo stesso apparato `WAN_VPN_SAME_DEVICE`, entrambi avvisi documentali.

## Pacchetto

`.ofam` e uno ZIP con `manifest.json`, `project.json` oppure `project.json.enc` e `attachments/`. Il manifest contiene versione, checksum SHA-256 e, quando necessario, parametri di cifratura.

La versione è 1 (ripartenza greenfield del 5 ottobre 2026): l'import rifiuta ogni altra versione, compresi i pacchetti 1.7--1.11 precedenti. Il decoder ignora chiavi JSON sconosciute. Anche il database Room è alla versione 1: uno schema diverso viene ricreato vuoto e i dati di prova si reimportano dal pacchetto demo.

## Protezione e fusione

Un export protetto usa PBKDF2-HMAC-SHA256, AES-256-GCM e IV casuali; anche gli allegati sono cifrati. La verifica della password di progetto supporta gli hash legacy e li aggiorna al formato PBKDF2 dopo uno sblocco valido.

La fusione a tre vie confronta gli elementi per ID contro la base dell'ultimo scambio. Modifiche concorrenti restano conflitti da scegliere; non esiste una fusione automatica silenziosa.

Riferimenti implementativi: `PackageSerializer`, `PasswordHasher`, `ProjectMerger` e `ModelValidator`.

## Fonte

- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
