# Architecture

[Retained Italian source](01-architecture.it.md)

Projects are local. There is no server or automatic synchronization. Downloading map tiles is an explicit network operation.

| Component | Responsibility |
| --- | --- |
| `:shared:core` | Model, validation, edits, forms, connection graphs and localization |
| `:shared:exchange` | `.ofam` packages, encryption, comparison/merge, XLSX, Markdown and labels |
| `:mobile:app` | Android Compose UI, Room/SQLCipher, Storage Access Framework, camera/scanner, PDF and printing |
| `:pc:app` | Compose Desktop UI, portable storage, PDFBox and Windows printing |
| `shared/configurator` | Shared Compose sources compiled into both applications; not a separate Gradle module |

Core and exchange are JVM modules without Android Context dependencies. ProjectEdits applies domain changes; ModelValidator distinguishes structural errors from documentary warnings. Editors start from existing entities to preserve fields not exposed by the form.

## Operations and state

On Android, ProjectViewModel orders commands and ProjectStore uses Room transactions. File work and report generation use workers. Results belonging to a dismissed screen/project must not reopen old drafts.

On Windows, DesktopIo uses a worker and an AWT SecondaryLoop to keep the UI responsive. The busy panel blocks concurrent edits; DesktopStorageManager saves through reversible operations.

MapUiState belongs to the host UI: selected view, camera and selection are not Room or package data. MapView.forLevel determines available views.

Dependency versions are in [the Gradle catalog](../gradle/libs.versions.toml). See [data contract](02-domain-data-contract.md), [storage](04-desktop-storage-interop.md) and [verification](05-testing-and-benchmarks.md). Local tests do not qualify real devices, printing or interrupted power loss.
