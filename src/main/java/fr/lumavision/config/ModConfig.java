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

    public static final ForgeConfigSpec.IntValue BASE_CELL_RESOLUTION = BUILDER
            .comment("Pixels per LED block before the maxTextureResolution cap. Lower = better FPS, softer image.")
            .defineInRange("baseCellResolution", 96, 32, 256);

    public static final ForgeConfigSpec.IntValue MAX_TEXTURE_UPDATES_PER_SECOND = BUILDER
            .comment("Maximum GPU texture uploads per second per LED wall (0 = unlimited). Lower values reduce CPU/GPU load near screens.")
            .defineInRange("maxTextureUpdatesPerSecond", 20, 0, 60);

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

    // --- Virtual camera / NDI output -------------------------------------

    public static final ForgeConfigSpec.BooleanValue NDI_ENABLE_OUTPUT = BUILDER
            .comment("Enables publishing camera blocks as NDI output sources (client-side render + send).")
            .define("ndiEnableOutput", true);

    public static final ForgeConfigSpec.IntValue CAMERA_MAX_ACTIVE = BUILDER
            .comment("Maximum number of camera blocks rendered + streamed at once on a client (performance cap).")
            .defineInRange("cameraMaxActive", 4, 1, 32);

    public static final ForgeConfigSpec.IntValue CAMERA_MAX_RESOLUTION = BUILDER
            .comment("Maximum resolution (longest side) for a camera's offscreen render target.")
            .defineInRange("cameraMaxResolution", 1920, 64, 3840);

    public static final ForgeConfigSpec.IntValue CAMERA_DEFAULT_FPS = BUILDER
            .comment("Default capture/output frame rate for a newly placed camera.")
            .defineInRange("cameraDefaultFps", 30, 1, 60);

    public static final ForgeConfigSpec.BooleanValue CAMERA_RENDER_WORLD = BUILDER
            .comment("Render the actual in-game view for camera NDI output. If false (or on failure/Fabulous graphics), a test pattern is sent instead.")
            .define("cameraRenderWorld", true);

    // --- Art-Net / DMX ---------------------------------------------------

    public static final ForgeConfigSpec.BooleanValue ARTNET_ENABLE = BUILDER
            .comment("Enables the server-side Art-Net listener that drives camera pan/tilt/zoom/track over DMX.")
            .define("artNetEnable", false);

    public static final ForgeConfigSpec.ConfigValue<String> ARTNET_BIND_ADDRESS = BUILDER
            .comment("Local interface address the Art-Net listener binds to (0.0.0.0 = all interfaces).")
            .define("artNetBindAddress", "0.0.0.0");

    public static final ForgeConfigSpec.IntValue ARTNET_PORT = BUILDER
            .comment("UDP port for the Art-Net listener (standard Art-Net is 6454).")
            .defineInRange("artNetPort", 6454, 1, 65535);

    public static final ForgeConfigSpec.IntValue ARTNET_POLL_RATE_HZ = BUILDER
            .comment("Max rate at which DMX changes are applied to cameras and synced to clients.")
            .defineInRange("artNetPollRateHz", 20, 1, 60);

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
