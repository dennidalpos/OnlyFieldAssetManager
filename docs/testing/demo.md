# Shared demo

[Retained Italian source](demo.it.md)

fixtures/demo/onlyfield-demo.ofam is synthetic and importable in both applications. It contains no real customer data. It must not silently replace a local project; same-ID import uses normal comparison/confirmation.

The recorded 11 October 2026 version contains 366 devices, 8 racks, 1,007 cables, 14 templates and 7 synthetic attachments. It covers multiple sites, fiber backbones, radio bridges, VLAN/subnet data, provider management, racks and physical routes. Documentary warnings are intentional; structural errors are not expected.

Inspect the connected-sites diagram; SW-COM-LAB/PP-COM-LAB and outlets/radio links; VLANs 10/20/30/90; P3/P4 layout/PoE; seven PNG attachment targets; provider/customer devices; open/incomplete ports and inconsistent observations.

Only when intentionally updating the tracked fixture:

~~~powershell
.\gradlew.bat :shared:exchange:demoPackage --no-parallel --max-workers=1
~~~

This regenerates the file and entity IDs. Inspect the diff; it does not update a demo already installed on a phone. To check existing demo behavior:

~~~powershell
.\gradlew.bat :shared:exchange:test --tests '*Demo*' --no-parallel --max-workers=1
~~~

The historical handoff records 19 passing demo tests. See [Android](hardware-checklist.md) and [Windows](windows-native-matrix.md) for physical qualification limits.
