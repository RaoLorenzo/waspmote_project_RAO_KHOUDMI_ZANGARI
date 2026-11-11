package waspmote.gateway.xbee;

import java.util.Arrays;

/**
 * Un frame XBee in API mode, già privato di delimitatore, lunghezza e checksum.
 *
 * <pre>
 * | 0x7E | len MSB | len LSB | frame type | frame data ... | checksum |
 * </pre>
 *
 * @param type tipo del frame (es. 0x90 = ZigBee Receive Packet)
 * @param data byte successivi al tipo, checksum escluso
 */
public record XBeeApiFrame(int type, byte[] data) {

    /** ZigBee Receive Packet: dati ricevuti da un altro nodo. */
    public static final int ZIGBEE_RECEIVE_PACKET = 0x90;

    /** ZigBee Transmit Request: usato dal nodo per inviare. */
    public static final int ZIGBEE_TRANSMIT_REQUEST = 0x10;

    public XBeeApiFrame {
        data = data.clone();
    }

    @Override
    public byte[] data() {
        return data.clone();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof XBeeApiFrame f && f.type == type && Arrays.equals(f.data, data);
    }

    @Override
    public int hashCode() {
        return 31 * type + Arrays.hashCode(data);
    }

    @Override
    public String toString() {
        return String.format("XBeeApiFrame[type=0x%02X, %d byte]", type, data.length);
    }
}
