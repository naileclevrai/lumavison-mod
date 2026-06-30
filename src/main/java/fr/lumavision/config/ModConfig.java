package fr.lumavision.config;

import fr.lumavision.LumaVisionMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Common mod configuration ({@code lumavision-common.toml}).
 * <p>
 * The values below prepare upcoming features without enabling them yet.
 */
@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Enables verbose logging (textures, networking, rendering).")
            .define("debugLogging", false);

    public static final ForgeConfigSpec.IntValue MAX_TEXTURE_RESOLUTION = BUILDER
            .comment("Maximum resolution (longest side) for LED screen dynamic textures.")
            .defineInRange("maxTextureResolution", 1024, 64, 4096);

    public static final ForgeConfigSpec.BooleanValue ENABLE_NDI = BUILDER
            .comment("Enables NDI support (Devolay). Not implemented yet.")
            .define("enableNdi", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ModConfig() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            LumaVisionMod.LOGGER.info(
                    "LumaVision config loaded (debug={}, maxRes={}, ndi={})",
                    DEBUG_LOGGING.get(),
                    MAX_TEXTURE_RESOLUTION.get(),
                    ENABLE_NDI.get()
            );
        }
    }

    @SubscribeEvent
    static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            LumaVisionMod.LOGGER.info("LumaVision config reloaded");
        }
    }
}
