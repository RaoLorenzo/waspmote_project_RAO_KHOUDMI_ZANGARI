package waspmote.gateway.xbee;

/**
 * Contenuto di un frame API 0x90 (ZigBee Receive Packet).
 *
 * <pre>
 * | indirizzo 64 bit (8) | indirizzo 16 bit (2) | opzioni (1) | dati RF ... |
 * </pre>
 *
 * @param source64 indirizzo MAC del nodo mittente
 * @param source16 indirizzo di rete a 16 bit del mittente
 * @param options  opzioni di ricezione (0x01 = ACK, 0x02 = broadcast)
 * @param rfData   dati inviati dal nodo (qui: il frame Waspmote)
 */
public record ZigBeeReceivePacket(long source64, int source16, int options, byte[] rfData) {

    private static final int HEADER_LENGTH = 11;

    public static ZigBeeReceivePacket parse(XBeeApiFrame frame) {
        if (frame.type() != XBeeApiFrame.ZIGBEE_RECEIVE_PACKET) {
            throw new IllegalArgumentException(
                    String.format("Atteso frame 0x90, ricevuto 0x%02X", frame.type()));
        }
        byte[] data = frame.data();
        if (data.length < HEADER_LENGTH) {
            throw new IllegalArgumentException("Frame 0x90 troppo corto: " + data.length + " byte");
        }
        long source64 = 0;
        for (int i = 0; i < 8; i++) {
            source64 = (source64 << 8) | (data[i] & 0xFF);
        }
        int source16 = ((data[8] & 0xFF) << 8) | (data[9] & 0xFF);
        int options = data[10] & 0xFF;
        byte[] rf = new byte[data.length - HEADER_LENGTH];
        System.arraycopy(data, HEADER_LENGTH, rf, 0, rf.length);
        return new ZigBeeReceivePacket(source64, source16, options, rf);
    }

    public ZigBeeReceivePacket {
        rfData = rfData.clone();
    }

    @Override
    public byte[] rfData() {
        return rfData.clone();
    }

    public String source64Hex() {
        return String.format("%016X", source64);
    }
}
