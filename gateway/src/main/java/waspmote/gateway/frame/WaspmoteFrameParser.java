package waspmote.gateway.frame;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Decodifica i frame Waspmote in formato ASCII (frame.createFrame(ASCII)).
 *
 * <p>Il formato binario (frame.createFrame(BINARY)) non è ancora supportato.
 */
public final class WaspmoteFrameParser {

    private static final byte[] DELIMITER = {'<', '=', '>'};
    private static final int ASCII_FLAG = 0x80;

    private WaspmoteFrameParser() {
    }

    public static WaspmoteFrame parse(byte[] data) {
        if (data.length < 6 || data[0] != DELIMITER[0] || data[1] != DELIMITER[1] || data[2] != DELIMITER[2]) {
            throw new FrameFormatException("Delimitatore <=> mancante");
        }
        int frameType = data[3] & 0xFF;
        if ((frameType & ASCII_FLAG) == 0) {
            throw new FrameFormatException(
                    String.format("Frame binario (tipo 0x%02X) non ancora supportato", frameType));
        }
        int fieldCount = data[4] & 0xFF;
        if (data[5] != '#') {
            throw new FrameFormatException("Separatore # atteso dopo l'intestazione");
        }

        String body = new String(data, 6, data.length - 6, StandardCharsets.US_ASCII);
        String[] tokens = body.split("#", -1);
        if (tokens.length < 3) {
            throw new FrameFormatException("Intestazione incompleta: " + body);
        }

        String serialId = tokens[0];
        String waspmoteId = tokens[1];
        int sequence;
        try {
            sequence = Integer.parseInt(tokens[2]);
        } catch (NumberFormatException e) {
            throw new FrameFormatException("Numero di sequenza non valido: " + tokens[2]);
        }

        Map<String, String> fields = new LinkedHashMap<>();
        for (int i = 3; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.isEmpty()) {
                continue;
            }
            int colon = token.indexOf(':');
            if (colon <= 0) {
                throw new FrameFormatException("Campo senza ':' -> " + token);
            }
            fields.put(token.substring(0, colon), token.substring(colon + 1));
        }

        return new WaspmoteFrame(frameType, fieldCount, serialId, waspmoteId, sequence, fields);
    }

    /** Costruisce un frame ASCII; usato dalla modalità demo e dai test. */
    public static byte[] build(String serialId, String waspmoteId, int sequence, Map<String, String> fields) {
        StringBuilder sb = new StringBuilder();
        sb.append(serialId).append('#').append(waspmoteId).append('#').append(sequence).append('#');
        fields.forEach((k, v) -> sb.append(k).append(':').append(v).append('#'));
        byte[] body = sb.toString().getBytes(StandardCharsets.US_ASCII);

        byte[] out = new byte[6 + body.length];
        out[0] = '<';
        out[1] = '=';
        out[2] = '>';
        out[3] = (byte) (0x06 | ASCII_FLAG); // information frame v15, ASCII
        out[4] = (byte) fields.size();
        out[5] = '#';
        System.arraycopy(body, 0, out, 6, body.length);
        return out;
    }

    public static class FrameFormatException extends RuntimeException {
        public FrameFormatException(String message) {
            super(message);
        }
    }
}
