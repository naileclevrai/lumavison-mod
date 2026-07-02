package fr.lumavision.block;

import fr.lumavision.entity.CameraSeatEntity;
import fr.lumavision.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A camera boom-arm block. Elevates a camera placed on top; right-click to sit on it as the camera
 * operator — while seated, your look direction drives the camera above (see {@link CameraSeatEntity}).
 */
public class CameraBoomBlock extends Block {

    public CameraBoomBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
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
