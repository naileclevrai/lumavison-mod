package fr.lumavision.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 2D coordinate system on the plane of a wall-mounted LED screen.
 * <p>
 * {@code horizontal} spans the width of the wall; {@code vertical} spans its height.
 */
public final class WallPlane {

    private final Direction facing;
    private final Direction horizontal;
    private final Direction vertical;

    private WallPlane(Direction facing, Direction horizontal, Direction vertical) {
        this.facing = facing;
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public static WallPlane forFacing(Direction facing) {
        return switch (facing) {
            case NORTH, SOUTH -> new WallPlane(facing, Direction.EAST, Direction.UP);
            case EAST, WEST -> new WallPlane(facing, Direction.SOUTH, Direction.UP);
            case UP -> new WallPlane(facing, Direction.EAST, Direction.SOUTH);
            case DOWN -> new WallPlane(facing, Direction.EAST, Direction.NORTH);
        };
    }

    public Direction facing() {
        return facing;
    }

    /** Coordinate along the axis perpendicular to the wall (must match for coplanar blocks). */
    public int depthCoordinate(BlockPos pos) {
        return switch (facing.getAxis()) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
    }

    public int horizontal(BlockPos pos) {
        return axisCoordinate(pos, horizontal);
    }

    public int vertical(BlockPos pos) {
        return axisCoordinate(pos, vertical);
    }

    public BlockPos stepHorizontal(BlockPos pos, int delta) {
        return pos.relative(horizontal, delta);
    }

    public BlockPos stepVertical(BlockPos pos, int delta) {
        return pos.relative(vertical, delta);
    }

    public Iterable<BlockPos> wallNeighbors(BlockPos pos) {
        return java.util.List.of(
                stepHorizontal(pos, 1),
                stepHorizontal(pos, -1),
                stepVertical(pos, 1),
                stepVertical(pos, -1)
        );
    }

    public boolean isCoplanar(BlockPos a, BlockPos b) {
        return depthCoordinate(a) == depthCoordinate(b);
    }

    private static int axisCoordinate(BlockPos pos, Direction axis) {
        int value = switch (axis.getAxis()) {
            case X -> pos.getX();
            case Y -> pos.getY();
            case Z -> pos.getZ();
        };
        return axis.getAxisDirection() == Direction.AxisDirection.POSITIVE ? value : -value;
    }
}
