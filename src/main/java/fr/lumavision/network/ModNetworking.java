package fr.lumavision.network;

import fr.lumavision.LumaVisionMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Entry point for network synchronization (Forge packets).
 */
public final class ModNetworking {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(LumaVisionMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextPacketId;

    private ModNetworking() {
    }

    /**
     * Registers network channels and handlers. Called during {@code FMLCommonSetupEvent}.
     */
    public static void register() {
        CHANNEL.registerMessage(
                nextPacketId++,
                SetScreenSourcePacket.class,
                SetScreenSourcePacket::encode,
                SetScreenSourcePacket::decode,
                SetScreenSourcePacket::handle
        );
        LumaVisionMod.LOGGER.debug("LumaVision networking ready (protocol v{})", PROTOCOL_VERSION);
    }
}
