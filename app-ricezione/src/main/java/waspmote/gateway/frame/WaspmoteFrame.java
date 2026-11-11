package waspmote.gateway.frame;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Frame Waspmote ASCII decodificato.
 *
 * <pre>
 * &lt;=&gt; | tipo | n. campi | # | serial ID | # | waspmote ID | # | sequenza | # | TAG:valore | # | ...
 * </pre>
 *
 * @param frameType   tipo di frame (bit 7 = ASCII, es. 0x86 = information frame v15)
 * @param fieldCount  numero di campi sensore dichiarato nell'intestazione
 * @param serialId    numero di serie della scheda Waspmote
 * @param waspmoteId  identificativo impostato con frame.setID()
 * @param sequence    numero di sequenza (0-255, si azzera)
 * @param fields      campi sensore in ordine, es. TC → "24.31"
 */
public record WaspmoteFrame(int frameType, int fieldCount, String serialId, String waspmoteId,
                            int sequence, Map<String, String> fields) {

    public static final String TEMPERATURE = "TC";
    public static final String HUMIDITY = "HUM";
    public static final String PRESSURE = "PRES";

    public WaspmoteFrame {
        fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    /** Valore numerico di un campo, se presente e numerico. */
    public OptionalDouble number(String tag) {
        String raw = fields.get(tag);
        if (raw == null) {
            return OptionalDouble.empty();
        }
        try {
            return OptionalDouble.of(Double.parseDouble(raw));
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }

    /** Temperatura in °C. */
    public OptionalDouble temperature() {
        return number(TEMPERATURE);
    }

    /** Umidità relativa in %. */
    public OptionalDouble humidity() {
        return number(HUMIDITY);
    }

    /** Pressione in Pa (come la invia la Waspmote). */
    public OptionalDouble pressure() {
        return number(PRESSURE);
    }
}
