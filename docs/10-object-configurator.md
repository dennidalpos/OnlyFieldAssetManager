# Device and port configurator

[Retained Italian source](10-object-configurator.it.md)

Shared Compose sources in shared/configurator implement both platform forms. Domain logic remains in core.forms and core.model.ConnectionGraph.

## Add and edit

Insertion follows type → preset/template and drawing → essential data → confirmation. Device name and site are required; advanced details may remain collapsed. Editing starts from the current record and uses copy to preserve unexposed fields. Hardware/templates/presets must not change IDs of already connected ports.

Port diagrams support front/rear faces, groups and visual order independent of technical order. HardwareSpec.portLayouts and portPoeOverrides are stored in hardware JSON. Moving a drawn port does not change cable identities. Saved dimensions/depth remain in the model even when absent from the current form.

## Quick card and layout

A port can show connectivity, physical/logical route, PoE and survey state. Explicit actions connect, insert pass-through, photograph and disconnect. Pass-throughs pair front and rear without inventing an ambiguous route. Layout and PoE changes are drafts with Save; links/photos have their own commits. Photo target can be PORT or CABLE.

Text forms stay within 640 dp; drawings/technical lists use available space. Phone work dialogs become full screen with keyboard-accessible actions. Real hardware/UX checks are separate [Android](testing/hardware-checklist.md)/[Windows](testing/windows-native-matrix.md) work.

## Third-party management

Device.management distinguishes technician, provider, customer and unknown management. Provider name/contact/contract are optional. Returning to technician-managed hides those details in views/exports without erasing the stored record.
