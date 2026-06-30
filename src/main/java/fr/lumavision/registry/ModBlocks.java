package fr.lumavision.registry;

import fr.lumavision.LumaVisionMod;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Central registry for LumaVision blocks (LED screens, controllers, etc.).
 * <p>
 * Add new blocks via {@code BLOCKS.register("name", () -> new MyBlock(...))}.
 */
public final class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, LumaVisionMod.MOD_ID);

    private ModBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
