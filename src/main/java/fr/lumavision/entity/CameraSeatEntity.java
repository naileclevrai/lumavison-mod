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
        // The operator drives the camera via client input (CameraRigInputPacket); the seat just holds
        // the player and cleans itself up when they leave.
        if (!level().isClientSide && getPassengers().isEmpty()) {
            discard();
        }
    }

    /**
     * Finds the camera this seat operates: the nearest camera block entity within a radius of the seat.
     * Returns null if none is nearby. (Called once when mounting, then cached.)
     */
    public static BlockPos findControlledCamera(Level level, BlockPos origin) {
        int radius = 12;
        BlockPos best = null;
        int bestDistSq = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (level.getBlockEntity(cursor) instanceof CameraBlockEntity) {
                        int distSq = dx * dx + dy * dy + dz * dz;
                        if (distSq < bestDistSq) {
                            bestDistSq = distSq;
                            best = cursor.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
