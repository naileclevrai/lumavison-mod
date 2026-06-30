package fr.lumavision.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hitbox for a thin LED panel attached to the block face opposite its {@link LedScreenBlock#FACING}.
 */
public final class LedScreenShapes {

    private static final double MARGIN = 1.0D / 16.0D;
    private static final double DEPTH = 1.0D / 16.0D;
    private static final double MIN = MARGIN;
    private static final double MAX = 1.0D - MARGIN;

    private LedScreenShapes() {
    }

    public static VoxelShape shape(Direction facing) {
        return switch (facing) {
            case NORTH -> Shapes.box(MIN, MIN, 0.0D, MAX, MAX, DEPTH);
            case SOUTH -> Shapes.box(MIN, MIN, 1.0D - DEPTH, MAX, MAX, 1.0D);
            case WEST -> Shapes.box(0.0D, MIN, MIN, DEPTH, MAX, MAX);
            case EAST -> Shapes.box(1.0D - DEPTH, MIN, MIN, 1.0D, MAX, MAX);
            case DOWN -> Shapes.box(MIN, 0.0D, MIN, MAX, DEPTH, MAX);
            case UP -> Shapes.box(MIN, 1.0D - DEPTH, MIN, MAX, 1.0D, MAX);
        };
    }
}
