# Demo Comune

`fixtures/demo/onlyfield-demo.ofam` è una fixture sintetica importabile nelle due app. Non contiene dati reali né va sostituita automaticamente a un progetto locale. L'import usa il normale confronto e richiede conferma se esiste già una copia dello stesso ID.

La revisione registrata l'11 ottobre 2026 contiene 366 apparati, 8 rack, 1007 cavi, 14 modelli e 7 allegati sintetici. Copre più sedi, dorsali in fibra, ponti radio, VLAN/subnet, gestione di fornitori, rack e percorsi fisici. Gli avvisi documentali sono intenzionali; il pacchetto non dovrebbe avere errori strutturali.

| Caso | Dove guardare |
| --- | --- |
| Sedi e collegamenti | Schema sedi, Teatro, Municipio e scuole |
| Percorso porta e passanti | `SW-COM-LAB`, `PP-COM-LAB`, prese e ponte radio |
| Rete logica | VLAN 10/20/30/90, subnet e interfacce di laboratorio |
| Layout e PoE | `SW-COM-LAB`, porte P3/P4 |
| File e classificazioni | Sette PNG sintetici su progetto, piano, rack, apparato, cavo e porta |
| Gestione terzi | ONT e apparati del fornitore/cliente |
| Casi incompleti | Porta aperta, passante incompleto, osservazione discordante |

Da PowerShell nella radice, soltanto se occorre aggiornare la fixture:

```powershell
.\gradlew.bat :shared:exchange:demoPackage --no-parallel --max-workers=1
.\gradlew.bat :shared:exchange:test --tests '*Demo*' --no-parallel --max-workers=1
```

La generazione riscrive il file versionato e gli ID delle entità; controllare il diff e non dedurre che la demo installata sul telefono sia stata aggiornata. L'ultimo handoff registra 19 prove demo verdi. Per i limiti del collaudo fisico vedi [Android](hardware-checklist.md) e [Windows](windows-native-matrix.md).
