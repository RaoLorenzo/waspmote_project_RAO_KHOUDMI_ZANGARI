package waspmote.gateway.xbee;

import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;

/**
 * Ricostruisce i frame XBee API dal flusso di byte della porta seriale.
 *
 * <p>I byte arrivano a pezzi e in modo arbitrario, quindi il decoder è una
 * macchina a stati alimentata un byte alla volta. Supporta sia API mode 1
 * (AP=1) sia API mode 2 con caratteri di escape (AP=2, quello usato di
 * default dalle librerie Libelium).
 *
 * <p>In AP=2 i byte 0x7E, 0x7D, 0x11 e 0x13 all'interno del frame sono
 * trasmessi come 0x7D seguito dal byte in XOR con 0x20.
 */
public class XBeeApiDecoder {

    public static final int START_DELIMITER = 0x7E;
    public static final int ESCAPE = 0x7D;
    public static final int XON = 0x11;
    public static final int XOFF = 0x13;

    private enum State { WAIT_START, LENGTH_MSB, LENGTH_LSB, PAYLOAD, CHECKSUM }

    private final boolean escaped;
    private final Consumer<XBeeApiFrame> onFrame;
    private final Consumer<String> onError;

    private State state = State.WAIT_START;
    private boolean escapeNext = false;
    private int length;
    private int sum;
    private final ByteArrayOutputStream payload = new ByteArrayOutputStream();

    /**
     * @param escaped true per API mode 2 (AP=2), false per API mode 1
     * @param onFrame chiamato per ogni frame con checksum valido
     * @param onError chiamato per frame scartati (checksum errato, ecc.)
     */
    public XBeeApiDecoder(boolean escaped, Consumer<XBeeApiFrame> onFrame, Consumer<String> onError) {
        this.escaped = escaped;
        this.onFrame = onFrame;
        this.onError = onError;
    }

    public void feed(byte[] buffer, int count) {
        for (int i = 0; i < count; i++) {
            feed(buffer[i]);
        }
    }

    public void feed(byte b) {
        int value = b & 0xFF;

        // Un delimitatore "nudo" in AP=2 indica sempre l'inizio di un nuovo frame
        if (value == START_DELIMITER && (escaped || state == State.WAIT_START)) {
            if (state != State.WAIT_START) {
                onError.accept("Frame incompleto scartato");
            }
            reset();
            state = State.LENGTH_MSB;
            return;
        }
        if (state == State.WAIT_START) {
            return; // byte spuri prima di un frame
        }

        if (escaped) {
            if (escapeNext) {
                value ^= 0x20;
                escapeNext = false;
            } else if (value == ESCAPE) {
                escapeNext = true;
                return;
            }
        }

        switch (state) {
            case LENGTH_MSB -> {
                length = value << 8;
                state = State.LENGTH_LSB;
            }
            case LENGTH_LSB -> {
                length |= value;
                state = length == 0 ? State.WAIT_START : State.PAYLOAD;
            }
            case PAYLOAD -> {
                payload.write(value);
                sum += value;
                if (payload.size() == length) {
                    state = State.CHECKSUM;
                }
            }
            case CHECKSUM -> {
                sum += value;
                if ((sum & 0xFF) == 0xFF) {
                    byte[] bytes = payload.toByteArray();
                    byte[] data = new byte[bytes.length - 1];
                    System.arraycopy(bytes, 1, data, 0, data.length);
                    onFrame.accept(new XBeeApiFrame(bytes[0] & 0xFF, data));
                } else {
                    onError.accept(String.format("Checksum errato (somma=0x%02X)", sum & 0xFF));
                }
                reset();
            }
            default -> reset();
        }
    }

    private void reset() {
        state = State.WAIT_START;
        escapeNext = false;
        length = 0;
        sum = 0;
        payload.reset();
    }
}
