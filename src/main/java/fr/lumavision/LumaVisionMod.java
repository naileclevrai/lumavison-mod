package fr.lumavision;

import fr.lumavision.config.ModConfig;
import fr.lumavision.registry.ModBlockEntities;
import fr.lumavision.registry.ModBlocks;
import fr.lumavision.registry.ModCreativeTabs;
import fr.lumavision.registry.ModEntities;
import fr.lumavision.registry.ModItems;
import fr.lumavision.registry.ModMenuTypes;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main entry point for the LumaVision mod.
 * <p>
 * This mod is dedicated to LED screens and video content playback in Minecraft.
 * This version contains only the foundations: registries, configuration, and network hooks.
 */
@Mod(LumaVisionMod.MOD_ID)
public final class LumaVisionMod {

    public static final String MOD_ID = "lumavision";

    public static final Logger LOGGER = LogManager.getLogger();

    public LumaVisionMod() {
        LOGGER.info("Initializing LumaVision");

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModEntities.register(modBus);
        ModMenuTypes.register(modBus);
        ModCreativeTabs.register(modBus);

        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.SPEC);
    }
}
