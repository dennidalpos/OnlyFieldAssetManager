# Configuratore di apparati e porte

La UI condivisa in `shared/configurator` compone i form dei due sistemi; il dominio resta in `core.forms` e `core.model.ConnectionGraph`.

## Aggiunta e modifica

L'inserimento segue tipo → preset/modello e disegno → dati essenziali → conferma. Nome e sede sono i minimi dell'apparato; dettagli avanzati possono restare chiusi. La modifica parte dal record corrente e usa `copy()` per conservare campi non esposti. Hardware, modelli e preset non cambiano gli ID delle porte già collegate.

Il layout grafico delle porte ha fronte/retro, gruppi e ordine visuale indipendente dall'ordine tecnico. `HardwareSpec.portLayouts` e `portPoeOverrides` sono nel JSON hardware; spostare una porta nel disegno non deve cambiare cavi o ID. Dimensioni/profondità già salvate restano nel modello anche se non esposte dai form correnti.

## Scheda rapida

Una porta può mostrare collegamento, percorso fisico/logico, PoE e rilievo. Le azioni esplicite permettono Collega, Inserisci passaggio, Foto e Scollega; i passanti associano fronte e retro senza inventare una tratta ambigua. Layout e supporto PoE sono bozze con Salva; collegamenti e foto hanno il proprio commit. La foto può avere target PORT o CABLE.

I form testuali restano entro 640 dp; disegni e liste tecniche usano lo spazio disponibile. Sui telefoni i dialoghi di lavoro diventano a schermo intero, con azioni accessibili con tastiera aperta. Le prove di hardware/UX reali sono nelle matrici [Android](testing/hardware-checklist.md) e [Windows](testing/windows-native-matrix.md).

## Gestione di terzi

`Device.management` indica se l'apparato è gestito dal tecnico, dal fornitore, dal cliente o da un soggetto sconosciuto. Nome, contatto e riferimento contratto del fornitore sono facoltativi. Tornare a “Gestito dal tecnico” nasconde tali dettagli dalle viste/export senza cancellarli dal record.

## Fonti

[Layout adattivi Compose](https://developer.android.com/develop/ui/compose/layouts/adaptive), [stato Compose](https://developer.android.com/develop/ui/compose/state-saving).
