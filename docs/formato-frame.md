# Formato dei frame

I dati del sensore arrivano al PC incapsulati su tre livelli. Gli esempi
completi in esadecimale sono in [`esempi-frame/`](../esempi-frame).

## 1. Frame XBee API

Il gateway XBee in API mode passa al PC frame strutturati:

```
| 0x7E | lunghezza (2 byte) | tipo (1) | dati ... | checksum (1) |
```

- **lunghezza**: numero di byte tra lunghezza e checksum (tipo + dati);
- **checksum**: `0xFF - (somma dei byte di tipo e dati, ultimi 8 bit)`.

**API mode 2 (AP=2)**, usato di default dalle librerie Libelium: i byte
`0x7E 0x7D 0x11 0x13` all'interno del frame sono trasmessi come `0x7D`
seguito dal byte in XOR con `0x20` (es. `0x13` → `7D 33`).

## 2. ZigBee Receive Packet (tipo 0x90)

È il frame che il gateway genera quando riceve dati da un nodo:

```
| MAC mittente (8 byte) | indirizzo rete (2) | opzioni (1) | dati RF ... |
```

Le opzioni valgono `0x01` (pacchetto con ACK) o `0x02` (broadcast).
I dati RF sono ciò che il nodo ha inviato, cioè il frame Waspmote.

## 3. Frame Waspmote ASCII

Formato delle librerie Libelium (`frame.createFrame(ASCII)`):

```
<=> | tipo | n. campi | # | serial ID | # | ID nodo | # | sequenza | # | TAG:valore | # | ...
```

| Parte      | Esempio            | Note |
|------------|--------------------|------|
| `<=>`      | `3C 3D 3E`         | delimitatore |
| tipo       | `0x86`             | bit 7 = ASCII; `0x06` = information frame v15 |
| n. campi   | `0x03`             | numero di campi sensore |
| serial ID  | `3F4A1C2D11223344` | numero di serie della scheda |
| ID nodo    | `NODO_01`          | impostato con `frame.setID()` |
| sequenza   | `12`               | 0–255, utile per stimare i pacchetti persi |
| campi      | `TC:24.31`         | temperatura °C |
|            | `HUM:51.2`         | umidità relativa % |
|            | `PRES:101325.00`   | pressione in Pa |

Il formato binario (`frame.createFrame(BINARY)`) non è ancora supportato
dall'app: andrà aggiunto se il nodo lo userà.

> I nomi dei campi e il formato definitivo vanno confermati con il codice del
> nodo, quando sarà disponibile.
