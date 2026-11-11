package waspmote.gateway.seriale;

/** Una sorgente di byte (porta seriale o simulatore) che alimenta la pipeline. */
public interface DataSource extends AutoCloseable {

    /** Avvia la lettura in background. */
    void start() throws Exception;

    /** Descrizione breve da mostrare nell'interfaccia. */
    String description();

    @Override
    void close();
}
