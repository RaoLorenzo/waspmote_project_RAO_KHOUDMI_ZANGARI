package waspmote.gateway.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Verifica che i frame in esempi-frame/ siano decodificati correttamente. */
class EsempiFrameTest {

    private static final Path ESEMPI = Path.of("..", "esempi-frame");

    /** Legge un file .hex: righe con '#' sono commenti, il resto byte esadecimali. */
    static byte[] readHex(Path file) throws IOException {
        StringBuilder hex = new StringBuilder();
        for (String line : Files.readAllLines(file)) {
            if (!line.isBlank() && !line.startsWith("#")) {
                hex.append(line.replaceAll("\\s", ""));
            }
        }
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }

    @ParameterizedTest
    @CsvSource({"xbee-0x90-ap1.hex, false", "xbee-0x90-ap2.hex, true"})
    void decodificaFrameDiEsempio(String file, boolean escaped) throws IOException {
        List<Reading> readings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        FramePipeline pipeline = new FramePipeline(escaped, new ReadingListener() {
            @Override
            public void onReading(Reading reading) {
                readings.add(reading);
            }

            @Override
            public void onError(String message) {
                errors.add(message);
            }
        });

        byte[] bytes = readHex(ESEMPI.resolve(file));
        pipeline.feed(bytes, bytes.length);

        assertEquals(List.of(), errors);
        assertEquals(1, readings.size());
        Reading r = readings.get(0);
        assertEquals("0013A20041A2B3C4", r.sourceMac());
        assertEquals("NODO_01", r.frame().waspmoteId());
        assertEquals(12, r.frame().sequence());
        assertEquals(24.31, r.frame().temperature().getAsDouble(), 1e-9);
        assertEquals(51.2, r.frame().humidity().getAsDouble(), 1e-9);
        assertEquals(101325.0, r.frame().pressure().getAsDouble(), 1e-9);
    }
}
