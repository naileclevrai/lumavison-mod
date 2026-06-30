package fr.lumavision.client.shimmer;

import fr.lumavision.block.LedScreenBlock;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.screen.ScreenGroupMembership;
import fr.lumavision.screen.WallPlane;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

/**
 * World-space position and radius for a single ambilight point on a merged LED wall.
 */
@OnlyIn(Dist.CLIENT)
public final class WallLightPlacement {

    public record Placement(Vector3f position, float radius) {
    }

    private WallLightPlacement() {
    }

    public static Placement compute(Level level, ScreenGroupMembership membership, float radiusMultiplier, float forwardOffset) {
        BlockPos origin = membership.groupOrigin();
        BlockEntity blockEntity = level.getBlockEntity(origin);
        if (!(blockEntity instanceof LedScreenBlockEntity)) {
            return new Placement(new Vector3f(origin.getX() + 0.5F, origin.getY() + 0.5F, origin.getZ() + 0.5F), radiusMultiplier);
        }

        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(LedScreenBlock.FACING);
        WallPlane plane = WallPlane.forFacing(facing);

        float centerH = (membership.gridWidth() - 1) * 0.5F;
        float centerV = (membership.gridHeight() - 1) * 0.5F;

        BlockPos centerCell = plane.stepVertical(plane.stepHorizontal(origin, Math.round(centerH)), Math.round(centerV));
        float x = centerCell.getX() + 0.5F + facing.getStepX() * forwardOffset;
        float y = centerCell.getY() + 0.5F + facing.getStepY() * forwardOffset;
        float z = centerCell.getZ() + 0.5F + facing.getStepZ() * forwardOffset;

        float radius = Math.max(membership.gridWidth(), membership.gridHeight()) * radiusMultiplier;
        return new Placement(new Vector3f(x, y, z), radius);
    }
}
