# Mappa e planimetrie

La mappa è una vista condivisa; `MapUiState` dell'host conserva camera, selezione, livello e contenitori mentre cambia vista o dimensione. La posizione degli oggetti e le tratte salvate sono invece dati del progetto.

## Livelli e viste

| Livello | Vista iniziale | Altre viste nella barra |
| --- | --- | --- |
| Progetto | Schema sedi interconnesse | Topologia, Logica, Apparati, Elenco |
| Sede | Topologia fisica | Logica, Apparati, Elenco |
| Piano | Planimetria | Topologia, Logica, Apparati, Elenco su schermi stretti |

`MapView.forLevel` esclude lo schema a livello sede, che non ha un disegno proprio. La barra è sempre visibile; sotto 600 dp usa icone e nomina la vista attiva. Con una sola sede la navigazione può aprire direttamente quella sede.

La planimetria contiene apparati, rack, passaggi, annotazioni e tratte. La topologia fisica riduce i passanti intermedi; la vista logica raggruppa subnet/VLAN ed evidenzia gateway e WAN/VPN; Apparati mostra frontali e prospetti rack. Questi schemi rappresentano relazioni del progetto, **non** posizioni geografiche delle sedi.

## Interazione e file

Tocco, trascinamento e zoom agiscono sulla scena; la scheda rapida della porta permette collegamento, passaggio, foto e scollegamento. Il progetto può allegare una planimetria immagine/PDF. Uno sfondo PDF richiesto ma illeggibile produce errore nell'export Windows.

Il download cartografico OpenTopoMap è esplicito; le tile hanno limite di 2 MiB e dimensione attesa 256 × 256 prima del decode. L'attribuzione resta nell'immagine acquisita. Il funzionamento offline della mappa usa dati e immagini già salvati.

Le prove simulate di larghezza non sostituiscono gesti fisici, rotazione o TalkBack: vedi [matrice Android](testing/hardware-checklist.md) e [Windows](testing/windows-native-matrix.md).

## Fonti

[Gesti Compose](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/multi-touch), [PdfRenderer Android](https://developer.android.com/reference/android/graphics/pdf/PdfRenderer), [ImageReader JDK](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/imageio/ImageReader.html).
