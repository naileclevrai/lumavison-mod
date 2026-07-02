package fr.lumavision.block;

import fr.lumavision.entity.CameraSeatEntity;
import fr.lumavision.registry.ModEntities;
import fr.lumavision.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A full 3D camera crane that works as a <em>mount</em>: place the crane, then right-click it holding a
 * Camera (or PTZ Camera) to attach a camera to the arm end. Once mounted, the crane renders the camera
 * head on the tip and publishes its NDI feed from there. Right-click with an empty hand to sit in the
 * operator seat and drive it (WASD swing/boom, scroll zoom); sneak-right-click opens the config menu.
 * The whole rig is drawn by {@link fr.lumavision.client.render.CameraCraneRenderer}, so the block model
 * is invisible.
 */
public class CameraCraneBlock extends CameraBlock {

    /** Whether a camera is attached to the arm end. No camera = no head rendered and no NDI feed. */
    public static final BooleanProperty MOUNTED = BooleanProperty.create("mounted");

    public CameraCraneBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(MOUNTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MOUNTED);
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

        ItemStack held = player.getItemInHand(hand);
        boolean cameraInHand = held.is(ModItems.CAMERA.get()) || held.is(ModItems.PTZ_CAMERA.get());
        boolean mounted = state.getValue(MOUNTED);

        if (cameraInHand) {
            // Attach / detach a camera to the arm end.
            if (!level.isClientSide) {
                level.setBlock(pos, state.setValue(MOUNTED, !mounted), Block.UPDATE_ALL);
                player.displayClientMessage(Component.literal(
                        mounted ? "Camera removed from crane" : "Camera mounted on crane arm"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!mounted) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.literal(
                        "Hold a Camera and right-click to mount it on the crane arm"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        // Camera mounted + empty hand → sit in the operator seat.
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
