# Nodo Waspmote

Questa cartella conterrà il codice del nodo sensore: la lettura del BME280
(temperatura, umidità, pressione) e l'invio del frame via XBee ZigBee al gateway.

**Stato:** in attesa dei file del collega che se ne occupa.

Quando arrivano, servirà verificare con l'app di ricezione:

- formato del frame (ASCII o binario) e nomi dei campi (es. `TC`, `HUM`, `PRES`);
- API mode dell'XBee (AP=1 o AP=2) e configurazione di rete (PAN ID, coordinatore);
- indirizzo di destinazione usato (MAC del gateway o broadcast).

Vedi [`docs/formato-frame.md`](../docs/formato-frame.md) per il formato atteso oggi dall'app.
