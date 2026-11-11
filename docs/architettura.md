# Architettura

```
 Nodo Waspmote + BME280        XBee gateway (USB)             PC
┌──────────────────────┐ ZigBee ┌──────────────┐ seriale ┌──────────────────────┐
│ legge T, UR, P       │ ─────► │ coordinatore │ ──────► │ app-ricezione (Java) │
│ crea e invia frame   │        │ in API mode  │         │ decodifica e mostra  │
└──────────────────────┘        └──────────────┘         └──────────────────────┘
        nodo/                                                 app-ricezione/
```

## Componenti

| Cartella          | Contenuto                                   | Responsabile |
|-------------------|---------------------------------------------|--------------|
| `nodo/`           | codice del nodo sensore (lettura + invio)   | da definire  |
| `app-ricezione/`  | applicazione Java sul PC                    | da definire  |
| `docs/`           | documentazione del progetto                 | tutti        |
| `esempi-frame/`   | frame di prova in esadecimale               | tutti        |

## App di ricezione

I byte arrivano dalla porta seriale e attraversano una catena di decodifica,
un package Java per ogni livello:

```
seriale/  →  xbee/  →  frame/  →  core/  →  ui/
 byte       frame API   frame      lettura    finestra
            0x7E..0x90  Waspmote   completa   o console
```

| Package   | Classi principali | Ruolo |
|-----------|-------------------|-------|
| `seriale` | `SerialDataSource`, `DemoDataSource` | legge i byte dalla porta USB, oppure li simula |
| `xbee`    | `XBeeApiDecoder`, `ZigBeeReceivePacket` | ricostruisce i frame API (escape, checksum) ed estrae il pacchetto 0x90 |
| `frame`   | `WaspmoteFrameParser`, `WaspmoteFrame` | decodifica il frame Waspmote ASCII e i campi sensore |
| `core`    | `FramePipeline`, `Reading`, `ReadingListener` | collega i livelli e notifica le letture |
| `ui`      | `MainWindow` | mostra ultimi valori e storico |

L'interfaccia riceve solo oggetti `Reading`: si può sostituire (o affiancare
con una pagina web, un file CSV, un database) senza toccare la decodifica.
