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
 * Client → server: operator input from the camera seat. Drives the camera's PTZ head:
 * strafe (A/D) → pan, forward (W/S) → tilt, scroll → zoom (FOV). Sent while operating; the server
 * applies to the seated camera's parameters and syncs to trackers.
 */
public record CameraRigInputPacket(BlockPos cameraPos, float forward, float strafe, float scroll) {

    private static final float PAN_PER_TICK = 4.0F;
    private static final float TILT_PER_TICK = 3.0F;
    private static final float ZOOM_PER_NOTCH = 8.0F;

    public static void encode(CameraRigInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.cameraPos());
        buffer.writeFloat(packet.forward());
        buffer.writeFloat(packet.strafe());
        buffer.writeFloat(packet.scroll());
    }

    public static CameraRigInputPacket decode(FriendlyByteBuf buffer) {
        return new CameraRigInputPacket(buffer.readBlockPos(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(CameraRigInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleOnServer(packet, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void handleOnServer(CameraRigInputPacket packet, ServerPlayer player) {
        if (player == null) {
            return;
        }
        BlockPos pos = packet.cameraPos();
        if (!player.level().hasChunkAt(pos)
                || player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 4096.0D) {
            return;
        }
        BlockEntity be = player.level().getBlockEntity(pos);
        if (!(be instanceof CameraBlockEntity camera)) {
            return;
        }
        // strafe A/D -> pan; forward W/S -> tilt (up = negative pitch); scroll -> zoom (fov).
        camera.parameters().setPan(camera.parameters().pan() - packet.strafe() * PAN_PER_TICK);
        camera.parameters().setTilt(camera.parameters().tilt() - packet.forward() * TILT_PER_TICK);
        if (packet.scroll() != 0.0F) {
            camera.parameters().setFov(camera.parameters().fov() - packet.scroll() * ZOOM_PER_NOTCH);
        }
        camera.setChanged();
        if (player.level() instanceof ServerLevel serverLevel) {
            ModNetworking.sendCameraLiveState(serverLevel, pos, camera.parameters());
        }
    }
}
