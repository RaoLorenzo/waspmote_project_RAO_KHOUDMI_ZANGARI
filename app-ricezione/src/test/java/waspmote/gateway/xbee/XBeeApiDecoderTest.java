package waspmote.gateway.xbee;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class XBeeApiDecoderTest {

    private final List<XBeeApiFrame> frames = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    private static byte[] bytes(int... values) {
        byte[] out = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            out[i] = (byte) values[i];
        }
        return out;
    }

    /** Esempio di ZigBee Receive Packet dalla documentazione Digi (AP=1, dati "RxData"). */
    private static final byte[] DIGI_EXAMPLE = bytes(
            0x7E, 0x00, 0x12, 0x90,
            0x00, 0x13, 0xA2, 0x00, 0x40, 0x52, 0x2B, 0xAA,
            0x7D, 0x84, 0x01,
            0x52, 0x78, 0x44, 0x61, 0x74, 0x61,
            0x0D);

    @Test
    void decodificaEsempioDigiInApiMode1() {
        XBeeApiDecoder decoder = new XBeeApiDecoder(false, frames::add, errors::add);
        decoder.feed(DIGI_EXAMPLE, DIGI_EXAMPLE.length);

        assertEquals(List.of(), errors);
        assertEquals(1, frames.size());
        ZigBeeReceivePacket packet = ZigBeeReceivePacket.parse(frames.get(0));
        assertEquals("0013A20040522BAA", packet.source64Hex());
        assertEquals(0x7D84, packet.source16());
        assertEquals(0x01, packet.options());
        assertEquals("RxData", new String(packet.rfData(), StandardCharsets.US_ASCII));
    }

    @Test
    void decodificaInApiMode2ConCaratteriDiEscape() {
        // indirizzo 16 bit 0x7D84 e un dato 0x7E/0x11/0x13: devono essere tutti "escapati"
        byte[] rf = bytes(0x7E, 0x11, 0x13, 0x41);
        XBeeApiFrame original = XBeeApiEncoder.zigbeeReceivePacket(0x0013A20040522BAAL, 0x7D84, rf);
        byte[] wire = XBeeApiEncoder.encode(original, true);

        // dopo il primo byte non deve comparire nessun 0x7E "nudo"
        for (int i = 1; i < wire.length; i++) {
            assertTrue((wire[i] & 0xFF) != 0x7E, "0x7E non escapato in posizione " + i);
        }

        XBeeApiDecoder decoder = new XBeeApiDecoder(true, frames::add, errors::add);
        decoder.feed(wire, wire.length);

        assertEquals(List.of(), errors);
        assertEquals(List.of(original), frames);
        assertArrayEquals(rf, ZigBeeReceivePacket.parse(frames.get(0)).rfData());
    }

    @Test
    void ignoraByteSpuriERicostruisceFrameArrivatiAPezzi() {
        XBeeApiDecoder decoder = new XBeeApiDecoder(false, frames::add, errors::add);
        byte[] noise = bytes(0x00, 0x42, 0xFF);
        decoder.feed(noise, noise.length);
        for (byte b : DIGI_EXAMPLE) {
            decoder.feed(b); // un byte alla volta
        }
        decoder.feed(DIGI_EXAMPLE, DIGI_EXAMPLE.length);

        assertEquals(2, frames.size());
        assertEquals(List.of(), errors);
    }

    @Test
    void scartaFrameConChecksumErrato() {
        byte[] corrupted = DIGI_EXAMPLE.clone();
        corrupted[corrupted.length - 1] = 0x00;
        XBeeApiDecoder decoder = new XBeeApiDecoder(false, frames::add, errors::add);
        decoder.feed(corrupted, corrupted.length);

        assertEquals(List.of(), frames);
        assertEquals(1, errors.size());
    }

    @Test
    void inApiMode2UnNuovoDelimitatoreFaRipartireIlFrame() {
        byte[] truncated = new byte[8];
        System.arraycopy(DIGI_EXAMPLE, 0, truncated, 0, truncated.length);
        XBeeApiDecoder decoder = new XBeeApiDecoder(true, frames::add, errors::add);
        decoder.feed(truncated, truncated.length);
        byte[] good = XBeeApiEncoder.encode(
                XBeeApiEncoder.zigbeeReceivePacket(1L, 2, bytes(0x41)), true);
        decoder.feed(good, good.length);

        assertEquals(1, frames.size());
        assertEquals(1, errors.size());
    }
}
