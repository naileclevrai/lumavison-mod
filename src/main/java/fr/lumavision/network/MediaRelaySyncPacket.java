package fr.lumavision.network;

import fr.lumavision.client.relay.MediaRelayClient;
import fr.lumavision.relay.WallRelayRole;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Server → client: per-player relay role assignments for walls and cameras.
 */
public record MediaRelaySyncPacket(
        Map<BlockPos, WallRelayRole> wallRoles,
        Set<BlockPos> captureCameras
) {

    public static void encode(MediaRelaySyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.wallRoles().size());
        for (Map.Entry<BlockPos, WallRelayRole> entry : packet.wallRoles().entrySet()) {
            buffer.writeBlockPos(entry.getKey());
            buffer.writeEnum(entry.getValue());
        }
        buffer.writeVarInt(packet.captureCameras().size());
        for (BlockPos pos : packet.captureCameras()) {
            buffer.writeBlockPos(pos);
        }
    }

    public static MediaRelaySyncPacket decode(FriendlyByteBuf buffer) {
        int wallCount = buffer.readVarInt();
        Map<BlockPos, WallRelayRole> wallRoles = new HashMap<>(wallCount);
        for (int i = 0; i < wallCount; i++) {
            wallRoles.put(buffer.readBlockPos(), buffer.readEnum(WallRelayRole.class));
        }
        int cameraCount = buffer.readVarInt();
        Set<BlockPos> cameras = new HashSet<>();
        for (int i = 0; i < cameraCount; i++) {
            cameras.add(buffer.readBlockPos());
        }
        return new MediaRelaySyncPacket(wallRoles, cameras);
    }

    public static void handle(MediaRelaySyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> MediaRelayClient.getInstance().onSync(packet.wallRoles(), packet.captureCameras()));
        context.setPacketHandled(true);
    }
}
