# Frame di esempio

Byte in esadecimale, uno spazio tra un byte e l'altro. Le righe che iniziano
con `#` sono commenti. Servono per i test dell'app e per confrontare i frame
reali catturati con XCTU.

| File | Contenuto |
|------|-----------|
| `waspmote-ascii-bme280.hex` | solo frame Waspmote ASCII (TC, HUM, PRES), senza XBee |
| `xbee-0x90-ap1.hex` | lo stesso frame dentro un pacchetto ZigBee 0x90, API mode 1 |
| `xbee-0x90-ap2.hex` | come sopra, API mode 2 (con caratteri di escape) |
| `digi-0x90-rxdata-ap1.hex` | esempio ufficiale Digi di pacchetto 0x90 (dati "RxData") |

I file `xbee-0x90-*.hex` sono verificati automaticamente da
`app-ricezione/src/test/java/waspmote/gateway/core/EsempiFrameTest.java`.
