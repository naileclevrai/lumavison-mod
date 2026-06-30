package fr.lumavision.client.shimmer;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.client.display.DisplayUvMapper;
import fr.lumavision.config.ModConfig;
import fr.lumavision.screen.ScreenDisplaySettings;
import fr.lumavision.screen.ScreenGroupMembership;
import fr.lumavision.video.VideoFrame;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * One Shimmer {@code ColorPointLight} per merged LED wall, driven by average frame color.
 */
@OnlyIn(Dist.CLIENT)
public final class LedAmbilightController {

    private static final LedAmbilightController INSTANCE = new LedAmbilightController();

    private final Map<Long, WallLight> lights = new HashMap<>();

    private LedAmbilightController() {
    }

    public static LedAmbilightController getInstance() {
        return INSTANCE;
    }

    public void onFrameUploaded(
            long groupKey,
            VideoFrame frame,
            ScreenGroupMembership membership,
            ScreenDisplaySettings displaySettings,
            Level level
    ) {
        if (!ModConfig.ENABLE_SHIMMER_AMBILIGHT.get() || !ShimmerBridge.isAvailable()) {
            return;
        }

        int sampleSize = ModConfig.SHIMMER_SAMPLE_SIZE.get();
        float[] contentUv = DisplayUvMapper.visibleContentUv(
                displaySettings,
                frame.getWidth(),
                frame.getHeight(),
                membership.gridWidth(),
                membership.gridHeight()
        );

        int frameHash = FrameAverageSampler.sampleHash(
                frame, sampleSize, sampleSize, contentUv[0], contentUv[1], contentUv[2], contentUv[3]
        );
        long nowMs = System.currentTimeMillis();

        WallLight wallLight = lights.get(groupKey);
        if (wallLight != null && wallLight.light != null && ShimmerBridge.isRemoved(wallLight.light)) {
            lights.remove(groupKey);
            wallLight = null;
        }

        if (wallLight != null) {
            if (frameHash == wallLight.lastFrameHash) {
                return;
            }
            long minIntervalMs = 1000L / Math.max(1, ModConfig.SHIMMER_MAX_UPDATES_PER_SECOND.get());
            if (nowMs - wallLight.lastUpdateMs < minIntervalMs) {
                return;
            }
        }

        int color = FrameAverageSampler.averageArgb(
                frame, sampleSize, sampleSize, contentUv[0], contentUv[1], contentUv[2], contentUv[3]
        );
        WallLightPlacement.Placement placement = WallLightPlacement.compute(
                level,
                membership,
                ModConfig.SHIMMER_LIGHT_RADIUS.get().floatValue(),
                ModConfig.SHIMMER_LIGHT_OFFSET.get().floatValue()
        );

        if (wallLight == null || wallLight.light == null) {
            Object light = ShimmerBridge.addLight(placement.position(), color, placement.radius());
            if (light == null) {
                return;
            }
            wallLight = new WallLight(light, frameHash, nowMs, placement.position(), placement.radius());
            lights.put(groupKey, wallLight);
        }

        if (!wallLight.position.equals(placement.position())) {
            ShimmerBridge.setLightPos(wallLight.light, placement.position());
            wallLight.position = placement.position();
        }
        if (wallLight.radius != placement.radius()) {
            ShimmerBridge.setLightRadius(wallLight.light, placement.radius());
            wallLight.radius = placement.radius();
        }

        ShimmerBridge.setLightColor(wallLight.light, color);
        ShimmerBridge.setEnable(wallLight.light, true);
        ShimmerBridge.updateLight(wallLight.light);

        wallLight.lastFrameHash = frameHash;
        wallLight.lastUpdateMs = nowMs;

        if (ModConfig.DEBUG_LOGGING.get()) {
            LumaVisionMod.LOGGER.debug(
                    "Ambilight wall {} color #{} hash {}",
                    groupKey,
                    String.format("%06X", color & 0xFFFFFF),
                    frameHash
            );
        }
    }

    public void removeLight(long groupKey) {
        WallLight wallLight = lights.remove(groupKey);
        if (wallLight != null && wallLight.light != null) {
            ShimmerBridge.removeLight(wallLight.light);
        }
    }

    public void clear() {
        for (WallLight wallLight : lights.values()) {
            if (wallLight.light != null) {
                ShimmerBridge.removeLight(wallLight.light);
            }
        }
        lights.clear();
    }

    public void pruneExcept(java.util.Set<Long> activeKeys) {
        Iterator<Map.Entry<Long, WallLight>> iterator = lights.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, WallLight> entry = iterator.next();
            if (!activeKeys.contains(entry.getKey())) {
                if (entry.getValue().light != null) {
                    ShimmerBridge.removeLight(entry.getValue().light);
                }
                iterator.remove();
            }
        }
    }

    private static final class WallLight {
        private final Object light;
        private int lastFrameHash;
        private long lastUpdateMs;
        private Vector3f position;
        private float radius;

        private WallLight(Object light, int lastFrameHash, long lastUpdateMs, Vector3f position, float radius) {
            this.light = light;
            this.lastFrameHash = lastFrameHash;
            this.lastUpdateMs = lastUpdateMs;
            this.position = position;
            this.radius = radius;
        }
    }
}
