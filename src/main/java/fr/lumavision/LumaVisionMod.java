package fr.lumavision;

import fr.lumavision.config.ModConfig;
import fr.lumavision.registry.ModBlockEntities;
import fr.lumavision.registry.ModBlocks;
import fr.lumavision.registry.ModCreativeTabs;
import fr.lumavision.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Point d'entrée principal du mod LumaVision.
 * <p>
 * Ce mod est dédié aux écrans LED et à la diffusion de contenu vidéo dans Minecraft.
 * Cette version ne contient que les fondations : enregistrements, configuration et hooks réseau.
 */
@Mod(LumaVisionMod.MOD_ID)
public final class LumaVisionMod {

    public static final String MOD_ID = "lumavision";

    public static final Logger LOGGER = LogManager.getLogger();

    public LumaVisionMod() {
        LOGGER.info("Initialisation de LumaVision");

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntities.register(modBus);
        ModCreativeTabs.register(modBus);

        ModLoadingContext.get().registerConfig(Type.COMMON, ModConfig.SPEC);
    }

}
