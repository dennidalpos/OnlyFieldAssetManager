# Restyling tecnico elegante — 9 ottobre 2026

## Identità e tema

Android e Windows condividono una palette blu profondo/turchese, superfici neutre e titoli semibold con caratteri di sistema. `OnlyFieldTheme` definisce esplicitamente i colori di testo, contenitori, bordi ed errori in entrambi i temi: le superfici non ereditano più i toni viola Material predefiniti. Controlli a 8 dp, schede a 12 dp e dialoghi a 16 dp; spaziatura 4/8/12/16/24 dp conservata.

L'icona Android rappresenta un segnaposto con tre nodi collegati: fondo blu notte, contorno chiaro e rete turchese. Foreground e versione monocromatica condividono la sagoma aperta, entro il cerchio sicuro centrale di diametro 66 dp del viewport 108 dp. Le quattro risorse launcher esistenti, incluse quelle round, referenziano gli stessi livelli aggiornati. Il marchio compare anche nelle schermate di ingresso e nella barra Desktop; l'icona della finestra/eseguibile Windows resta invariata.

## Presentazione

- Android: ripresa dell'ultimo progetto in un pannello dedicato, importazione sotto il pannello e barra superiore con nome breve OnlyField e selettore lingua. Schermata iniziale scorrevole, schede con bordi e metadati distinti, badge turchesi e ricerca con icona.
- Windows: barra superiore, rail/sidebar e contenuto hanno superfici distinte; selezioni, stati vuoti e pannelli editor hanno contorni riconoscibili.
- Mappe: strumenti, canvas e dettagli sono separati visivamente senza cambiare le soglie adattive, lo stato della vista o i colori tecnici di oggetti e collegamenti.
- Ricerca: il suggerimento occupa una sola riga con ellissi; in precedenza poteva espandere la barra fino a tre righe, anche con campo `singleLine`.

Navigazione, callback, localizzazioni, preferenze del tema, database e formato `.ofam` restano compatibili. Nessuna dipendenza aggiunta. Questa decisione sostituisce la conservazione estetica di colori e forme prevista nella [revisione precedente](ui-ux-audit-2026-10-08.md), mantenendone i comportamenti.

## Evidenze

Compilazione iniziale e successiva di Android/Desktop riuscite. Otto scenari Desktop di `DesktopUxLayoutTest` passati: 1024×768 e 1360×860, chiaro/scuro, testo 1,0/1,3; progetti, inventario, editor e porte. Screenshot aggiornati in `pc/app/build/reports/restyling`.

Controllo dei colori dichiarati: 46 coppie testo/sfondo e bordi, nessun rapporto sotto soglia; minimo testo 5,17:1. Sono esclusi dal risultato colori tecnici, fotografie, planimetrie e controlli disabilitati. Report in `build/reports/restyling-20261009/contrast.json`.

Android moto g86, API 36: `OK (5 tests)` in `native-final.txt`, con database in memoria e progetti sintetici. Copertura:

- Progetti e inventario: 32 combinazioni di 360/412/600/840 dp, temi e testo 1,0/1,3; ricerca senza risultati e cancellazione del filtro. Ripetizione finale `OK (1 test)` in `native-navigation-final.txt`, con verifica esplicita barra/rail. Ogni densità sintetica ricrea l'host Compose conservando il ViewModel: il cambio della sola `LocalDensity` lasciava la misura precedente in alcune prime acquisizioni.
- Mappa: 20 combinazioni di 360/412/600/840/1024 dp, temi e testo 1,0/1,3; Espandi/Riduci, selezione e zoom conservati.
- Configuratore a 48 porte: 8 combinazioni di 360/412 dp, temi e testo 1,0/1,3; disposizione, PoE, salvataggio e annullamento.
- Form lungo: ultimo campo e conferme raggiungibili con tastiera reale aperta, sul viewport del dispositivo. Non esteso a tutta la matrice dimensioni/temi.
- Icona: sagoma e zona sicura verificate sui drawable Android; anteprime a 48 e 432 px, maschere circolare/arrotondata, colore/monocromatico. Il confronto di alpha ammette due livelli su 255 ai bordi antialias, senza modificare la geometria.

Immagini ispezionate nelle tavole riepilogative e nei dettagli rappresentativi. Evidenze in `build/reports/restyling-20261009`: `android-verified`, `adaptive`, `configurator`, `icon-preview.png` e `preview.png`. Le prime immagini in `android` e `android-final` sono superate da `android-verified` per progetti/inventario. APK principale aggiornato con `adb install -r`, APK test rimosso, MainActivity riaperta. Confronto dei quattro file privati prima/dopo: unica variazione `files/profileInstalled`; altri file controllati invariati. Nessun processo EXE avviato e nessun dato Desktop personale usato.

Non verificati: TalkBack audio/focus, rotazione reale, tablet fisici, intera matrice di editor e comandi dell'EXE. Le larghezze simulate sul telefono e gli host Desktop non equivalgono a questi collaudi. RES-13/19/23/24 restano aperti; nessuna suite generale ripetuta. Commit e push su main successivamente richiesti dall’utente per il cambio sessione; passaggio di consegne in [roadmap](../roadmap.md).

### Comandi eseguiti

```powershell
.\gradlew.bat :pc:app:classes :mobile:app:assembleDebug --no-parallel --max-workers=1
.\gradlew.bat :pc:app:test --tests '*DesktopUxLayoutTest' --no-parallel --max-workers=1
.\gradlew.bat :mobile:app:assembleDebug :mobile:app:assembleDebugAndroidTest --no-parallel --max-workers=1
```

Native: `adb shell am instrument -w -r -e class com.onlyfield.assetmanager.VisualThemeNativeTest,com.onlyfield.assetmanager.AdaptiveUiNativeTest,com.onlyfield.assetmanager.ConfiguratorMatrixNativeTest#densePortConfigurationCanSaveAndDiscardAcrossDisplayMatrix -e matrixEvidence true com.onlyfield.assetmanager.test/androidx.test.runner.AndroidJUnitRunner`. Rerun finale limitato a `VisualThemeNativeTest#projectsAndInventoryRemainReadableAcrossThemesAndWidths`. I wrapper di esecuzione controllano anche la riga `OK`, perché il comando instrumentation può terminare con exit code zero dopo un fallimento JUnit.

## Fonti

Documentazione ufficiale consultata il 9 ottobre 2026:

- [Android adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive): livelli, maschere e zona sicura.
- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3): ruoli cromatici, tipografia e forme.
- [Accessibilità Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults): semantica e target touch.
