# Maps and floor plans

[Retained Italian source](08-floor-map.it.md)

MapUiState stores camera, selection, level and containers while view/window size changes. Saved object positions/routes are project data.

| Level | Initial view | Other views |
| --- | --- | --- |
| Project | Connected-sites diagram | Topology, Logical, Devices, List |
| Site | Physical topology | Logical, Devices, List |
| Floor | Floor plan | Topology, Logical, Devices, List on narrow screens |

MapView.forLevel excludes the project-wide sites diagram at site level. The view bar remains visible; below 600 dp it uses icons and names the active view. A single-site project can open that site directly.

Floor plans contain devices, racks, pass-throughs, annotations and routes. Physical topology simplifies intermediate pass-throughs. Logical view groups subnet/VLAN data and highlights gateways/WAN/VPN. Devices shows front panels/rack elevations. These are relationship diagrams, not geographic site positions.

## Interaction and files

Touch, dragging and zoom operate on the scene. Port quick cards provide connect, pass-through, photo and disconnect actions. Projects can attach image/PDF floor plans. A requested unreadable PDF background causes a Windows export error.

OpenTopoMap tile downloads are explicit. Tiles are limited to 2 MiB and expected 256×256 dimensions before decoding; attribution stays in the acquired image. Offline use relies on saved project data/images.

Simulated width tests do not replace physical gestures, rotation or TalkBack. See [Android](testing/hardware-checklist.md) and [Windows](testing/windows-native-matrix.md).
