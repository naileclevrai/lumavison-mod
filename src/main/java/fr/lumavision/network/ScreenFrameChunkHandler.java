package fr.lumavision.network;

import fr.lumavision.LumaVisionMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Arrays;

/**
 * Reassembles chunked frame packets and forwards complete frames.
 */
public final class ScreenFrameChunkHandler {

    private static final long ASSEMBLY_TIMEOUT_MS = 5_000;

    private static final java.util.Map<AssemblyKey, PendingAssembly> SERVER_ASSEMBLIES = new java.util.HashMap<>();
    private static final java.util.Map<AssemblyKey, PendingAssembly> CLIENT_ASSEMBLIES = new java.util.HashMap<>();

    private ScreenFrameChunkHandler() {
    }

    public static void sendToServer(SimpleChannel channel, BlockPos origin, long sequence,
                                    int width, int height, byte[] compressed) {
        sendChunks(channel, null, origin, sequence, width, height, compressed);
    }

    public static void sendToPlayer(SimpleChannel channel, ServerPlayer player, BlockPos origin, long sequence,
                                    int width, int height, byte[] compressed) {
        sendChunks(channel, player, origin, sequence, width, height, compressed);
    }

    private static void sendChunks(SimpleChannel channel, ServerPlayer player, BlockPos origin, long sequence,
                                   int width, int height, byte[] compressed) {
        int chunkCount = FramePacketLimits.chunkCount(compressed.length);
        for (int i = 0; i < chunkCount; i++) {
            int start = i * FramePacketLimits.MAX_CHUNK_BYTES;
            int length = Math.min(FramePacketLimits.MAX_CHUNK_BYTES, compressed.length - start);
            byte[] chunk = Arrays.copyOfRange(compressed, start, start + length);
            ScreenFrameChunkPacket packet = new ScreenFrameChunkPacket(origin, sequence, width, height, i, chunkCount, chunk);
            if (player == null) {
                channel.sendToServer(packet);
            } else {
                channel.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }

    static void handleUploadOnServer(ScreenFrameChunkPacket packet, ServerPlayer player) {
        if (player == null) {
            return;
        }
        byte[] complete = assemble(SERVER_ASSEMBLIES, packet);
        if (complete == null) {
            return;
        }
        fr.lumavision.server.MediaRelayManager.getInstance().onFrameUpload(
                player,
                packet.groupOrigin(),
                packet.sequence(),
                packet.width(),
                packet.height(),
                complete
        );
    }

    static void handleBroadcastOnClient(ScreenFrameChunkPacket packet) {
        byte[] complete = assemble(CLIENT_ASSEMBLIES, packet);
        if (complete == null) {
            return;
        }
        fr.lumavision.client.relay.MediaRelayClient.getInstance().onFrameReceived(
                packet.groupOrigin(),
                packet.sequence(),
                packet.width(),
                packet.height(),
                complete
        );
    }

    public static void clearServer() {
        SERVER_ASSEMBLIES.clear();
    }

    public static void clearClient() {
        CLIENT_ASSEMBLIES.clear();
    }

    private static byte[] assemble(java.util.Map<AssemblyKey, PendingAssembly> assemblies, ScreenFrameChunkPacket packet) {
        pruneStale(assemblies);
        AssemblyKey key = new AssemblyKey(packet.groupOrigin(), packet.sequence());
        PendingAssembly pending = assemblies.computeIfAbsent(key, ignored -> new PendingAssembly(packet.chunkCount()));
        if (pending.chunkCount != packet.chunkCount()
                || packet.chunkIndex() < 0
                || packet.chunkIndex() >= packet.chunkCount()) {
            assemblies.remove(key);
            return null;
        }
        pending.chunks[packet.chunkIndex()] = packet.chunk();
        pending.received++;
        pending.lastUpdateMs = System.currentTimeMillis();
        if (pending.received < pending.chunkCount) {
            return null;
        }
        assemblies.remove(key);
        int total = 0;
        for (byte[] chunk : pending.chunks) {
            if (chunk == null) {
                return null;
            }
            total += chunk.length;
        }
        byte[] complete = new byte[total];
        int offset = 0;
        for (byte[] chunk : pending.chunks) {
            System.arraycopy(chunk, 0, complete, offset, chunk.length);
            offset += chunk.length;
        }
        return complete;
    }

    private static void pruneStale(java.util.Map<AssemblyKey, PendingAssembly> assemblies) {
        long now = System.currentTimeMillis();
        java.util.Iterator<java.util.Map.Entry<AssemblyKey, PendingAssembly>> iterator = assemblies.entrySet().iterator();
        while (iterator.hasNext()) {
            if (now - iterator.next().getValue().lastUpdateMs > ASSEMBLY_TIMEOUT_MS) {
                iterator.remove();
            }
        }
    }

    private record AssemblyKey(BlockPos origin, long sequence) {
    }

    private static final class PendingAssembly {
        private final int chunkCount;
        private final byte[][] chunks;
        private int received;
        private long lastUpdateMs = System.currentTimeMillis();

        private PendingAssembly(int chunkCount) {
            this.chunkCount = chunkCount;
            this.chunks = new byte[chunkCount][];
        }
    }
}
