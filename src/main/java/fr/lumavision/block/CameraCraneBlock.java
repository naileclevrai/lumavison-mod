package fr.lumavision.block;

import fr.lumavision.entity.CameraSeatEntity;
import fr.lumavision.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A full 3D camera crane. Unlike the plain camera, the whole rig (base, swinging turntable, booming
 * arm, camera head, counterweight) is drawn by {@link fr.lumavision.client.render.CameraCraneRenderer}
 * so the block model is invisible. Right-click to sit in the operator seat and drive it with WASD
 * (swing/boom) + scroll (zoom); sneak-right-click opens the camera configuration menu.
 */
public class CameraCraneBlock extends CameraBlock {

    public CameraCraneBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // The crane is rendered entirely by its block-entity renderer.
        return RenderShape.INVISIBLE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (player.isSecondaryUseActive()) {
            // Sneak-click configures the camera (NDI name, resolution, …).
            return super.use(state, level, pos, player, hand, hit);
        }
        if (!level.isClientSide) {
            AABB here = new AABB(pos);
            if (level.getEntitiesOfClass(CameraSeatEntity.class, here).isEmpty()) {
                CameraSeatEntity seat = new CameraSeatEntity(ModEntities.CAMERA_SEAT.get(), level);
                seat.setPos(pos.getX() + 0.5D, pos.getY() + 0.3D, pos.getZ() + 0.5D);
                level.addFreshEntity(seat);
                player.startRiding(seat);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
