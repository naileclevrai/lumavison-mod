package fr.lumavision.network;

import fr.lumavision.client.CameraClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server → client: live camera motion (pan/tilt/fov/track) authored on the server from Art-Net,
 * pushed to players tracking the camera's chunk. Lightweight and rate-limited (sent only when values
 * change) so smooth DMX motion doesn't require full block-entity resyncs.
 */
public record CameraLiveStatePacket(BlockPos pos, float pan, float tilt, float fov, float trackPos,
                                    float boomSwing, float boomPitch, float boomLength) {

    public static void encode(CameraLiveStatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos());
        buffer.writeFloat(packet.pan());
        buffer.writeFloat(packet.tilt());
        buffer.writeFloat(packet.fov());
        buffer.writeFloat(packet.trackPos());
        buffer.writeFloat(packet.boomSwing());
        buffer.writeFloat(packet.boomPitch());
        buffer.writeFloat(packet.boomLength());
    }

    public static CameraLiveStatePacket decode(FriendlyByteBuf buffer) {
        return new CameraLiveStatePacket(buffer.readBlockPos(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
    }

    public static void handle(CameraLiveStatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CameraClientPacketHandler.applyLiveState(packet)));
        context.setPacketHandled(true);
    }
}
