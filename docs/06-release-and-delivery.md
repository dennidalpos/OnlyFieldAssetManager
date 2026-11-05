# Build and release

[Retained Italian source](06-release-and-delivery.it.md)

## Prerequisites

Dependency versions are in [libs.versions.toml](../gradle/libs.versions.toml). App versions are in the two application build files. Kotlin/JVM targets Java 17; Windows packaging resolves a JDK 21 toolchain with jpackage. Android currently declares minSdk 34, targetSdk 35 and compileSdk 37.

gradle.properties pins org.gradle.java.home to a workstation-specific Android Studio JBR path. Override or configure it for your own machine. Do not copy another developer's path into a guide or infer that the directory exists everywhere.

## Packages

~~~powershell
.\gradlew.bat :mobile:app:assembleDebug
.\gradlew.bat :pc:app:packagePortable
~~~

The desktop task creates dist/OnlyFieldAssetManager and a portable ZIP. Local data belongs beside the EXE and must not enter the ZIP. createDistributable refuses a source runtime already containing data; preserve and move that data before an explicitly authorized rebuild. prepare-release.ps1 requires an empty destination. Never use a personal profile for packaging experiments.

## Workflow and publication

[release.yml](../.github/workflows/release.yml) is manual/tag-triggered. Its declared steps run JVM/Compose suites, build debug APK/portable ZIP and prepare SHA-256 checksums. Only the tag-triggered publishing job creates a GitHub release. Workflow presence does not prove GitHub Actions are enabled or that a run succeeded; inspect the current repository settings/run separately.

A local build does not publish. A previous v1.0.1 release is historical evidence, not qualification of the current checkout. Keep build, native verification and publication outcomes distinct. See [verification](05-testing-and-benchmarks.md) and [Windows matrix](testing/windows-native-matrix.md).
