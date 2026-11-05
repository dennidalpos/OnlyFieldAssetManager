# Storage, protection and recovery

[Retained Italian source](04-desktop-storage-interop.it.md)

## Android

EncryptedDatabase.open opens Room v3 using SQLCipher. The database key is wrapped by Android Keystore. ProjectStore uses transactions for coherent reads/commands and checks ID ownership before deletion or replacement.

A schema change without a configured migration recreates tables. Export projects before a schema-changing installation. Keep this limit distinct from verified recovery of the current schema.

## Windows

DesktopStorageManager stores projects/preferences under `data/` beside the executable. Working copy, exchange base, trash and media use reversible saves. A .lock file prevents concurrent use of the same copy. Protected-project media stays encrypted in local staging. “Export and open” creates a plaintext copy at the user's selected destination.

DesktopIo performs work on its worker. Busy state blocks concurrent edits. A failed save retains the draft for retry; it is not a successful undo.

## Recovery and trash

FileRecovery retains journals, encrypted staging and backups. Before commit it restores previous files; after commit it completes new files and retries cleanup. Unverifiable recovery blocks the project and preserves evidence. Windows can require the local project password; Android reconciles file state with the Room transaction using a distinct Keystore-wrapped recovery key.

Local trash retains objects/media required for restoration. Restore rejects missing original contexts, occupied IDs, incompatible placement and power cycles without relocating objects automatically. Merge exposes four explicit transfer choices for associated credentials/configuration/power/extra fields; untransferred content stays in trash.

Process interruption and normal reboot records are historical evidence in [roadmap](../roadmap.md). Forced shutdown/power loss during writes and native recovery focus remain separate qualification work: RES-13, RES-19 and RES-23. Do not delete recovery records or package over personal data to obtain a clean build.

See [Windows guide](guide/windows.md), [Android guide](guide/android.md) and [verification](05-testing-and-benchmarks.md).
