package waspmote.gateway.xbee;

import java.io.ByteArrayOutputStream;

/**
 * Operazione inversa di {@link XBeeApiDecoder}: costruisce i byte di un frame
 * API. Serve alla modalità demo e ai test, per simulare ciò che arriva dalla
 * porta seriale del gateway.
 */
public final class XBeeApiEncoder {

    private XBeeApiEncoder() {
    }

    public static byte[] encode(XBeeApiFrame frame, boolean escaped) {
        byte[] data = frame.data();
        int length = data.length + 1;

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(length >> 8);
        body.write(length & 0xFF);
        body.write(frame.type());
        body.write(data, 0, data.length);

        int sum = frame.type();
        for (byte b : data) {
            sum += b & 0xFF;
        }
        body.write(0xFF - (sum & 0xFF));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(XBeeApiDecoder.START_DELIMITER);
        for (byte b : body.toByteArray()) {
            int v = b & 0xFF;
            if (escaped && needsEscape(v)) {
                out.write(XBeeApiDecoder.ESCAPE);
                out.write(v ^ 0x20);
            } else {
                out.write(v);
            }
        }
        return out.toByteArray();
    }

    /**
     * Costruisce un ZigBee Receive Packet (0x90) come quello che il gateway
     * passa al PC quando riceve dati da un nodo.
     */
    public static XBeeApiFrame zigbeeReceivePacket(long source64, int source16, byte[] rfData) {
        byte[] data = new byte[8 + 2 + 1 + rfData.length];
        for (int i = 0; i < 8; i++) {
            data[i] = (byte) (source64 >>> (56 - 8 * i));
        }
        data[8] = (byte) (source16 >> 8);
        data[9] = (byte) source16;
        data[10] = 0x01; // opzioni: pacchetto con ACK
        System.arraycopy(rfData, 0, data, 11, rfData.length);
        return new XBeeApiFrame(XBeeApiFrame.ZIGBEE_RECEIVE_PACKET, data);
    }

    private static boolean needsEscape(int v) {
        return v == XBeeApiDecoder.START_DELIMITER || v == XBeeApiDecoder.ESCAPE
                || v == XBeeApiDecoder.XON || v == XBeeApiDecoder.XOFF;
    }
}
