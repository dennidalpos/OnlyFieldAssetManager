# Domain and .ofam data contract

[Retained Italian source](02-domain-data-contract.it.md)

## Model and validation

Projects contain sites, floors (Area), devices/ports, racks, cables, pass-throughs, logical networks, power connections, media, templates, extra fields and badges. The hierarchy is project → site → floor. A device's floor is optional; its absence is a documentary warning. Operational and survey states are separate.

ConnectionGraph follows cables and port pass-throughs. Logical topology uses VLANs, subnets, interfaces and WAN/VPN links. Referenced network entities cannot be deleted or merged into dangling references: remove those references explicitly. Power validation considers every edge and rejects cycles.

ModelValidator emits STRUCTURAL_ERROR (blocks import/save) or DOCUMENTARY_WARNING (allows saving after review). Non-finite numbers, foreign IDs and inconsistent ZIP data are rejected. Missing attachments and incomplete surveys can remain warnings if the catalog is valid.

## Package format and limits

`.ofam` v1 is a ZIP containing manifest, project JSON and attachments. Password protection uses PBKDF2-HMAC-SHA256 and AES-256-GCM for the project and attachments. PackageSerializer ignores unknown JSON fields but rejects unsupported versions, duplicate entries and inconsistent metadata. Each non-manifest entry needs its matching SHA-256; a missing checksum is invalid.

| Limit | Value |
| --- | ---: |
| ZIP size | 256 MiB |
| Unencrypted individual file | 32 MiB |
| Expanded total | 512 MiB |
| ZIP entries, including directories | 10,000 |
| PBKDF2 export iterations | 600,000 |
| Maximum accepted PBKDF2 iterations | 1,000,000 |

IDs must be canonical lowercase UUIDs. Windows export forces the write and verifies checksums before replacing its destination. ProjectPackage is AutoCloseable; close it after use. Large payloads are processed progressively. Android's conservative protected-export preflight can reject content near the ZIP limit even if the final archive might fit.

## Exchange and compatibility

The last-exchange base enables three-way comparison. Concurrent edits need explicit conflict choices. Missing/unreadable bases are reported. Import/merge does not remap IDs belonging to another project. The local trash and locally recoverable media are excluded from exchange packages.

Device.management records technician/provider/customer/unknown ownership; provider contact and contract are optional. It is stored in v1 package JSON and Room v3 columns. HardwareSpec.portLayouts and portPoeOverrides remain in hardware JSON. Older applications can ignore and lose unknown fields when saving: do not assume round-trip preservation across versions.

Android currently uses Room v3 with a destructive fallback when a migration is missing. Export projects before installing a build with another schema. This is a current limitation, not a recommendation to erase data.

Source contracts: PackageImportLimits, PackageSerializer, Models, ModelValidator and AppDatabase. See [storage](04-desktop-storage-interop.md) and [exchange guide](guide/exchange-and-documents.md).
