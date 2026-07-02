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

    public static final ForgeConfigSpec.IntValue MIN_TEXTURE_RESOLUTION = BUILDER
            .comment("Minimum resolution (longest side) for LED screen dynamic textures. Raises quality on small walls.")
            .defineInRange("minTextureResolution", 768, 64, 4096);

    public static final ForgeConfigSpec.IntValue BASE_CELL_RESOLUTION = BUILDER
            .comment("Pixels per LED block before the maxTextureResolution cap. Lower = better FPS, softer image.")
            .defineInRange("baseCellResolution", 96, 32, 256);

    public static final ForgeConfigSpec.IntValue MAX_TEXTURE_UPDATES_PER_SECOND = BUILDER
            .comment("Maximum GPU texture uploads per second per LED wall (0 = unlimited). Lower values reduce CPU/GPU load near screens.")
            .defineInRange("maxTextureUpdatesPerSecond", 20, 0, 60);

    public static final ForgeConfigSpec.IntValue MAX_NDI_CAPTURE_FRAMES_PER_SECOND = BUILDER
            .comment("Maximum NDI frames converted per second per active wall (0 = unlimited).")
            .defineInRange("maxNdiCaptureFramesPerSecond", 30, 0, 60);

    public static final ForgeConfigSpec.BooleanValue ENABLE_DYNAMIC_LOD = BUILDER
            .comment("Automatically lowers video texture resolution for distant LED walls.")
            .define("enableDynamicLod", true);

    public static final ForgeConfigSpec.IntValue MID_TEXTURE_RESOLUTION = BUILDER
            .comment("Minimum longest-side texture resolution for medium-distance LED walls.")
            .defineInRange("midTextureResolution", 512, 64, 4096);

    public static final ForgeConfigSpec.IntValue FAR_TEXTURE_RESOLUTION = BUILDER
            .comment("Minimum longest-side texture resolution for far LED walls.")
            .defineInRange("farTextureResolution", 256, 64, 4096);

    public static final ForgeConfigSpec.IntValue LOD_NEAR_DISTANCE = BUILDER
            .comment("Distance in blocks where LED walls use the close/high quality resolution.")
            .defineInRange("lodNearDistance", 24, 1, 512);

    public static final ForgeConfigSpec.IntValue LOD_MID_DISTANCE = BUILDER
            .comment("Distance in blocks where LED walls switch from medium to far quality.")
            .defineInRange("lodMidDistance", 48, 1, 512);

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
            .defineInRange("cameraDefaultFps", 20, 1, 60);

    public static final ForgeConfigSpec.IntValue CAMERA_MAX_CAPTURE_FPS = BUILDER
            .comment("Hard cap on offscreen world captures per second per camera (lower = better game FPS).")
            .defineInRange("cameraMaxCaptureFps", 15, 1, 60);

    public static final ForgeConfigSpec.IntValue CAMERA_CAPTURE_VIEW_DISTANCE = BUILDER
            .comment("Chunk render distance used for camera offscreen capture (lower = much better game FPS).")
            .defineInRange("cameraCaptureViewDistance", 6, 2, 32);

    public static final ForgeConfigSpec.IntValue CAMERA_DEFAULT_RESOLUTION = BUILDER
            .comment("Default longest side (pixels) for a newly placed camera's offscreen render target.")
            .defineInRange("cameraDefaultResolution", 960, 64, 3840);

    public static final ForgeConfigSpec.BooleanValue CAMERA_USE_CONFIGURED_RESOLUTION = BUILDER
            .comment("Render at the camera's configured output resolution instead of the game window size (much better FPS). "
                    + "If post-processing mods (Shimmer) misalign blocks, set this to false.")
            .define("cameraUseConfiguredResolution", true);

    public static final ForgeConfigSpec.IntValue CAMERA_RENDER_SCALE = BUILDER
            .comment("Extra scale applied to the camera render resolution, in percent (50 = half pixels). "
                    + "Applied after cameraUseConfiguredResolution / window sizing.")
            .defineInRange("cameraRenderScale", 100, 25, 100);

    public static final ForgeConfigSpec.BooleanValue CAMERA_RENDER_WORLD = BUILDER
            .comment("Render the actual in-game view for camera NDI output. If false (or on failure/Fabulous graphics), a test pattern is sent instead.")
            .define("cameraRenderWorld", true);

    public static final ForgeConfigSpec.BooleanValue CAMERA_SKIP_WHEN_LAGGING = BUILDER
            .comment("Skip a camera capture on frames where the game is already running slow, to avoid a lag spiral.")
            .define("cameraSkipWhenLagging", true);

    public static final ForgeConfigSpec.BooleanValue CAMERA_SKIP_STATIC_FRAMES = BUILDER
            .comment("Reuse the last captured frame when the camera view has not changed (big FPS gain for fixed shots, but moving entities won't update in the feed).")
            .define("cameraSkipStaticFrames", false);

    public static final ForgeConfigSpec.IntValue CAMERA_LAG_FRAME_MS = BUILDER
            .comment("Frame time (ms) above which camera captures are skipped when cameraSkipWhenLagging is on.")
            .defineInRange("cameraLagFrameMs", 50, 10, 500);

    // --- Multiplayer media relay -----------------------------------------

    public static final ForgeConfigSpec.BooleanValue ENABLE_MULTIPLAYER_RELAY = BUILDER
            .comment("When multiple players are online, one client captures NDI/local sources and the server relays frames to others (server NDI bridge).")
            .define("enableMultiplayerRelay", true);

    public static final ForgeConfigSpec.IntValue RELAY_MAX_FRAME_RESOLUTION = BUILDER
            .comment("Longest side of relayed frames sent over the network (lower = less bandwidth).")
            .defineInRange("relayMaxFrameResolution", 512, 64, 1024);

    public static final ForgeConfigSpec.IntValue RELAY_MAX_FPS = BUILDER
            .comment("Maximum relay frame uploads per second per wall from the capture client (0 = unlimited).")
            .defineInRange("relayMaxFps", 15, 0, 60);

    public static final ForgeConfigSpec.IntValue RELAY_PLAYER_RANGE = BUILDER
            .comment("Maximum distance (blocks) for a player to participate in wall/camera relay.")
            .defineInRange("relayPlayerRange", 96, 16, 256);

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
