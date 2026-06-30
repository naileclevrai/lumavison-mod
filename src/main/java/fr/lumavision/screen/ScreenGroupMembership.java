package fr.lumavision.screen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * Describes how a single LED block maps into a merged logical screen (wall).
 * <p>
 * All blocks in a group share the same {@link #groupOrigin} and grid dimensions.
 * Each block renders a sub-rectangle of one shared texture via {@link #uvMinU()} … {@link #uvMaxV()}.
 */
public record ScreenGroupMembership(
        BlockPos groupOrigin,
        int gridWidth,
        int gridHeight,
        int gridX,
        int gridY
) {

    public static final String NBT_ORIGIN = "GroupOrigin";
    public static final String NBT_WIDTH = "GridWidth";
    public static final String NBT_HEIGHT = "GridHeight";
    public static final String NBT_X = "GridX";
    public static final String NBT_Y = "GridY";

    public ScreenGroupMembership {
        if (gridWidth < 1 || gridHeight < 1) {
            throw new IllegalArgumentException("Grid dimensions must be positive");
        }
        if (gridX < 0 || gridY < 0 || gridX >= gridWidth || gridY >= gridHeight) {
            throw new IllegalArgumentException("Grid coordinates out of bounds");
        }
    }

    public static ScreenGroupMembership solo(BlockPos pos) {
        return new ScreenGroupMembership(pos, 1, 1, 0, 0);
    }

    public boolean isMerged() {
        return gridWidth > 1 || gridHeight > 1;
    }

    public int cellCount() {
        return gridWidth * gridHeight;
    }

    public long groupKey() {
        return groupOrigin.asLong();
    }

    /** Left edge of this cell in normalized texture space. */
    public float uvMinU() {
        return (float) gridX / gridWidth;
    }

    /** Right edge of this cell in normalized texture space. */
    public float uvMaxU() {
        return (float) (gridX + 1) / gridWidth;
    }

    /**
     * Top edge of this cell in Minecraft UV space (V decreases upward on the quad).
     */
    public float uvMinV() {
        return 1.0F - (float) (gridY + 1) / gridHeight;
    }

    /** Bottom edge of this cell in Minecraft UV space. */
    public float uvMaxV() {
        return 1.0F - (float) gridY / gridHeight;
    }

    public void write(CompoundTag tag) {
        tag.putLong(NBT_ORIGIN, groupOrigin.asLong());
        tag.putInt(NBT_WIDTH, gridWidth);
        tag.putInt(NBT_HEIGHT, gridHeight);
        tag.putInt(NBT_X, gridX);
        tag.putInt(NBT_Y, gridY);
    }

    public static ScreenGroupMembership read(CompoundTag tag, BlockPos fallbackPos) {
        if (!tag.contains(NBT_WIDTH)) {
            return solo(fallbackPos);
        }
        BlockPos origin = BlockPos.of(tag.getLong(NBT_ORIGIN));
        return new ScreenGroupMembership(
                origin,
                tag.getInt(NBT_WIDTH),
                tag.getInt(NBT_HEIGHT),
                tag.getInt(NBT_X),
                tag.getInt(NBT_Y)
        );
    }
}
