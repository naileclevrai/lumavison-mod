package fr.lumavision.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Helpers for splitting frame payloads to stay under Minecraft's ~32 KiB packet limit.
 */
public final class FramePacketLimits {

    /** Max chunk payload — leaves room for headers and Forge packet overhead. */
    public static final int MAX_CHUNK_BYTES = 24_000;

    private FramePacketLimits() {
    }

    public static int chunkCount(int totalBytes) {
        return (totalBytes + MAX_CHUNK_BYTES - 1) / MAX_CHUNK_BYTES;
    }

    public static void writeChunk(FriendlyByteBuf buffer, BlockPos origin, long sequence,
                                  int width, int height, int chunkIndex, int chunkCount, byte[] chunk) {
        buffer.writeBlockPos(origin);
        buffer.writeVarLong(sequence);
        buffer.writeVarInt(width);
        buffer.writeVarInt(height);
        buffer.writeVarInt(chunkIndex);
        buffer.writeVarInt(chunkCount);
        buffer.writeVarInt(chunk.length);
        buffer.writeBytes(chunk);
    }

    public static ScreenFrameChunkPacket readChunk(FriendlyByteBuf buffer) {
        BlockPos origin = buffer.readBlockPos();
        long sequence = buffer.readVarLong();
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();
        int chunkIndex = buffer.readVarInt();
        int chunkCount = buffer.readVarInt();
        int length = buffer.readVarInt();
        if (length < 0 || length > MAX_CHUNK_BYTES) {
            throw new IllegalArgumentException("Invalid frame chunk size: " + length);
        }
        byte[] chunk = new byte[length];
        buffer.readBytes(chunk);
        return new ScreenFrameChunkPacket(origin, sequence, width, height, chunkIndex, chunkCount, chunk);
    }
}
