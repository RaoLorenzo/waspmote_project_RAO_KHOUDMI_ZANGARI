package waspmote.gateway.frame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import waspmote.gateway.core.FramePipeline;
import waspmote.gateway.core.Reading;
import waspmote.gateway.core.ReadingListener;
import waspmote.gateway.xbee.XBeeApiEncoder;

class WaspmoteFrameParserTest {

    /** Frame come lo costruisce la libreria Libelium: &lt;=&gt;, 0x86, n. campi, #... */
    private static byte[] sampleFrame() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes("<=>".getBytes(StandardCharsets.US_ASCII));
        out.write(0x86);
        out.write(3);
        out.writeBytes("#3F4A1C2D11223344#NODO_01#12#TC:24.31#HUM:51.2#PRES:101325.00#"
                .getBytes(StandardCharsets.US_ASCII));
        return out.toByteArray();
    }

    @Test
    void decodificaFrameAsciiConTemperaturaUmiditaPressione() {
        WaspmoteFrame f = WaspmoteFrameParser.parse(sampleFrame());

        assertEquals(0x86, f.frameType());
        assertEquals(3, f.fieldCount());
        assertEquals("3F4A1C2D11223344", f.serialId());
        assertEquals("NODO_01", f.waspmoteId());
        assertEquals(12, f.sequence());
        assertEquals(List.of("TC", "HUM", "PRES"), List.copyOf(f.fields().keySet()));
        assertEquals(24.31, f.temperature().getAsDouble(), 1e-9);
        assertEquals(51.2, f.humidity().getAsDouble(), 1e-9);
        assertEquals(101325.0, f.pressure().getAsDouble(), 1e-9);
    }

    @Test
    void buildEParseSonoSimmetrici() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("TC", "19.80");
        fields.put("BAT", "87");
        byte[] built = WaspmoteFrameParser.build("ABC", "N2", 255, fields);
        WaspmoteFrame f = WaspmoteFrameParser.parse(built);

        assertEquals("N2", f.waspmoteId());
        assertEquals(255, f.sequence());
        assertEquals(fields, f.fields());
    }

    @Test
    void rifiutaFrameBinariEDatiNonValidi() {
        byte[] binary = sampleFrame();
        binary[3] = 0x06;
        assertThrows(WaspmoteFrameParser.FrameFormatException.class, () -> WaspmoteFrameParser.parse(binary));
        assertThrows(WaspmoteFrameParser.FrameFormatException.class,
                () -> WaspmoteFrameParser.parse("ciao mondo".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    void pipelineCompletaDaiByteSerialiAllaLettura() {
        List<Reading> readings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        FramePipeline pipeline = new FramePipeline(true, new ReadingListener() {
            @Override
            public void onReading(Reading reading) {
                readings.add(reading);
            }

            @Override
            public void onError(String message) {
                errors.add(message);
            }
        });

        byte[] wire = XBeeApiEncoder.encode(
                XBeeApiEncoder.zigbeeReceivePacket(0x0013A20041A2B3C4L, 0x1234, sampleFrame()), true);
        pipeline.feed(wire, wire.length);

        assertEquals(List.of(), errors);
        assertEquals(1, readings.size());
        assertEquals("0013A20041A2B3C4", readings.get(0).sourceMac());
        assertEquals(24.31, readings.get(0).frame().temperature().getAsDouble(), 1e-9);
    }
}
