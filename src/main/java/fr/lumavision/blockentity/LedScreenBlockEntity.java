package fr.lumavision.blockentity;

import fr.lumavision.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Server-authoritative state for a single LED screen block.
 * <p>
 * Pixel content is produced client-side via {@link fr.lumavision.video.VideoSource};
 * this entity will later hold source configuration (URL, NDI name, crop, etc.).
 */
public class LedScreenBlockEntity extends BlockEntity {

    /** Reserved for future source binding (file path, NDI id, URL, …). */
    private String sourceId = "";

    public LedScreenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LED_SCREEN.get(), pos, state);
    }

    public Direction getFacing() {
        return getBlockState().getValue(fr.lumavision.block.LedScreenBlock.FACING);
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId == null ? "" : sourceId;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("SourceId", sourceId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        sourceId = tag.getString("SourceId");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
