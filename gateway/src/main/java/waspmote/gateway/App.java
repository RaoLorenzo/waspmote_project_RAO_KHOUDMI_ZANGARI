package waspmote.gateway;

import java.awt.GraphicsEnvironment;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;
import javax.swing.SwingUtilities;
import waspmote.gateway.frame.WaspmoteFrame;
import waspmote.gateway.source.DataSource;
import waspmote.gateway.source.DemoDataSource;
import waspmote.gateway.source.SerialDataSource;
import waspmote.gateway.ui.MainWindow;

/**
 * Punto di ingresso.
 *
 * <pre>
 * java -jar gateway.jar --list-ports
 * java -jar gateway.jar --port COM3            (Windows)
 * java -jar gateway.jar --port /dev/ttyUSB0    (Linux)
 * java -jar gateway.jar --demo                 (senza hardware)
 * </pre>
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        if (options == null) {
            printUsage();
            return;
        }
        if (options.listPorts) {
            var ports = SerialDataSource.availablePorts();
            System.out.println(ports.isEmpty() ? "Nessuna porta seriale trovata." : String.join("\n", ports));
            return;
        }

        boolean console = options.console || GraphicsEnvironment.isHeadless();
        ForwardingListener forwarding = new ForwardingListener();
        FramePipeline pipeline = new FramePipeline(!options.apiMode1, forwarding);

        DataSource source = options.demo
                ? new DemoDataSource(pipeline, !options.apiMode1, 2000)
                : new SerialDataSource(options.port, options.baud, pipeline);

        if (console) {
            forwarding.target = new ConsoleListener();
            System.out.println("In ascolto su: " + source.description() + "  (Ctrl+C per uscire)");
        } else {
            MainWindow[] window = new MainWindow[1];
            SwingUtilities.invokeAndWait(() -> {
                window[0] = new MainWindow(source.description());
                window[0].setVisible(true);
            });
            forwarding.target = window[0];
        }

        Runtime.getRuntime().addShutdownHook(new Thread(source::close));
        source.start();

        if (console) {
            new CountDownLatch(1).await();
        }
    }

    private static void printUsage() {
        System.out.println("""
                Uso:
                  java -jar gateway.jar --port <porta> [--baud 115200] [--api1] [--console]
                  java -jar gateway.jar --demo [--console]
                  java -jar gateway.jar --list-ports

                  --port        porta seriale del gateway XBee (es. COM3, /dev/ttyUSB0)
                  --baud        velocità della porta (default 115200)
                  --api1        XBee in API mode 1 (AP=1); default API mode 2 (AP=2)
                  --demo        genera dati simulati, senza hardware
                  --console     stampa a terminale invece di aprire la finestra
                  --list-ports  elenca le porte seriali disponibili
                """);
    }

    /** Inoltra gli eventi al listener scelto (finestra o console). */
    private static final class ForwardingListener implements ReadingListener {
        private volatile ReadingListener target;

        @Override
        public void onReading(Reading reading) {
            if (target != null) {
                target.onReading(reading);
            }
        }

        @Override
        public void onError(String message) {
            if (target != null) {
                target.onError(message);
            }
        }

        @Override
        public void onStatus(String message) {
            if (target != null) {
                target.onStatus(message);
            }
        }
    }

    private static final class ConsoleListener implements ReadingListener {
        private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

        @Override
        public void onReading(Reading r) {
            WaspmoteFrame f = r.frame();
            System.out.printf("%s  %-10s seq=%-3d %s%n",
                    TIME.format(r.receivedAt()), f.waspmoteId(), f.sequence(), f.fields());
        }

        @Override
        public void onError(String message) {
            System.err.println("Frame scartato: " + message);
        }

        @Override
        public void onStatus(String message) {
            System.out.println(message);
        }
    }

    private static final class Options {
        String port;
        int baud = 115200;
        boolean demo;
        boolean console;
        boolean listPorts;
        boolean apiMode1;

        static Options parse(String[] args) {
            Options o = new Options();
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--port" -> o.port = i + 1 < args.length ? args[++i] : null;
                    case "--baud" -> o.baud = Integer.parseInt(args[++i]);
                    case "--demo" -> o.demo = true;
                    case "--console" -> o.console = true;
                    case "--list-ports" -> o.listPorts = true;
                    case "--api1" -> o.apiMode1 = true;
                    default -> {
                        System.err.println("Opzione sconosciuta: " + args[i]);
                        return null;
                    }
                }
            }
            return (o.listPorts || o.demo || o.port != null) ? o : null;
        }
    }
}
