package fr.lumavision.network;

import fr.lumavision.blockentity.CameraBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → server: apply GUI-authored static camera configuration (NDI name, resolution, fps, FOV,
 * manual aim, enabled). Live motion (pan/tilt/zoom/track) is not carried here — it is authored on the
 * server and pushed to clients via a separate lightweight packet.
 */
public final class ConfigureCameraPacket {

    private final BlockPos cameraPos;
    private final FriendlyByteBuf config;

    public ConfigureCameraPacket(BlockPos cameraPos, FriendlyByteBuf config) {
        this.cameraPos = cameraPos;
        this.config = config;
    }

    public static void encode(ConfigureCameraPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.cameraPos);
        buffer.writeVarInt(packet.config.readableBytes());
        buffer.writeBytes(packet.config.slice());
    }

    public static ConfigureCameraPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int len = buffer.readVarInt();
        FriendlyByteBuf payload = new FriendlyByteBuf(buffer.readBytes(len));
        return new ConfigureCameraPacket(pos, payload);
    }

    public static void handle(ConfigureCameraPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleOnServer(packet, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void handleOnServer(ConfigureCameraPacket packet, ServerPlayer player) {
        if (player == null) {
            return;
        }
        BlockPos pos = packet.cameraPos;
        if (!player.level().hasChunkAt(pos)) {
            return;
        }
        if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof CameraBlockEntity camera)) {
            return;
        }
        camera.parameters().readConfig(packet.config);
        camera.onParametersChanged();
    }
}
