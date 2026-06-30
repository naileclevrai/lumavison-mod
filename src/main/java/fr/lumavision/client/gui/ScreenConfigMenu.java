package fr.lumavision.client.gui;

import fr.lumavision.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class ScreenConfigMenu extends AbstractContainerMenu {

    private final BlockPos groupOrigin;

    public ScreenConfigMenu(int containerId, Inventory playerInventory, BlockPos groupOrigin) {
        super(ModMenuTypes.SCREEN_CONFIG.get(), containerId);
        this.groupOrigin = groupOrigin;
    }

    public ScreenConfigMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, extraData.readBlockPos());
    }

    public BlockPos getGroupOrigin() {
        return groupOrigin;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(
                groupOrigin.getX() + 0.5D,
                groupOrigin.getY() + 0.5D,
                groupOrigin.getZ() + 0.5D
        ) <= 64.0D;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
