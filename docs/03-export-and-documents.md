# Documents and export

[Retained Italian source](03-export-and-documents.it.md)

shared:exchange generates XLSX, Markdown and QR labels. Android uses PdfExportManager and its printing framework; Windows uses PdfReportWriter/PDFBox and native printing. Credentials are excluded from these documents.

## Selection and content

DocumentSelection applies site, floor, category and classification filters to inventory, network, power, attachments and drawings. Restricted content requires explicit inclusion. Necessary external endpoints can appear as context explaining a selected route, without exporting unrelated records.

| Format | Content and limits |
| --- | --- |
| XLSX | Inventory, ports/cabling, network, power, notes and full routes. User text stays text rather than becoming a formula. |
| Markdown | Text report with Markdown/HTML escaping; source data remains unchanged. |
| Android PDF | Paginated inventory, racks, ports/cabling, network, power/badges, notes and attachment catalog. |
| Windows PDF | Selectable sections including floor plans, front/rear rack views, routes and topology. Requested unreadable backgrounds cause a visible error. |

Missing survey data is labeled “Da verificare”. Notes/warnings follow selection filters. The generation operation captures the selected language; user names and notes are not translated.

## Verification

Export tests cover filters, special characters, pagination and content. They do not verify printer dialogs, the spooler or a physical printer. Review the final PDF when it contains plans or attachments.

See [exchange and documents](guide/exchange-and-documents.md), [Android hardware checks](testing/hardware-checklist.md) and [Windows matrix](testing/windows-native-matrix.md).
