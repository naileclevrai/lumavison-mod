package fr.lumavision.registry;

import fr.lumavision.LumaVisionMod;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Registre central des blocs LumaVision (écrans LED, contrôleurs, etc.).
 * <p>
 * Ajoutez de nouveaux blocs via {@code BLOCKS.register("nom", () -> new MonBloc(...))}.
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
