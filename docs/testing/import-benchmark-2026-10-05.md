# Import grandi — 5 ottobre 2026

## Metodo e baseline

Prove esplicite sul serializzatore di produzione, con fixture valide generate da `LargeImportBenchmark`. Gli allegati hanno identificativi UUID, catalogo e checksum SHA-256; nessun valore reale o credenziale. La password delle fixture è un segnaposto di prova. La generazione non modifica i progetti delle app.

Windows 11 Pro 10.0.26300, Intel Core i7-10700, 34.242.015.232 byte di RAM; JVM JDK 21, heap massimo imposto a 2 GiB. Heap campionato ogni 10 ms; disponibili anche i picchi dei pool JVM. Moto g86 5G, API 36, heap massimo 256 MiB; heap e PSS campionati ogni 50 ms. Un heartbeat sul Main ogni 16 ms misura il massimo intervallo fra due esecuzioni. PSS è una misura della RAM attribuibile al processo, diversa dall'heap Java. Il campionamento può perdere picchi brevi e influenza la misura.

Baseline prima della correzione:

| Fixture | Windows lettura | Windows picco heap campionato | Moto g86 lettura | Esito Android |
| --- | ---: | ---: | ---: | --- |
| 32 MiB in chiaro | 408 ms | 173.600.048 B | 730 ms | Superato; heap 136.790.416 B, PSS 166.744 KiB |
| 32 MiB cifrati | 1.009 ms | 179.361.136 B | 1.290 ms | Superato; heap 113.119.744 B, PSS 144.317 KiB |
| 10.000 entry | 1.256 ms | 168.005.064 B | 3.803 ms | Superato; heap 44.160.016 B, PSS 133.472 KiB |
| ZIP circa 255 MiB | 1.715 ms | 953.861.736 B | — | Interrotto dopo il difetto successivo |
| 511 MiB decompressi | 2.655 ms | 1.181.940.656 B | 1,308 s al fallimento | `OutOfMemoryError` |

Errore reale: `Failed to allocate a 33554448 byte allocation ... growth limit 268435456`, in `ByteArrayOutputStream.toByteArray` / `PackageSerializer.kt:251`. Il pacchetto è valido: ZIP 526.331 byte, payload 535.822.336 byte, 18 entry. AUD-10 registra il difetto; l'utente ha autorizzato la gestione progressiva mantenendo limiti e formato.

Una prima correzione ha completato sul telefono anche la persistenza isolata SQLCipher: lettura 3.921 ms, totale 5.131 ms, heap campionato 237.110.552 byte, Main massimo 18 ms. Il margine rispetto a 256 MiB era limitato; estrazione ed export sono stati quindi resi progressivi prima delle misure conclusive.

## Esiti conclusivi

Tutte le sette fixture valide passano su entrambi i runtime. Sul moto g86 è abilitata la persistenza SQLCipher isolata e verificato lo SHA-256 di ogni allegato estratto. Ogni prova chiude il pacchetto e verifica l’assenza dello staging residuo.

| Fixture | Windows lettura | Windows picco heap | Moto lettura / totale | Moto picco heap / PSS | Main massimo |
| --- | ---: | ---: | ---: | ---: | ---: |
| 32 MiB in chiaro | 523 ms | 11.195.968 B | 1.080 / 1.391 ms | 154.464.184 B / 188.671 KiB | 29 ms |
| 32 MiB cifrati | 1.731 ms | 435.740.896 B | 1.782 / 2.113 ms | 187.847.920 B / 199.747 KiB | 32 ms |
| 10.000 entry | 1.092 ms | 164.011.088 B | 4.240 / 15.520 ms | 53.206.912 B / 146.924 KiB | 18 ms |
| ZIP circa 255 MiB | 1.787 ms | 130.440.152 B | 5.449 / 7.033 ms | 203.845.320 B / 233.445 KiB | 41 ms |
| 511 MiB decompressi | 3.311 ms | 86.042.752 B | 3.908 / 7.144 ms | 234.248.912 B / 249.802 KiB | 40 ms |
| 255 MiB cifrati | 3.535 ms | 934.024.592 B | 7.558 / 9.137 ms | 229.823.968 B / 258.104 KiB | 40 ms |
| KDF 1.000.000 | 946 ms | 47.707.920 B | 4.950 / 5.282 ms | 10.602.224 B / 76.511 KiB | 18 ms |

