package waspmote.gateway.core;

import java.time.LocalDateTime;
import waspmote.gateway.frame.WaspmoteFrame;
import waspmote.gateway.frame.WaspmoteFrameParser;
import waspmote.gateway.xbee.XBeeApiDecoder;
import waspmote.gateway.xbee.XBeeApiFrame;
import waspmote.gateway.xbee.ZigBeeReceivePacket;

/**
 * Collega i livelli di decodifica:
 * byte seriali → frame XBee API → pacchetto ZigBee 0x90 → frame Waspmote.
 *
 * <p>Qualunque sorgente (porta seriale, demo, test) chiama {@link #feed}.
 */
public class FramePipeline {

    private final XBeeApiDecoder decoder;
    private final ReadingListener listener;

    public FramePipeline(boolean escapedApi, ReadingListener listener) {
        this.listener = listener;
        this.decoder = new XBeeApiDecoder(escapedApi, this::onApiFrame, listener::onError);
    }

    public synchronized void feed(byte[] buffer, int count) {
        decoder.feed(buffer, count);
    }

    private void onApiFrame(XBeeApiFrame apiFrame) {
        if (apiFrame.type() != XBeeApiFrame.ZIGBEE_RECEIVE_PACKET) {
            // Es. risposte a comandi AT o modem status: per ora le ignoriamo
            listener.onStatus(String.format("Frame XBee 0x%02X ignorato", apiFrame.type()));
            return;
        }
        try {
            ZigBeeReceivePacket packet = ZigBeeReceivePacket.parse(apiFrame);
            WaspmoteFrame frame = WaspmoteFrameParser.parse(packet.rfData());
            listener.onReading(new Reading(LocalDateTime.now(), packet.source64Hex(), frame));
        } catch (RuntimeException e) {
            listener.onError(e.getMessage());
        }
    }
}
