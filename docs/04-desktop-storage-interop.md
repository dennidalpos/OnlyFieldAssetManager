# Storage desktop e interoperabilita

Desktop conserva dati e impostazioni in `data/` accanto all'eseguibile portable. `DesktopStorageManager` usa una working copy, sostituzione atomica e `.lock` per evitare aperture concorrenti.

Android memorizza il progetto in Room cifrato; `EncryptedDatabase` protegge la chiave con Android Keystore e prepara backup/checkpoint prima della migrazione 13--14. Il backup locale non sostituisce l'export `.ofam` per trasferire un progetto.

Entrambe le app usano lo stesso serializer `.ofam`, inclusi allegati, cifratura e base di fusione. Le fixture e i test di interoperabilita verificano i round-trip tra le piattaforme.

Per il formato vedi [contratto](02-domain-data-contract.md); per il pacchetto Windows vedi [rilascio](06-release-and-delivery.md).
