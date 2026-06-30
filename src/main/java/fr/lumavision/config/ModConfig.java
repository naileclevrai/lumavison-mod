package fr.lumavision.config;

import fr.lumavision.LumaVisionMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Common mod configuration ({@code lumavision-common.toml}).
 */
@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModConfig {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Enables verbose logging (textures, networking, rendering, NDI discovery).")
            .define("debugLogging", false);

    public static final ForgeConfigSpec.IntValue MAX_TEXTURE_RESOLUTION = BUILDER
            .comment("Maximum resolution (longest side) for LED screen dynamic textures.")
            .defineInRange("maxTextureResolution", 1024, 64, 4096);

    public static final ForgeConfigSpec.BooleanValue ENABLE_NDI = BUILDER
            .comment("Enables NDI input via Devolay on the client.")
            .define("enableNdi", true);

    public static final ForgeConfigSpec.ConfigValue<String> NDI_DEFAULT_SOURCE = BUILDER
            .comment("Default NDI source name for walls without an explicit sourceId (exact name from discovery).")
            .define("ndiDefaultSource", "");

    public static final ForgeConfigSpec.BooleanValue NDI_AUTO_SELECT_FIRST = BUILDER
            .comment("When no wall or default source is set, use the first discovered NDI source.")
            .define("ndiAutoSelectFirst", false);

    public static final ForgeConfigSpec.IntValue NDI_RECEIVE_TIMEOUT_MS = BUILDER
            .comment("Per-frame NDI receive timeout in milliseconds.")
            .defineInRange("ndiReceiveTimeoutMs", 5, 1, 1000);

    public static final ForgeConfigSpec.IntValue NDI_DISCOVERY_INTERVAL_MS = BUILDER
            .comment("How often to refresh the list of NDI sources on the network.")
            .defineInRange("ndiDiscoveryIntervalMs", 2000, 250, 60000);

    public static final ForgeConfigSpec.BooleanValue ENABLE_SHIMMER_AMBILIGHT = BUILDER
            .comment("Emit dynamic Shimmer lights from LED screen content when the Shimmer mod is installed.")
            .define("enableShimmerAmbilight", true);

    public static final ForgeConfigSpec.IntValue SHIMMER_SAMPLE_SIZE = BUILDER
            .comment("Grid size for average color sampling (e.g. 16 = 16x16 samples).")
            .defineInRange("shimmerSampleSize", 16, 4, 32);

    public static final ForgeConfigSpec.IntValue SHIMMER_MAX_UPDATES_PER_SECOND = BUILDER
            .comment("Maximum Shimmer light color updates per second per wall.")
            .defineInRange("shimmerMaxUpdatesPerSecond", 30, 5, 60);

    public static final ForgeConfigSpec.DoubleValue SHIMMER_LIGHT_RADIUS = BUILDER
            .comment("Light radius multiplier based on wall size (max grid dimension).")
            .defineInRange("shimmerLightRadius", 1.5, 0.5, 8.0);

    public static final ForgeConfigSpec.DoubleValue SHIMMER_LIGHT_OFFSET = BUILDER
            .comment("Distance in blocks in front of the screen face to place the light.")
            .defineInRange("shimmerLightOffset", 0.3, 0.0, 2.0);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ModConfig() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            LumaVisionMod.LOGGER.info(
                    "LumaVision config loaded (debug={}, maxRes={}, ndi={}, ndiSource='{}')",
                    DEBUG_LOGGING.get(),
                    MAX_TEXTURE_RESOLUTION.get(),
                    ENABLE_NDI.get(),
                    NDI_DEFAULT_SOURCE.get()
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
