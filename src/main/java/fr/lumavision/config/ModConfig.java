package fr.lumavision.config;

import fr.lumavision.LumaVisionMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Configuration commune du mod (fichier {@code lumavision-common.toml}).
 * <p>
 * Les valeurs ci-dessous préparent les fonctionnalités futures sans les activer.
 */
@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Active les logs détaillés (textures, réseau, rendu).")
            .define("debugLogging", false);

    public static final ForgeConfigSpec.IntValue MAX_TEXTURE_RESOLUTION = BUILDER
            .comment("Résolution maximale (côté le plus long) des textures dynamiques des écrans LED.")
            .defineInRange("maxTextureResolution", 1024, 64, 4096);

    public static final ForgeConfigSpec.BooleanValue ENABLE_NDI = BUILDER
            .comment("Active le support NDI (Devolay). Non implémenté pour l'instant.")
            .define("enableNdi", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ModConfig() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            LumaVisionMod.LOGGER.info(
                    "Configuration LumaVision chargée (debug={}, maxRes={}, ndi={})",
                    DEBUG_LOGGING.get(),
                    MAX_TEXTURE_RESOLUTION.get(),
                    ENABLE_NDI.get()
            );
        }
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            LumaVisionMod.LOGGER.info("Configuration LumaVision rechargée");
        }
    }
}
