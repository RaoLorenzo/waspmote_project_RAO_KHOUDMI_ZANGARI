package waspmote.gateway.core;

import java.time.LocalDateTime;
import waspmote.gateway.frame.WaspmoteFrame;

/**
 * Una lettura ricevuta: frame Waspmote decodificato più i dati di contorno
 * della rete ZigBee.
 *
 * @param receivedAt istante di ricezione sul PC
 * @param sourceMac  indirizzo MAC a 64 bit dell'XBee mittente
 * @param frame      contenuto decodificato
 */
public record Reading(LocalDateTime receivedAt, String sourceMac, WaspmoteFrame frame) {
}
