package fr.lumavision.entity;

import fr.lumavision.block.CameraBlock;
import fr.lumavision.blockentity.CameraBlockEntity;
import fr.lumavision.network.ModNetworking;
import fr.lumavision.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

/**
 * Invisible "camera operator seat" spawned on a boom arm. While a player rides it, their look
 * direction drives the pan/tilt of the camera directly above the boom — sit down, look around, and
 * the virtual camera follows. Auto-discards when empty.
 */
public class CameraSeatEntity extends Entity {

    public CameraSeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Forge spawn-packet constructor. */
    public CameraSeatEntity(PlayMessages.SpawnEntity packet, Level level) {
        this(ModEntities.CAMERA_SEAT.get(), level);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.35D;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (getPassengers().isEmpty()) {
            discard();
            return;
        }
        if (!(getPassengers().get(0) instanceof Player player)) {
            return;
        }
        // Drive the camera sitting on top of the boom from the operator's look direction.
        BlockPos cameraPos = blockPosition().above();
        BlockEntity be = level().getBlockEntity(cameraPos);
        if (!(be instanceof CameraBlockEntity camera)) {
            return;
        }
        BlockState state = camera.getBlockState();
        float baseYaw = state.hasProperty(CameraBlock.FACING) ? state.getValue(CameraBlock.FACING).toYRot() : 0.0F;
        camera.parameters().setPan(Mth.wrapDegrees(player.getYRot() - baseYaw));
        camera.parameters().setTilt(Mth.clamp(player.getXRot(), -90.0F, 90.0F));
        camera.setChanged();
        if (level() instanceof ServerLevel serverLevel) {
            ModNetworking.sendCameraLiveState(serverLevel, cameraPos, camera.parameters());
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
