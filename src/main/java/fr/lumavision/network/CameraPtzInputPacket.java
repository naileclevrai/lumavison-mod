package fr.lumavision.network;

import fr.lumavision.blockentity.CameraBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → server: a PTZ target (pan/tilt/FOV) received over NDI on a rendering client, applied to the
 * server-authoritative camera parameters and synced back to trackers. Lets an NDI receiver (OBS, vMix,
 * NDI Studio Monitor) drive the virtual camera like a real PTZ camera.
 */
public record CameraPtzInputPacket(BlockPos pos, float pan, float tilt, float fov) {

    public static void encode(CameraPtzInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos());
        buffer.writeFloat(packet.pan());
        buffer.writeFloat(packet.tilt());
        buffer.writeFloat(packet.fov());
    }

    public static CameraPtzInputPacket decode(FriendlyByteBuf buffer) {
        return new CameraPtzInputPacket(buffer.readBlockPos(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(CameraPtzInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleOnServer(packet, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void handleOnServer(CameraPtzInputPacket packet, ServerPlayer player) {
        if (player == null) {
            return;
        }
        BlockPos pos = packet.pos();
        if (!player.level().hasChunkAt(pos)) {
            return;
        }
        BlockEntity be = player.level().getBlockEntity(pos);
        if (!(be instanceof CameraBlockEntity camera)) {
            return;
        }
        camera.parameters().setPan(packet.pan());
        camera.parameters().setTilt(packet.tilt());
        camera.parameters().setFov(packet.fov());
        camera.setChanged();
        if (player.level() instanceof ServerLevel serverLevel) {
            ModNetworking.sendCameraLiveState(serverLevel, pos, camera.parameters());
        }
    }
}
