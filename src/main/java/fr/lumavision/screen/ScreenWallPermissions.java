package fr.lumavision.screen;

import fr.lumavision.blockentity.LedScreenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Server-side checks for who may configure a merged LED wall.
 */
public final class ScreenWallPermissions {

    private ScreenWallPermissions() {
    }

    public static boolean canConfigure(Player player, Level level, BlockPos groupOrigin) {
        if (player == null) {
            return false;
        }
        if (player.hasPermissions(2)) {
            return true;
        }
        if (player.isCreative()) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(groupOrigin);
        if (!(blockEntity instanceof LedScreenBlockEntity screen)) {
            return false;
        }

        UUID owner = screen.getOwnerUuid();
        if (owner == null) {
            return true;
        }
        return owner.equals(player.getUUID());
    }

    public static boolean canConfigure(ServerPlayer player, BlockPos groupOrigin) {
        return canConfigure((Player) player, player.level(), groupOrigin);
    }

    @Nullable
    public static UUID resolveOwner(Level level, BlockPos groupOrigin) {
        BlockEntity blockEntity = level.getBlockEntity(groupOrigin);
        if (blockEntity instanceof LedScreenBlockEntity screen) {
            return screen.getOwnerUuid();
        }
        return null;
    }
}
