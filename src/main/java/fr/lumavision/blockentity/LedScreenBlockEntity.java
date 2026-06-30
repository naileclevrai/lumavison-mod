package fr.lumavision.blockentity;

import fr.lumavision.registry.ModBlockEntities;
import fr.lumavision.screen.ScreenDisplaySettings;
import fr.lumavision.screen.ScreenGroupMembership;
import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceDescriptors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Server-authoritative state for a single LED screen block.
 * <p>
 * Pixel content is produced client-side via {@link fr.lumavision.video.VideoSource};
 * group membership defines which portion of the shared wall texture this block displays.
 * Source binding, display settings, and ownership are authoritative on the group origin.
 */
public class LedScreenBlockEntity extends BlockEntity {

    private String sourceId = "";
    private ScreenGroupMembership groupMembership;
    private ScreenDisplaySettings displaySettings = ScreenDisplaySettings.DEFAULT;
    @Nullable
    private UUID ownerUuid;

    public LedScreenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LED_SCREEN.get(), pos, state);
        this.groupMembership = ScreenGroupMembership.solo(pos);
    }

    public Direction getFacing() {
        return getBlockState().getValue(fr.lumavision.block.LedScreenBlock.FACING);
    }

    public ScreenGroupMembership getGroupMembership() {
        return groupMembership;
    }

    public void setGroupMembership(ScreenGroupMembership membership) {
        this.groupMembership = membership;
        setChanged();
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId == null ? "" : sourceId;
        setChanged();
    }

    public ScreenDisplaySettings getDisplaySettings() {
        return displaySettings;
    }

    public void setDisplaySettings(ScreenDisplaySettings settings) {
        this.displaySettings = settings == null ? ScreenDisplaySettings.DEFAULT : settings;
        setChanged();
    }

    @Nullable
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(@Nullable UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        setChanged();
    }

    public void claimOwnership(UUID playerUuid) {
        if (ownerUuid == null) {
            ownerUuid = playerUuid;
            setChanged();
        }
    }

    public boolean isGroupOrigin() {
        return worldPosition.equals(groupMembership.groupOrigin());
    }

    /**
     * Display settings for this wall — read from the group origin block entity.
     */
    public static ScreenDisplaySettings resolveDisplaySettings(Level level, ScreenGroupMembership membership) {
        BlockEntity blockEntity = level.getBlockEntity(membership.groupOrigin());
        if (blockEntity instanceof LedScreenBlockEntity origin) {
            return origin.getDisplaySettings();
        }
        return ScreenDisplaySettings.DEFAULT;
    }

    /**
     * Parsed media binding for this block. Only the group origin's source drives a merged wall.
     */
    public VideoSourceDescriptor getSourceDescriptor() {
        return VideoSourceDescriptors.parse(sourceId);
    }

    public boolean hasExplicitSourceId() {
        return VideoSourceDescriptors.hasExplicitSource(sourceId);
    }

    public BlockPos getGroupOrigin() {
        return groupMembership.groupOrigin();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("SourceId", sourceId);
        groupMembership.write(tag);
        displaySettings.write(tag);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        sourceId = tag.getString("SourceId");
        groupMembership = ScreenGroupMembership.read(tag, worldPosition);
        displaySettings = ScreenDisplaySettings.read(tag);
        ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
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
