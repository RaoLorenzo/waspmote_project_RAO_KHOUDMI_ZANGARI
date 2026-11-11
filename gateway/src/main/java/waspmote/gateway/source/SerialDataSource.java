package waspmote.gateway.source;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import waspmote.gateway.FramePipeline;

/** Legge i byte dal gateway XBee collegato via USB (porta seriale virtuale). */
public class SerialDataSource implements DataSource {

    private final String portName;
    private final int baudRate;
    private final FramePipeline pipeline;
    private SerialPort port;

    public SerialDataSource(String portName, int baudRate, FramePipeline pipeline) {
        this.portName = portName;
        this.baudRate = baudRate;
        this.pipeline = pipeline;
    }

    /** Porte seriali disponibili, es. COM3 su Windows o /dev/ttyUSB0 su Linux. */
    public static List<String> availablePorts() {
        return Arrays.stream(SerialPort.getCommPorts())
                .map(p -> p.getSystemPortName() + "  (" + p.getDescriptivePortName() + ")")
                .toList();
    }

    @Override
    public void start() throws IOException {
        port = SerialPort.getCommPort(portName);
        port.setComPortParameters(baudRate, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        if (!port.openPort()) {
            throw new IOException("Impossibile aprire la porta " + portName
                    + " (è già usata da un altro programma, es. XCTU o la IDE?)");
        }
        port.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                byte[] data = event.getReceivedData();
                pipeline.feed(data, data.length);
            }
        });
    }

    @Override
    public String description() {
        return "Porta " + portName + " @ " + baudRate + " baud";
    }

    @Override
    public void close() {
        if (port != null) {
            port.removeDataListener();
            port.closePort();
        }
    }
}
