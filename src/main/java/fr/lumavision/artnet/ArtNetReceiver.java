package fr.lumavision.artnet;

import fr.lumavision.LumaVisionMod;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal server-side Art-Net (ArtDmx) receiver: a UDP listener that keeps the latest DMX values per
 * universe. Self-contained (no third-party dependency) — Art-Net is a fixed-layout UDP packet. Values
 * are read on the server tick to drive camera parameters. Pattern (universe→channel consume) borrowed
 * from Theatrical (MIT).
 */
public final class ArtNetReceiver {

    private static final byte[] ART_NET_ID = "Art-Net\0".getBytes(StandardCharsets.US_ASCII); // 8 bytes
    private static final int OP_DMX = 0x5000;
    private static final int HEADER_LENGTH = 18;
    private static final int MAX_CHANNELS = 512;

    private static volatile ArtNetReceiver instance;

    private final Map<Integer, byte[]> universeData = new ConcurrentHashMap<>();
    private final DatagramSocket socket;
    private final Thread thread;
    private volatile boolean running = true;

    private ArtNetReceiver(String bindAddress, int port) throws Exception {
        this.socket = new DatagramSocket(null);
        this.socket.setReuseAddress(true);
        this.socket.bind(new InetSocketAddress(InetAddress.getByName(bindAddress), port));
        this.thread = new Thread(this::loop, "LumaVision-ArtNet");
        this.thread.setDaemon(true);
        this.thread.start();
    }

    public static synchronized void start(String bindAddress, int port) {
        stop();
        try {
            instance = new ArtNetReceiver(bindAddress, port);
            LumaVisionMod.LOGGER.info("Art-Net receiver listening on {}:{}", bindAddress, port);
        } catch (Exception e) {
            instance = null;
            LumaVisionMod.LOGGER.error("Failed to start Art-Net receiver on {}:{}: {}", bindAddress, port, e.toString());
        }
    }

    public static synchronized void stop() {
        ArtNetReceiver current = instance;
        if (current != null) {
            current.running = false;
            current.socket.close();
            current.thread.interrupt();
            instance = null;
            LumaVisionMod.LOGGER.info("Art-Net receiver stopped");
        }
    }

    public static boolean isRunning() {
        return instance != null;
    }

    /** Latest value (0-255) of a 1-based DMX address in a universe, or 0 if unpatched/no data. */
    public static int channel(int universe, int address1Based) {
        ArtNetReceiver current = instance;
        if (current == null || address1Based < 1 || address1Based > MAX_CHANNELS) {
            return 0;
        }
        byte[] data = current.universeData.get(universe);
        if (data == null) {
            return 0;
        }
        return data[address1Based - 1] & 0xFF;
    }

    private void loop() {
        byte[] buffer = new byte[600];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (running) {
            try {
                socket.receive(packet);
                parse(packet.getData(), packet.getLength());
            } catch (Exception e) {
                if (running) {
                    // transient receive error; keep listening
                    Thread.yield();
                }
            }
        }
    }

    private void parse(byte[] b, int len) {
        if (len < HEADER_LENGTH) {
            return;
        }
        for (int i = 0; i < ART_NET_ID.length; i++) {
            if (b[i] != ART_NET_ID[i]) {
                return;
            }
        }
        int opCode = (b[8] & 0xFF) | ((b[9] & 0xFF) << 8); // little-endian
        if (opCode != OP_DMX) {
            return;
        }
        int universe = (b[14] & 0xFF) | ((b[15] & 0xFF) << 8); // 15-bit port address, little-endian
        int dmxLength = ((b[16] & 0xFF) << 8) | (b[17] & 0xFF); // big-endian
        dmxLength = Math.min(dmxLength, Math.min(MAX_CHANNELS, len - HEADER_LENGTH));
        if (dmxLength <= 0) {
            return;
        }
        byte[] channels = new byte[MAX_CHANNELS];
        System.arraycopy(b, HEADER_LENGTH, channels, 0, dmxLength);
        universeData.put(universe, channels);
    }
}
