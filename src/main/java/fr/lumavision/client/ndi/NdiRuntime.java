package fr.lumavision.client.ndi;

import fr.lumavision.LumaVisionMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

/**
 * One-time NDI native library initialization (client only).
 */
@OnlyIn(Dist.CLIENT)
public final class NdiRuntime {

    private static boolean initialized;
    private static boolean available;
    private static boolean failureLogged;
    private static String failureReason;

    private NdiRuntime() {
    }

    public static synchronized boolean init() {
        if (initialized) {
            return available;
        }
        initialized = true;

        try {
            Class.forName("me.walkerknapp.devolay.Devolay", false, NdiRuntime.class.getClassLoader());
        } catch (ClassNotFoundException classNotFound) {
            available = false;
            failureReason = "Devolay library missing from mod classpath (rebuild with jarJar/minecraftLibrary)";
            logFailure(classNotFound);
            return false;
        }

        try {
            int result = me.walkerknapp.devolay.Devolay.loadLibraries();
            if (result != 0) {
                available = false;
                failureReason = "NDI native libraries failed to load (code " + result + ")";
                logFailure(null);
                return false;
            }
            available = true;
            failureReason = null;
            LumaVisionMod.LOGGER.info("NDI runtime ready ({})", me.walkerknapp.devolay.Devolay.getNDIVersion());
            if (!me.walkerknapp.devolay.Devolay.isSupportedCpu()) {
                LumaVisionMod.LOGGER.warn("CPU may not fully support NDI acceleration");
            }
        } catch (Throwable throwable) {
            available = false;
            failureReason = throwable.getMessage() != null
                    ? throwable.getMessage()
                    : throwable.getClass().getSimpleName();
            logFailure(throwable);
        }
        return available;
    }

    public static boolean isAvailable() {
        return available;
    }

    @Nullable
    public static String getFailureReason() {
        return failureReason;
    }

    private static void logFailure(@Nullable Throwable throwable) {
        if (failureLogged) {
            return;
        }
        failureLogged = true;
        if (throwable != null) {
            LumaVisionMod.LOGGER.error("Failed to initialize NDI runtime: {}", failureReason, throwable);
        } else {
            LumaVisionMod.LOGGER.error("Failed to initialize NDI runtime: {}", failureReason);
        }
    }
}
