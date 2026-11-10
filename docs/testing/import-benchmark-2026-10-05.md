# Benchmark storico degli import grandi — 5 ottobre 2026

Una fixture valida da 511 MiB espansi provocava `OutOfMemoryError` su moto g86 API 36 prima della correzione. Lettura, estrazione ed export sono poi diventati progressivi, con staging cifrato e chiusura esplicita di `ProjectPackage`; formato `.ofam` v1 e limiti invariati.

Dopo la correzione passarono sette fixture valide su Windows 11/JDK 21 e moto g86, con persistenza SQLCipher isolata e verifica SHA-256 di ogni allegato. Campionamento heap/PSS e heartbeat Main non catturano necessariamente picchi brevissimi; una esecuzione per fixture non è una garanzia per ogni dispositivo.

| Fixture | Moto: lettura / totale | Picco heap campionato | Main massimo |
| --- | ---: | ---: | ---: |
| ZIP circa 255 MiB | 5.449 / 7.033 ms | 203,8 MB | 41 ms |
| 511 MiB espansi | 3.908 / 7.144 ms | 234,2 MB | 40 ms |
| 255 MiB cifrati | 7.558 / 9.137 ms | 229,8 MB | 40 ms |
| KDF 1.000.000 | 4.950 / 5.282 ms | 10,6 MB | 18 ms |

L'EXE Windows reale importò la fixture da 511 MiB in un profilo isolato: 16 allegati salvati con SHA-256 coincidenti e staging vuoto dopo la chiusura. Non era una prova completa di focus e blocco input, ancora in [RES-23](windows-native-matrix.md). I log storici erano in `build/reports/import-benchmark/`, ignorati da Git.

## Riproduzione

Da PowerShell nella radice del repo, con spazio disco e profilo isolati:

```powershell
.\gradlew.bat -I tools\benchmarks\import.init.gradle :shared:exchange:importBenchmark -PbenchmarkMode=generate -PbenchmarkPath=build/import-benchmark --no-parallel --max-workers=1
.\gradlew.bat -I tools\benchmarks\import.init.gradle :shared:exchange:importBenchmark -PbenchmarkMode=measure -PbenchmarkPath=build/import-benchmark/expanded-511.ofam --no-parallel --max-workers=1
```

Questi comandi generano centinaia di MiB in output ignorati; non sono stati rieseguiti nella revisione documentale. Fonte: [ADB](https://developer.android.com/tools/adb), [memoria con dumpsys](https://developer.android.com/tools/dumpsys), [CipherInputStream e autenticazione](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/javax/crypto/CipherInputStream.html).
