package fr.lumavision.network;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.screen.ScreenDisplaySettings;
import fr.lumavision.screen.ScreenWallPermissions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → server: update display properties on a merged wall origin block.
 */
public record SetScreenDisplayPacket(BlockPos groupOrigin, ScreenDisplaySettings settings) {

    public static void encode(SetScreenDisplayPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.groupOrigin());
        ScreenDisplaySettings.encode(packet.settings(), buffer);
    }

    public static SetScreenDisplayPacket decode(FriendlyByteBuf buffer) {
        return new SetScreenDisplayPacket(buffer.readBlockPos(), ScreenDisplaySettings.decode(buffer));
    }

    public static void handle(SetScreenDisplayPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> handleOnServer(packet, context.getSender()));
        context.setPacketHandled(true);
    }

    private static void handleOnServer(SetScreenDisplayPacket packet, ServerPlayer player) {
        if (player == null) {
            return;
        }

        BlockPos origin = packet.groupOrigin();
        if (!player.level().hasChunkAt(origin)) {
            return;
        }

        if (player.distanceToSqr(origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D) > 64.0D) {
            return;
        }

        if (!ScreenWallPermissions.canConfigure(player, origin)) {
            LumaVisionMod.LOGGER.debug("Rejected display update from non-owner {}", player.getGameProfile().getName());
            return;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(origin);
        if (!(blockEntity instanceof LedScreenBlockEntity screen)) {
            return;
        }

        screen.setDisplaySettings(packet.settings());
        screen.setChanged();
        player.level().sendBlockUpdated(origin, screen.getBlockState(), screen.getBlockState(), Block.UPDATE_ALL);
    }
}
