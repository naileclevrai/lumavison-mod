package fr.lumavision.block;

import fr.lumavision.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a camera's dolly position along a straight {@link CameraRailBlock} run directly beneath it.
 * The camera slides along the run's axis as {@code trackPosition} (0..1) changes; its other axes stay
 * at the block centre. Returns null if the camera isn't sitting on a rail.
 */
public final class RailTrack {

    private static final int MAX_RUN = 256;

    private RailTrack() {
    }

    @Nullable
    public static Vec3 resolve(BlockGetter level, BlockPos cameraPos, float trackPosition) {
        BlockPos below = cameraPos.below();
        BlockState rail = level.getBlockState(below);
        if (!rail.is(ModBlocks.CAMERA_RAIL.get())) {
            return null;
        }
        Direction.Axis axis = rail.getValue(CameraRailBlock.AXIS);
        Direction negative = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        Direction positive = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

        int minCoord = coord(below, axis);
        int maxCoord = minCoord;

        BlockPos.MutableBlockPos cursor = below.mutable();
        for (int i = 0; i < MAX_RUN; i++) {
            cursor.move(negative);
            if (level.getBlockState(cursor).is(ModBlocks.CAMERA_RAIL.get())) {
                minCoord = coord(cursor, axis);
            } else {
                break;
            }
        }
        cursor.set(below);
        for (int i = 0; i < MAX_RUN; i++) {
            cursor.move(positive);
            if (level.getBlockState(cursor).is(ModBlocks.CAMERA_RAIL.get())) {
                maxCoord = coord(cursor, axis);
            } else {
                break;
            }
        }

        float t = Math.max(0.0F, Math.min(1.0F, trackPosition));
        double along = (minCoord + 0.5D) + t * (maxCoord - minCoord);
        double rx = cameraPos.getX() + 0.5D;
        double ry = cameraPos.getY() + 0.5D;
        double rz = cameraPos.getZ() + 0.5D;
        if (axis == Direction.Axis.X) {
            rx = along;
        } else {
            rz = along;
        }
        return new Vec3(rx, ry, rz);
    }

    private static int coord(BlockPos pos, Direction.Axis axis) {
        return axis == Direction.Axis.X ? pos.getX() : pos.getZ();
    }
}
