package waspmote.gateway;

/** Riceve gli eventi della pipeline: letture valide, errori e messaggi di stato. */
public interface ReadingListener {

    void onReading(Reading reading);

    void onError(String message);

    default void onStatus(String message) {
    }
}
