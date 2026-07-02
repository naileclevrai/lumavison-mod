package fr.lumavision.network;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.camera.CameraParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * Entry point for network synchronization (Forge packets).
 */
public final class ModNetworking {

    private static final String PROTOCOL_VERSION = "2";

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
        CHANNEL.registerMessage(
                nextPacketId++,
                SetScreenDisplayPacket.class,
                SetScreenDisplayPacket::encode,
                SetScreenDisplayPacket::decode,
                SetScreenDisplayPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                ConfigureCameraPacket.class,
                ConfigureCameraPacket::encode,
                ConfigureCameraPacket::decode,
                ConfigureCameraPacket::handle
        );
        CHANNEL.registerMessage(
                nextPacketId++,
                CameraLiveStatePacket.class,
                CameraLiveStatePacket::encode,
                CameraLiveStatePacket::decode,
                CameraLiveStatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        LumaVisionMod.LOGGER.debug("LumaVision networking ready (protocol v{})", PROTOCOL_VERSION);
    }

    /** Server → clients tracking the camera's chunk: push live DMX-driven camera motion. */
    public static void sendCameraLiveState(ServerLevel level, BlockPos pos, CameraParameters parameters) {
        CHANNEL.send(
                PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new CameraLiveStatePacket(pos, parameters.pan(), parameters.tilt(),
                        parameters.fov(), parameters.trackPosition()));
    }
}
