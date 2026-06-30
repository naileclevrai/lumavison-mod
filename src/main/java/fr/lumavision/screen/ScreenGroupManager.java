package fr.lumavision.screen;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.Set;

/**
 * Server-side coordinator for merged LED screen walls.
 * <p>
 * Rebuilds logical groups when blocks are placed, broken, or loaded so every
 * wall shares one {@link ScreenGroupMembership} layout and one future video source.
 */
public final class ScreenGroupManager {

    private ScreenGroupManager() {
    }

    public static void rebuildAround(Level level, BlockPos center) {
        if (level.isClientSide()) {
            return;
        }

        Set<Set<BlockPos>> components = ScreenGroupResolver.findAffectedComponents(level, center);
        for (Set<BlockPos> component : components) {
            applyMemberships(level, ScreenGroupResolver.resolveMemberships(level, component));
        }
    }

    private static void applyMemberships(Level level, Map<BlockPos, ScreenGroupMembership> memberships) {
        for (Map.Entry<BlockPos, ScreenGroupMembership> entry : memberships.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!(level.getBlockEntity(pos) instanceof LedScreenBlockEntity screen)) {
                continue;
            }

            ScreenGroupMembership membership = entry.getValue();
            if (screen.getGroupMembership().equals(membership)) {
                continue;
            }

            screen.setGroupMembership(membership);

            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);

            if (membership.isMerged()) {
                LumaVisionMod.LOGGER.debug(
                        "LED group updated at {} -> {}x{} cell ({}, {}) origin {}",
                        pos,
                        membership.gridWidth(),
                        membership.gridHeight(),
                        membership.gridX(),
                        membership.gridY(),
                        membership.groupOrigin()
                );
            }
        }
    }
}