Picchi campionati comprensivi degli oggetti non ancora raccolti; una singola esecuzione per fixture. Il valore Windows del cifrato grande resta elevato: JCE autentica il singolo file e il processo conserva allocazioni fino alla raccolta. Nessun OOM; queste misure non garantiscono gli stessi consumi su ogni dispositivo.

Collaudo nell’EXE Windows reale, con dati isolati in `build/native-window-app/data`: import di `expanded-511.ofam`, confronto e conferma, salvataggio dei 16 allegati, working copy da 526.567 byte, pannello «Operazione in corso…» visibile e ritorno alla mappa. Al momento del campionamento durante il salvataggio il processo JVM rispondeva e aveva working set 1.049.989.120 byte. Gli SHA-256 dei 16 payload salvati coincidono con quelli originali. Dopo la chiusura della finestra lo staging era vuoto. Non è una misura temporale completa della finestra né una prova del blocco di tutti i comandi: tali dettagli rimangono nel collaudo RES-23.

Verifica dopo i difetti: suite completa **345 test** (108 Core, 69 Exchange, 133 Desktop, 35 Android JVM), zero fallimenti/errori/saltati; APK, APK test e portable compilati (`BUILD SUCCESSFUL in 1m 46s`). Revisione della gestione risorse e rollback foto: suite Exchange/Desktop/Android JVM `BUILD SUCCESSFUL in 1m 38s`. Cinque regressioni `StagedPayloadTest` coprono staging cifrato, successo, manifest errato, tag GCM errato, interruzione e mancata sovrascrittura del file preesistente. Suite nativa **14 test superati su moto g86 API 36 e Pixel 9 API 37**; nessuna scrittura nel database della demo.

AUD-10 e RES-22 completati e rimossi dal tracker. Log, hash, conteggi e catture locali in `build/reports/import-benchmark/` e `build/reports/native-windows/`; tali evidenze sono ignorate da Git.

## Riproduzione

Generare le fixture negli output ignorati:

```powershell
.\gradlew.bat -I tools\benchmarks\import.init.gradle :shared:exchange:importBenchmark -PbenchmarkMode=generate -PbenchmarkPath=build/import-benchmark --no-parallel --max-workers=1
.\gradlew.bat -I tools\benchmarks\import.init.gradle :shared:exchange:importBenchmark -PbenchmarkMode=measure -PbenchmarkPath=build/import-benchmark/expanded-511.ofam --no-parallel --max-workers=1
```

Le fixture includono anche `encrypted-255.ofam` e `encrypted-kdf-1000000.ofam`. Il caso da 511 MiB è in chiaro: cifrare tutti i payload rende il contenuto poco comprimibile e supera il limite ZIP di 256 MiB. I checksum e le dimensioni delle fixture effettivamente misurate sono conservati nei report locali; la cifratura usa IV casuali, perciò una nuova generazione cambia l'hash del pacchetto.

Su Android `LargeImportBenchmarkTest#measureImport` senza argomenti usa il piccolo demo incluso negli asset. L'argomento `benchmarkFixture` seleziona un file nella sottocartella `import-benchmark` della cartella esterna dell'app; `benchmarkPersist=true` abilita database e media isolati nella cache. La prova confronta il catalogo riaperto, la dimensione e lo SHA-256 di ogni file estratto, chiude il pacchetto e verifica la rimozione dello staging. Non scrive nel database applicativo.

## Fonti e limiti

Fonti ufficiali consultate il 5 ottobre 2026: [ADB](https://developer.android.com/tools/adb), [PSS e diagnostica della memoria](https://developer.android.com/tools/dumpsys), [Cipher e autenticazione GCM](https://developer.android.com/reference/javax/crypto/Cipher), [limitazioni di CipherInputStream per l'autenticazione](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/crypto/CipherInputStream.html). La decifratura usa `Cipher.update`/`doFinal` espliciti; un tag errato viene propagato o classificato come allegato non decifrabile, mai ignorato.

Le prove strumentali verificano il worker e il Main, ma non sostituiscono il collaudo della finestra, del focus, dei dialoghi o di altri dispositivi. Le prove su altri dispositivi e il collaudo completo di focus/dialoghi/stampa rimangono rispettivamente RES-19 e RES-23.
