/*
 * Nodo sensore Waspmote: temperatura, umidità e pressione (BME280)
 * inviate via XBee ZigBee al gateway collegato al PC.
 *
 * IPOTESI DA VERIFICARE: il BME280 è montato sulla Events Sensor Board v30.
 * Se usate un'altra scheda (es. Gases v30, Agriculture v30, Smart Cities PRO)
 * cambiate l'include e le chiamate a "Events" con quelle della vostra scheda.
 *
 * Formato inviato: frame Waspmote ASCII, es.
 *   <=>\x86\x03#<serial>#NODO_01#12#TC:24.31#HUM:51.2#PRES:101325.00#
 */

#include <WaspSensorEvent_v30.h>
#include <WaspXBeeZB.h>
#include <WaspFrame.h>

// Indirizzo MAC a 64 bit dell'XBee del gateway (si legge sotto il modulo
// o con XCTU). Sostituire con quello reale.
char GATEWAY_MAC[] = "0013A20040XXXXXX";

// Identificativo del nodo, compare nel frame ricevuto dal PC
char NODE_ID[] = "NODO_01";

// Intervallo di invio (formato giorni:ore:minuti:secondi)
#define SLEEP_TIME "00:00:00:30"

void setup()
{
  USB.ON();
  USB.println(F("Avvio nodo BME280 + XBee ZigBee"));

  // Identificativo che verrà inserito in ogni frame
  frame.setID(NODE_ID);

  // Accensione del modulo XBee sulla SOCKET0
  xbeeZB.ON();

  // Attesa dell'associazione alla rete ZigBee del coordinatore
  xbeeZB.getAssociationIndication();
  while (xbeeZB.associationIndication != 0)
  {
    USB.println(F("In attesa di associazione alla rete ZigBee..."));
    delay(2000);
    xbeeZB.getAssociationIndication();
  }
  USB.println(F("Associato alla rete ZigBee"));
}

void loop()
{
  // 1. Lettura sensori
  Events.ON();
  float temperatura = Events.getTemperature();  // °C
  float umidita     = Events.getHumidity();     // %RH
  float pressione   = Events.getPressure();     // Pa
  Events.OFF();

  USB.print(F("T=")); USB.print(temperatura);
  USB.print(F(" C  H=")); USB.print(umidita);
  USB.print(F(" %  P=")); USB.print(pressione); USB.println(F(" Pa"));

  // 2. Costruzione del frame ASCII
  frame.createFrame(ASCII);
  frame.addSensor(SENSOR_EVENTS_TC, temperatura);
  frame.addSensor(SENSOR_EVENTS_HUM, umidita);
  frame.addSensor(SENSOR_EVENTS_PRES, pressione);
  frame.showFrame();

  // 3. Invio al gateway
  uint8_t errore = xbeeZB.send(GATEWAY_MAC, frame.buffer, frame.length);
  if (errore == 0)
  {
    USB.println(F("Invio OK"));
  }
  else
  {
    USB.print(F("Errore di invio: "));
    USB.println(errore, DEC);
  }

  // 4. Sospensione fino al prossimo campionamento
  PWR.deepSleep(SLEEP_TIME, RTC_OFFSET, RTC_ALM1_MODE1, ALL_OFF);
  xbeeZB.ON();
}
