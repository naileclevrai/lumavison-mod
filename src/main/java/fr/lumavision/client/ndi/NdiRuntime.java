package fr.lumavision.client.ndi;

import fr.lumavision.LumaVisionMod;
import me.walkerknapp.devolay.Devolay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * One-time NDI native library initialization (client only).
 */
@OnlyIn(Dist.CLIENT)
public final class NdiRuntime {

    private static boolean initialized;
    private static boolean available;

    private NdiRuntime() {
    }

    public static synchronized boolean init() {
        if (initialized) {
            return available;
        }
        initialized = true;
        try {
            int result = Devolay.loadLibraries();
            if (result != 0) {
                LumaVisionMod.LOGGER.error("NDI libraries failed to load (code {})", result);
                available = false;
                return false;
            }
            available = true;
            LumaVisionMod.LOGGER.info("NDI runtime ready ({})", Devolay.getNDIVersion());
            if (!Devolay.isSupportedCpu()) {
                LumaVisionMod.LOGGER.warn("CPU may not fully support NDI acceleration");
            }
        } catch (Throwable throwable) {
            available = false;
            LumaVisionMod.LOGGER.error("Failed to initialize NDI runtime", throwable);
        }
        return available;
    }

    public static boolean isAvailable() {
        return available;
    }
}
