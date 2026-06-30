package fr.lumavision.network;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.video.VideoSourceDescriptors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → server: bind a media source to a merged wall origin block.
 */
public record SetScreenSourcePacket(BlockPos groupOrigin, String sourceId) {

    private static final int MAX_SOURCE_ID_LENGTH = 512;

    public static void encode(SetScreenSourcePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.groupOrigin());
        buffer.writeUtf(packet.sourceId(), MAX_SOURCE_ID_LENGTH);
    }

    public static SetScreenSourcePacket decode(FriendlyByteBuf buffer) {
        return new SetScreenSourcePacket(buffer.readBlockPos(), buffer.readUtf(MAX_SOURCE_ID_LENGTH));
    }

    public static void handle(SetScreenSourcePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleOnServer(packet, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void handleOnServer(SetScreenSourcePacket packet, ServerPlayer player) {
        if (player == null) {
            return;
        }

        if (!VideoSourceDescriptors.isRecognizedSourceId(packet.sourceId())) {
            LumaVisionMod.LOGGER.warn("Rejected unrecognized screen source id from {}", player.getGameProfile().getName());
            return;
        }

        BlockPos origin = packet.groupOrigin();
        if (!player.level().hasChunkAt(origin)) {
            return;
        }

        if (player.distanceToSqr(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D) > 64.0D) {
            return;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(origin);
        if (!(blockEntity instanceof LedScreenBlockEntity screen)) {
            return;
        }

        screen.setSourceId(packet.sourceId().trim());
        screen.setChanged();
        player.level().sendBlockUpdated(origin, screen.getBlockState(), screen.getBlockState(), Block.UPDATE_ALL);
    }
}
