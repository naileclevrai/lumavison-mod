package fr.lumavision.menu;

import fr.lumavision.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Container menu for configuring a camera block. Carries only the camera's {@link BlockPos};
 * the client screen reads the live {@link fr.lumavision.camera.CameraParameters} straight from the
 * synced block entity and sends edits back via {@link fr.lumavision.network.ConfigureCameraPacket}.
 */
public final class CameraConfigMenu extends AbstractContainerMenu {

    private final BlockPos cameraPos;

    public CameraConfigMenu(int containerId, Inventory playerInventory, BlockPos cameraPos) {
        super(ModMenuTypes.CAMERA_CONFIG.get(), containerId);
        this.cameraPos = cameraPos;
    }

    public CameraConfigMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    public BlockPos getCameraPos() {
        return cameraPos;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                cameraPos.getX() + 0.5D,
                cameraPos.getY() + 0.5D,
                cameraPos.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
