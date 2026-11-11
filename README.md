# waspmote_project_RAO_KHOUDMI_ZANGARI

Progetto di Wireless Networks: un nodo **Waspmote** misura temperatura, umidità
e pressione e invia i dati via **ZigBee (XBee PRO)** a un gateway collegato al
PC. Un'applicazione **Java** locale riceve i frame, li decodifica e mostra i valori.

## Struttura

```
nodo/            codice del nodo Waspmote (in arrivo)
app-ricezione/   applicazione Java sul PC (Maven)
docs/            architettura e formato dei frame
esempi-frame/    frame di prova in esadecimale
```

- [Architettura](docs/architettura.md)
- [Formato dei frame](docs/formato-frame.md)

## App di ricezione

Requisiti: JDK 21 e Maven.

```bash
cd app-ricezione
mvn package
```

Produce `app-ricezione/target/gateway.jar`, con tutte le dipendenze incluse.

```bash
java -jar target/gateway.jar --demo          # dati simulati, senza hardware
java -jar target/gateway.jar --list-ports    # elenca le porte seriali
java -jar target/gateway.jar --port COM3     # Windows (su Linux es. /dev/ttyUSB0)
```

Opzioni: `--baud 115200` (default), `--api1` se l'XBee del gateway è in API
mode 1, `--console` per stampare a terminale invece di aprire la finestra.

![App in modalità demo](docs/img/app-demo.png)

Con il gateway reale:

- l'XBee del gateway deve essere **coordinatore** in **API mode** (AP=2, oppure AP=1 con `--api1`);
- chiudere XCTU o altri monitor seriali prima di avviare l'app, altrimenti la porta risulta occupata.
