package fr.lumavision.client.ndi;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.config.ModConfig;
import me.walkerknapp.devolay.DevolayFinder;
import me.walkerknapp.devolay.DevolayReceiver;
import me.walkerknapp.devolay.DevolaySource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Background NDI source discovery using {@link DevolayFinder}.
 */
@OnlyIn(Dist.CLIENT)
public final class NdiDiscoveryService {

    private static final NdiDiscoveryService INSTANCE = new NdiDiscoveryService();

    private final AtomicReference<List<NdiSourceInfo>> discoveredSources =
            new AtomicReference<>(List.of());

    private DevolayFinder finder;
    private Thread discoveryThread;
    private volatile boolean running;

    private NdiDiscoveryService() {
    }

    public static NdiDiscoveryService getInstance() {
        return INSTANCE;
    }

    public synchronized void start() {
        if (!NdiRuntime.init()) {
            return;
        }
        if (running && discoveryThread != null && discoveryThread.isAlive()) {
            return;
        }
        if (finder == null) {
            finder = new DevolayFinder(true);
        }
        running = true;
        discoveryThread = new Thread(this::discoveryLoop, "LumaVision-NDI-Discovery");
        discoveryThread.setDaemon(true);
        discoveryThread.start();
        LumaVisionMod.LOGGER.info("NDI discovery started");
    }

    public synchronized void shutdown() {
        running = false;
        Thread thread = discoveryThread;
        discoveryThread = null;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join(2000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        if (finder != null) {
            finder.close();
            finder = null;
        }
        discoveredSources.set(List.of());
    }

    public List<NdiSourceInfo> getDiscoveredSources() {
        return discoveredSources.get();
    }

    /**
     * Diagnostic: sources visible over the NETWORK only. Unlike the normal finder (which also lists
     * in-process/local sources from NDI's local registry), this uses {@code showLocalSources=false},
     * so a source appearing here proves it is actually being advertised via mDNS to other machines.
     * Blocks up to {@code waitMs} for discovery. Call off the main thread.
     */
    public List<String> probeNetworkSources(long waitMs) {
        if (!NdiRuntime.isAvailable()) {
            return List.of();
        }
        DevolayFinder netFinder = new DevolayFinder(false); // network/mDNS only — no local registry
        try {
            Thread.sleep(Math.max(500L, waitMs));
            DevolaySource[] sources = netFinder.getCurrentSources();
            List<String> names = new ArrayList<>(sources.length);
            for (DevolaySource source : sources) {
                names.add(source.getSourceName());
            }
            return names;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (Throwable throwable) {
            LumaVisionMod.LOGGER.error("NDI network probe failed", throwable);
            return List.of();
        } finally {
            netFinder.close();
        }
    }

    public String getFirstSourceName() {
        List<NdiSourceInfo> sources = discoveredSources.get();
        return sources.isEmpty() ? null : sources.get(0).sourceName();
    }

    /**
     * Opens a receiver connected to the named source using the latest finder snapshot.
     */
    public synchronized DevolayReceiver openReceiverForSource(String sourceName) {
        if (!NdiRuntime.isAvailable()) {
            return null;
        }
        if (finder == null) {
            finder = new DevolayFinder(true);
        }

        DevolaySource[] sources = finder.getCurrentSources();
        for (DevolaySource source : sources) {
            if (source.getSourceName().equals(sourceName)) {
                return new DevolayReceiver(
                        source,
                        DevolayReceiver.ColorFormat.RGBX_RGBA,
                        DevolayReceiver.RECEIVE_BANDWIDTH_HIGHEST,
                        false,
                        "LumaVision"
                );
            }
        }
        return null;
    }

    private void discoveryLoop() {
        while (running) {
            try {
                pollSources();
                Thread.sleep(Math.max(250, ModConfig.NDI_DISCOVERY_INTERVAL_MS.get()));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable throwable) {
                LumaVisionMod.LOGGER.error("NDI discovery error", throwable);
            }
        }
    }

    private void pollSources() {
        if (finder == null) {
            return;
        }

        List<NdiSourceInfo> previous = discoveredSources.get();
        DevolaySource[] sources = finder.getCurrentSources();
        List<NdiSourceInfo> current = new ArrayList<>(sources.length);
        for (DevolaySource source : sources) {
            current.add(new NdiSourceInfo(source.getSourceName()));
        }
        current = Collections.unmodifiableList(current);
        discoveredSources.set(current);

        if (ModConfig.DEBUG_LOGGING.get() && !current.equals(previous)) {
            LumaVisionMod.LOGGER.debug("NDI sources updated ({} found)", current.size());
            for (NdiSourceInfo info : current) {
                LumaVisionMod.LOGGER.debug("  - {}", info.sourceName());
            }
        }
    }
}
