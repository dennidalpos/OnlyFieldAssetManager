# OnlyFieldAssetManager

Field survey editor for networks, devices, cabling and sites. Projects stay on the device; Android and Windows exchange copies manually through `.ofam` v1 packages. Only the explicit download of map tiles needs a network connection.

## Features

- **Survey data model**: sites, floors, devices, racks, ports, cables and patching, with photos attached to the right target.
- **Map views**: project overview of sites, site topology and floor plans, with logical, device and list views where applicable.
- **Object configurator**: configure devices and ports from the map or the inventory.
- **Documents**: export PDF, XLSX and Markdown reports from a project.
- **Manual exchange**: `.ofam` packages with comparison, replace or three-way merge and conflict selection; password-protected copies are supported.
- **Local-first storage**: Room with SQLCipher on Android; a portable `data/` folder next to the executable on Windows. Interrupted saves can be recovered.
- **Localization**: multiple UI languages.

## Platforms

| Platform | Module | Notes |
| --- | --- | --- |
| Android 14+ | `mobile/app` | `minSdk` 34, `targetSdk` 35 |
| Windows 11 x64 | `pc/app` | Compose Desktop, portable ZIP |
| Shared | `shared/core`, `shared/exchange`, `shared/configurator` | Domain model, `.ofam` exchange, configurator |

## Build

Requirements: JDK 17 for the Kotlin code, a JDK 21 toolchain with `jpackage` for the Windows package, Android SDK for the mobile app. Dependency versions are in [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

```powershell
.\gradlew.bat :mobile:app:assembleDebug     # Android debug APK
.\gradlew.bat :pc:app:packagePortable       # Windows portable folder and ZIP
```

`gradle.properties` sets `org.gradle.java.home` to the Android Studio JBR path; adjust it for your machine. The repository contains a manual/tag-triggered release workflow, but GitHub Actions are disabled for this repository: build locally. Details in [Build and release](docs/06-release-and-delivery.md).

## Documentation

Guides and technical documents are written in Italian.

- User guides: [Android](docs/guide/android.md), [Windows](docs/guide/windows.md), [exchange and documents](docs/guide/exchange-and-documents.md), [demo and checks](docs/testing/demo.md).
- Product decisions: [plan.md](plan.md); recorded verifications: [roadmap.md](roadmap.md).

| Topic | Document |
| --- | --- |
| Modules and boundaries | [Architecture](docs/01-architecture.md) |
| Data, validation, `.ofam` | [Domain and data contract](docs/02-domain-data-contract.md) |
| PDF, XLSX, Markdown, filters | [Documents](docs/03-export-and-documents.md) |
| Persistence and recovery | [Storage and interoperability](docs/04-desktop-storage-interop.md) |
| Tests and demo | [Verification](docs/05-testing-and-benchmarks.md) |
| Distribution | [Build and release](docs/06-release-and-delivery.md) |
| Application workflows | [Workflows](docs/07-workflows.md) |
| Scenes and floor plans | [Map](docs/08-floor-map.md) |
| Languages | [Localization](docs/09-localization.md) |
| Objects and ports | [Configurator](docs/10-object-configurator.md) |

The [Android](docs/testing/hardware-checklist.md) and [Windows](docs/testing/windows-native-matrix.md) matrices separate native checks still open from automated ones. Brand assets are described in [assets/README.md](assets/README.md). The PDF `OnlyFieldAssetManager_Manuale_Utente.pdf` is a historical version; use the guides above for current workflows.

## Security

Project data is stored locally; there is no cloud backend. See [SECURITY.md](SECURITY.md) to report a vulnerability.

## License

Proprietary, all rights reserved. See [LICENSE](LICENSE). Third-party dependencies keep their own licenses.
