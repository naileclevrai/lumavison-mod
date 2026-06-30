package fr.lumavision.screen;

import fr.lumavision.block.LedScreenBlock;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Detects connected LED screen components on a wall plane and computes grid layout.
 */
public final class ScreenGroupResolver {

    private ScreenGroupResolver() {
    }

    /**
     * Collects every logical screen group that may have changed around {@code center}.
     */
    public static Set<Set<BlockPos>> findAffectedComponents(Level level, BlockPos center) {
        Set<BlockPos> seeds = gatherSeeds(level, center);
        Set<BlockPos> visited = new HashSet<>();
        Set<Set<BlockPos>> components = new LinkedHashSet<>();

        for (BlockPos seed : seeds) {
            if (visited.contains(seed)) {
                continue;
            }
            Set<BlockPos> component = floodFill(level, seed);
            visited.addAll(component);
            if (!component.isEmpty()) {
                components.add(component);
            }
        }
        return components;
    }

    /**
     * Assigns {@link ScreenGroupMembership} for every block in a connected component.
     */
    public static Map<BlockPos, ScreenGroupMembership> resolveMemberships(Level level, Set<BlockPos> blocks) {
        if (blocks.isEmpty()) {
            return Map.of();
        }
        if (blocks.size() == 1) {
            BlockPos only = blocks.iterator().next();
            return Map.of(only, ScreenGroupMembership.solo(only));
        }

        BlockPos reference = blocks.iterator().next();
        LedScreenBlockEntity referenceEntity = getScreen(level, reference);
        if (referenceEntity == null) {
            BlockPos only = blocks.iterator().next();
            return Map.of(only, ScreenGroupMembership.solo(only));
        }

        Direction facing = referenceEntity.getFacing();
        WallPlane plane = WallPlane.forFacing(facing);
        int depth = plane.depthCoordinate(reference);

        int minH = Integer.MAX_VALUE;
        int minV = Integer.MAX_VALUE;
        int maxH = Integer.MIN_VALUE;
        int maxV = Integer.MIN_VALUE;
        Map<BlockPos, int[]> coordinates = new HashMap<>();

        for (BlockPos pos : blocks) {
            LedScreenBlockEntity entity = getScreen(level, pos);
            if (entity == null || entity.getFacing() != facing || plane.depthCoordinate(pos) != depth) {
                continue;
            }
            int h = plane.horizontal(pos);
            int v = plane.vertical(pos);
            coordinates.put(pos, new int[]{h, v});
            minH = Math.min(minH, h);
            minV = Math.min(minV, v);
            maxH = Math.max(maxH, h);
            maxV = Math.max(maxV, v);
        }

        int gridWidth = maxH - minH + 1;
        int gridHeight = maxV - minV + 1;
        BlockPos origin = findOrigin(coordinates, minH, minV);

        Map<BlockPos, ScreenGroupMembership> memberships = new HashMap<>();
        for (Map.Entry<BlockPos, int[]> entry : coordinates.entrySet()) {
            int gridX = entry.getValue()[0] - minH;
            int gridY = entry.getValue()[1] - minV;
            memberships.put(entry.getKey(), new ScreenGroupMembership(origin, gridWidth, gridHeight, gridX, gridY));
        }
        return memberships;
    }

    private static BlockPos findOrigin(Map<BlockPos, int[]> coordinates, int minH, int minV) {
        for (Map.Entry<BlockPos, int[]> entry : coordinates.entrySet()) {
            int[] coords = entry.getValue();
            if (coords[0] == minH && coords[1] == minV) {
                return entry.getKey();
            }
        }
        return coordinates.keySet().iterator().next();
    }

    private static Set<BlockPos> gatherSeeds(Level level, BlockPos center) {
        Set<BlockPos> seeds = new LinkedHashSet<>();
        if (isLedScreen(level, center)) {
            seeds.add(center);
        }
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = center.relative(direction);
            if (isLedScreen(level, neighbor)) {
                seeds.add(neighbor);
            }
        }
        return seeds;
    }

    private static Set<BlockPos> floodFill(Level level, BlockPos start) {
        LedScreenBlockEntity startEntity = getScreen(level, start);
        if (startEntity == null) {
            return Set.of();
        }

        Direction facing = startEntity.getFacing();
        WallPlane plane = WallPlane.forFacing(facing);
        int depth = plane.depthCoordinate(start);

        Set<BlockPos> component = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            if (!component.add(pos)) {
                continue;
            }

            for (BlockPos neighbor : plane.wallNeighbors(pos)) {
                if (component.contains(neighbor)) {
                    continue;
                }
                LedScreenBlockEntity neighborEntity = getScreen(level, neighbor);
                if (neighborEntity == null) {
                    continue;
                }
                if (neighborEntity.getFacing() != facing || plane.depthCoordinate(neighbor) != depth) {
                    continue;
                }
                queue.add(neighbor);
            }
        }
        return component;
    }

    private static boolean isLedScreen(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(ModBlocks.LED_SCREEN.get());
    }

    private static LedScreenBlockEntity getScreen(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof LedScreenBlockEntity screen)) {
            return null;
        }
        if (!level.getBlockState(pos).is(ModBlocks.LED_SCREEN.get())) {
            return null;
        }
        return screen;
    }
}
