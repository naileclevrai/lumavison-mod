package fr.lumavision.network;

import fr.lumavision.LumaVisionMod;

/**
 * Entry point for network synchronization (Forge packets).
 * <p>
 * Video control messages, screen state, and NDI streams will be registered here.
 */
public final class ModNetworking {

    private static final String PROTOCOL_VERSION = "1";

    private ModNetworking() {
    }

    /**
     * Registers network channels and handlers. Called during {@code FMLCommonSetupEvent}.
     */
    public static void register() {
        LumaVisionMod.LOGGER.debug("LumaVision networking ready (protocol v{})", PROTOCOL_VERSION);
        // SimpleChannel channel = NetworkRegistry.newSimpleChannel(...)
    }
}
