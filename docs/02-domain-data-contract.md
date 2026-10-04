# Dominio e contratto `.ofam`

## Modello

Un progetto contiene organizzazione e siti, aree/piani, apparati e porte, rack, cavi e percorsi, rete logica, alimentazione, media, campi extra, badge, modelli e cestino locale. Il contenimento associa un solo genitore a un apparato o rack; cavi e cicli non sono ammessi.

`ModelValidator` distingue `STRUCTURAL_ERROR`, che blocca l'import, da `DOCUMENTARY_WARNING`, che segnala dati incompleti senza bloccare il salvataggio.

Le connessioni WAN/VPN (`WanVpnConnection`) sono agganciate ai dispositivi, non alle porte. Un riferimento a un apparato inesistente è un errore strutturale; una connessione senza alcuna estremità (apparato o sede) produce `WAN_VPN_WITHOUT_ENDPOINTS` e una con le due estremità sullo stesso apparato `WAN_VPN_SAME_DEVICE`, entrambi avvisi documentali.

## Pacchetto

`.ofam` e uno ZIP con `manifest.json`, `project.json` oppure `project.json.enc` e `attachments/`. Il manifest contiene versione, checksum SHA-256 e, quando necessario, parametri di cifratura.

La versione corrente e 1.11. L'import accetta 1.7--1.10 e rifiuta ogni altra versione. Il decoder ignora chiavi JSON sconosciute; non inventa dati mancanti delle versioni precedenti.

## Protezione e fusione

Un export protetto usa PBKDF2-HMAC-SHA256, AES-256-GCM e IV casuali; anche gli allegati sono cifrati. La verifica della password di progetto supporta gli hash legacy e li aggiorna al formato PBKDF2 dopo uno sblocco valido.

La fusione a tre vie confronta gli elementi per ID contro la base dell'ultimo scambio. Modifiche concorrenti restano conflitti da scegliere; non esiste una fusione automatica silenziosa.

Riferimenti implementativi: `PackageSerializer`, `PasswordHasher`, `ProjectMerger` e `ModelValidator`.

## Fonte

- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
