package waspmote.gateway.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.OptionalDouble;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import waspmote.gateway.core.Reading;
import waspmote.gateway.core.ReadingListener;
import waspmote.gateway.frame.WaspmoteFrame;

/**
 * Finestra principale: ultimi valori in grande e storico delle letture.
 * Volutamente semplice (solo Swing, incluso nel JDK).
 */
public class MainWindow extends JFrame implements ReadingListener {

    private static final int MAX_ROWS = 500;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JLabel temperatureValue = bigValue();
    private final JLabel humidityValue = bigValue();
    private final JLabel pressureValue = bigValue();
    private final JLabel status = new JLabel(" ");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] {"Ora", "Nodo", "MAC XBee", "Seq", "T (°C)", "UR (%)", "P (hPa)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public MainWindow(String sourceDescription) {
        super("Waspmote gateway - " + sourceDescription);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel cards = new JPanel(new GridLayout(1, 3, 12, 0));
        cards.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));
        cards.add(card("Temperatura", temperatureValue));
        cards.add(card("Umidità relativa", humidityValue));
        cards.add(card("Pressione", pressureValue));
        add(cards, BorderLayout.NORTH);

        JTable table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 12, 0, 12), scroll.getBorder()));
        add(scroll, BorderLayout.CENTER);

        status.setBorder(BorderFactory.createEmptyBorder(4, 12, 8, 12));
        add(status, BorderLayout.SOUTH);
        setStatus("In ascolto su: " + sourceDescription, false);

        setSize(820, 520);
        setLocationRelativeTo(null);
    }

    @Override
    public void onReading(Reading reading) {
        SwingUtilities.invokeLater(() -> {
            WaspmoteFrame f = reading.frame();
            temperatureValue.setText(format(f.temperature(), 1, 1) + " °C");
            humidityValue.setText(format(f.humidity(), 1, 1) + " %");
            pressureValue.setText(format(f.pressure(), 100, 1) + " hPa");

            tableModel.insertRow(0, new Object[] {
                TIME.format(reading.receivedAt()),
                f.waspmoteId(),
                reading.sourceMac(),
                f.sequence(),
                format(f.temperature(), 1, 2),
                format(f.humidity(), 1, 1),
                format(f.pressure(), 100, 2),
            });
            if (tableModel.getRowCount() > MAX_ROWS) {
                tableModel.removeRow(MAX_ROWS);
            }
            setStatus("Ultimo frame da " + f.waspmoteId() + " alle " + TIME.format(reading.receivedAt()), false);
        });
    }

    @Override
    public void onError(String message) {
        SwingUtilities.invokeLater(() -> setStatus("Frame scartato: " + message, true));
    }

    @Override
    public void onStatus(String message) {
        SwingUtilities.invokeLater(() -> setStatus(message, false));
    }

    private void setStatus(String text, boolean error) {
        status.setText(text);
        status.setForeground(error ? new Color(0xB00020) : Color.DARK_GRAY);
    }

    private static String format(OptionalDouble value, double divisor, int decimals) {
        return value.isPresent()
                ? String.format(Locale.ITALY, "%." + decimals + "f", value.getAsDouble() / divisor)
                : "-";
    }

    private static JLabel bigValue() {
        JLabel label = new JLabel("-", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 30f));
        return label;
    }

    private static JPanel card(String title, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }
}
