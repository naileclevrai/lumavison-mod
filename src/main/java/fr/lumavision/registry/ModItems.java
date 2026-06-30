package fr.lumavision.registry;

import fr.lumavision.LumaVisionMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Central registry for LumaVision items.
 * <p>
 * Placeable blocks typically use {@link net.minecraft.world.item.BlockItem}.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LumaVisionMod.MOD_ID);

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
