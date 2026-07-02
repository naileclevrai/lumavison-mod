package fr.lumavision.blockentity;

import fr.lumavision.camera.CameraParameters;
import fr.lumavision.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Server-authoritative state for a virtual NDI camera block: holds the {@link CameraParameters},
 * persists them to NBT, and syncs them to nearby clients (which render the offscreen view and emit
 * the NDI feed). The Art-Net apply loop (M3) writes live pan/tilt/zoom/track here on the server tick;
 * the client capture scheduler (M2) reads the synced copy.
 */
public class CameraBlockEntity extends BlockEntity {

    private final CameraParameters parameters = new CameraParameters();

    /** Client-side frame counter used to pace capture to the camera's configured FPS (M2). */
    private long clientTickCounter;

    public CameraBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAMERA.get(), pos, state);
        if (parameters.ndiSourceName().isEmpty()) {
            parameters.setNdiSourceName(defaultSourceName(pos));
        }
    }

    public static String defaultSourceName(BlockPos pos) {
        return "Minecraft " + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public CameraParameters parameters() {
        return parameters;
    }

    public long clientTickCounter() {
        return clientTickCounter;
    }

    /** Mutate {@link #parameters()} then call this (server-side) to persist + resync to clients. */
    public void onParametersChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CameraBlockEntity be) {
        be.clientTickCounter++;
        // Client-only: register/refresh this camera's NDI sender. Runs only on the client because
        // getTicker wires clientTick solely when level.isClientSide, so the client-only manager
        // class is never loaded on a dedicated server.
        fr.lumavision.client.ndi.CameraNdiManager.getInstance().onCameraClientTick(be);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CameraBlockEntity be) {
        // Art-Net apply loop hooks in here in M3.
    }

    // --- persistence -------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        parameters.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        parameters.load(tag);
        if (parameters.ndiSourceName().isEmpty()) {
            parameters.setNdiSourceName(defaultSourceName(worldPosition));
        }
    }

    // --- client sync -------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        parameters.save(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        parameters.load(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            parameters.load(tag);
        }
    }
}
