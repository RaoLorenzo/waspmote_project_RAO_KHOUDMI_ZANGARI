package waspmote.gateway.source;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import waspmote.gateway.FramePipeline;
import waspmote.gateway.frame.WaspmoteFrameParser;
import waspmote.gateway.xbee.XBeeApiEncoder;

/**
 * Simula il gateway senza hardware: genera frame Waspmote realistici, li
 * incapsula in frame XBee API (come farebbe il modulo reale) e li passa alla
 * pipeline byte per byte, a pezzi.
 */
public class DemoDataSource implements DataSource {

    private static final long DEMO_MAC = 0x0013A20041A2B3C4L;

    private final FramePipeline pipeline;
    private final boolean escapedApi;
    private final long periodMillis;
    private final Random random = new Random();
    private ScheduledExecutorService executor;
    private int sequence = 0;
    private double temperature = 22.0;
    private double humidity = 50.0;
    private double pressure = 101_325.0;

    public DemoDataSource(FramePipeline pipeline, boolean escapedApi, long periodMillis) {
        this.pipeline = pipeline;
        this.escapedApi = escapedApi;
        this.periodMillis = periodMillis;
    }

    @Override
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "demo-source");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(this::emit, 0, periodMillis, TimeUnit.MILLISECONDS);
    }

    private void emit() {
        temperature += random.nextGaussian() * 0.2;
        humidity = Math.clamp(humidity + random.nextGaussian() * 0.8, 0, 100);
        pressure += random.nextGaussian() * 15;

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("TC", String.format(Locale.ROOT, "%.2f", temperature));
        fields.put("HUM", String.format(Locale.ROOT, "%.1f", humidity));
        fields.put("PRES", String.format(Locale.ROOT, "%.2f", pressure));

        byte[] waspFrame = WaspmoteFrameParser.build("3F4A1C2D11223344", "NODO_DEMO", sequence, fields);
        sequence = (sequence + 1) % 256;
        byte[] bytes = XBeeApiEncoder.encode(
                XBeeApiEncoder.zigbeeReceivePacket(DEMO_MAC, 0x1A2B, waspFrame), escapedApi);

        // Consegna in due pezzi, come può succedere con la seriale reale
        int half = bytes.length / 2;
        pipeline.feed(bytes, half);
        byte[] rest = new byte[bytes.length - half];
        System.arraycopy(bytes, half, rest, 0, rest.length);
        pipeline.feed(rest, rest.length);
    }

    @Override
    public String description() {
        return "Demo (dati simulati)";
    }

    @Override
    public void close() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
