package fr.lumavision.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * A fragment of a compressed frame payload (C→S upload or S→C broadcast).
 * Full frames are split to respect Minecraft's ~32 KiB packet size limit.
 */
public record ScreenFrameChunkPacket(
        BlockPos groupOrigin,
        long sequence,
        int width,
        int height,
        int chunkIndex,
        int chunkCount,
        byte[] chunk
) {

    public static void encode(ScreenFrameChunkPacket packet, FriendlyByteBuf buffer) {
        FramePacketLimits.writeChunk(
                buffer,
                packet.groupOrigin(),
                packet.sequence(),
                packet.width(),
                packet.height(),
                packet.chunkIndex(),
                packet.chunkCount(),
                packet.chunk()
        );
    }

    public static ScreenFrameChunkPacket decode(FriendlyByteBuf buffer) {
        return FramePacketLimits.readChunk(buffer);
    }

    public static void handle(ScreenFrameChunkPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isServer()) {
                ScreenFrameChunkHandler.handleUploadOnServer(packet, context.getSender());
            } else {
                ScreenFrameChunkHandler.handleBroadcastOnClient(packet);
            }
        });
        context.setPacketHandled(true);
    }
}
