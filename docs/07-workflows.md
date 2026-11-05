# Application workflows

[Retained Italian source](07-workflows.it.md)

## Create, navigate and edit

The wizard creates a project, site and floor, with an optional local password. Main navigation offers Map, Inventory (Devices/Racks), Cabling and Project. Network, Power, Templates, Attachments, Documents, Credentials and Trash remain available in their relevant groups. Empty optional Credentials, Configurations and Badges appear in “Altri moduli”; this does not delete project data.

Edits start from drafts. Leaving or changing context with unsaved changes requires confirmation. A failed save retains the draft and visible error for retry. Android uses full-screen editors; Windows places lists and details side by side when space permits. Long operations expose busy state before accepting further edits.

## Import, merge and restore

- Validate and compare a received package with the same-ID local project, even when it is closed. Structural errors block; documentary warnings appear before confirmation.
- Replacement requires confirmation. Cancel preserves the previous copy. Android replacement follows received-package protection; merge keeps local protection.
- Three-way merge uses the last-exchange base and asks for each conflict choice. An absent/unreadable base is reported and is not rewritten by inspection alone.
- Trash restoration needs original site/floor/containers/ports/IDs to remain available. Rejection preserves the item for correction/retry.
- Remove explicit network references before deleting referenced sites, devices or VLANs; no automatic retargeting occurs.

## Media, maps and documents

Photos/attachments have explicit project/floor/rack/device/cable/port targets. Files over 32 MiB are rejected before commit. Android photo series continues only after successful saving. Trash media remains local and is excluded from exchange.

Map quick cards expose devices, ports and links. Port layout changes remain drafts until Save. Map downloads are explicit. Document exports apply scope/classification filters.

See [maps](08-floor-map.md), [configurator](10-object-configurator.md), [documents](03-export-and-documents.md) and platform guides [Android](guide/android.md)/[Windows](guide/windows.md). Native checks still open remain in the tracker and matrices.
