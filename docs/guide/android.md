# Android user guide

[Retained Italian source](android.it.md)

Android 14+ stores projects in Room/SQLCipher. Export projects to .ofam before installing a build with a different schema: missing migrations currently trigger database recreation.

## Survey a project

1. Open/create a project; add a site and an optional floor.
2. Add devices/racks in Inventory. Device name/site are required; floor/placement can be completed later.
3. Open Map. Project level starts with connected sites, site level with topology, floor level with a floor plan. Logical, Devices and List are available where appropriate.
4. Select devices/ports for the configurator and quick card. Record cables, patching and photos on the correct target.
5. Use Project → Documents for PDF/XLSX/Markdown, and .ofam exchange to transfer a copy to Windows.

Data is local. Explicit map download uses the network; saved plans/data remain available offline. Camera/scanning/physical gestures still require the [hardware checklist](../testing/hardware-checklist.md).

## Errors and recovery

Structural errors block saving/import. Documentary warnings permit proceeding after review. A failed save leaves the draft available; closing a dialog alone is not confirmation of a committed save.

Before replacement/merge, compare copies and choose conflicts. Three-way merge cannot independently decide concurrent edits. See [workflows](../07-workflows.md), [data contract](../02-domain-data-contract.md) and [exchange](exchange-and-documents.md).
