package fr.lumavision.client.texture;

import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.client.display.DisplayColorGrading;
import fr.lumavision.client.video.catalog.ClientVideoSourceCatalog;
import fr.lumavision.config.ModConfig;
import fr.lumavision.screen.ScreenDisplaySettings;
import fr.lumavision.screen.ScreenGroupMembership;
import fr.lumavision.video.VideoFrame;
import fr.lumavision.video.VideoSource;
import fr.lumavision.video.VideoSourceDescriptor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * One {@link VideoSource} pipeline per merged screen group (wall).
 * <p>
 * GPU uploads happen only during the client tick — never from block entity rendering.
 * Color grading runs in the display layer after {@link VideoSource#getCurrentFrame()}.
 */
@OnlyIn(Dist.CLIENT)
public final class ScreenTextureManager {

    private static final int BASE_CELL_RESOLUTION = 128;
    private static final int[] FALLBACK_FRAME_SIZE = {16, 16};

    private static final ScreenTextureManager INSTANCE = new ScreenTextureManager();

    private final Map<Long, ScreenPipeline> pipelines = new HashMap<>();
    private final Set<BlockPos> pendingOrigins = new HashSet<>();
    private DynamicTextureHandle fallbackTexture;

    private ScreenTextureManager() {
    }

    public static ScreenTextureManager getInstance() {
        return INSTANCE;
    }

    public ResourceLocation getTexture(LedScreenBlockEntity blockEntity) {
        ScreenGroupMembership membership = blockEntity.getGroupMembership();
        ScreenPipeline pipeline = pipelines.get(membership.groupKey());
        if (pipeline != null) {
            return pipeline.texture().location();
        }

        pendingOrigins.add(membership.groupOrigin());
        return fallbackTexture().location();
    }

    public int[] getFrameSize(long groupKey) {
        ScreenPipeline pipeline = pipelines.get(groupKey);
        return pipeline == null ? FALLBACK_FRAME_SIZE : pipeline.frameSize();
    }

    public void tick(Level level) {
        processPendingPipelines(level);
        pruneInvalid(level);
        for (ScreenPipeline pipeline : pipelines.values()) {
            pipeline.tick(level);
        }
    }

    public void clear() {
        for (ScreenPipeline pipeline : pipelines.values()) {
            pipeline.close();
        }
        pipelines.clear();
        pendingOrigins.clear();
        if (fallbackTexture != null) {
            fallbackTexture.close();
            fallbackTexture = null;
        }
    }

    private void processPendingPipelines(Level level) {
        if (pendingOrigins.isEmpty()) {
            return;
        }

        Set<BlockPos> origins = Set.copyOf(pendingOrigins);
        pendingOrigins.clear();

        for (BlockPos origin : origins) {
            BlockEntity blockEntity = level.getBlockEntity(origin);
            if (blockEntity instanceof LedScreenBlockEntity led) {
                ensurePipeline(level, led.getGroupMembership());
            }
        }
    }

    private void ensurePipeline(Level level, ScreenGroupMembership membership) {
        long key = membership.groupKey();
        VideoSourceDescriptor descriptor = resolveDescriptor(level, membership);
        ScreenDisplaySettings displaySettings = LedScreenBlockEntity.resolveDisplaySettings(level, membership);

        ScreenPipeline existing = pipelines.get(key);
        if (existing != null && existing.matches(membership, descriptor, displaySettings)) {
            return;
        }
        if (existing != null) {
            existing.close();
        }

        ScreenPipeline pipeline = createPipeline(membership, descriptor);
        pipelines.put(key, pipeline);
        pipeline.tick(level);
    }

    private static VideoSourceDescriptor resolveDescriptor(Level level, ScreenGroupMembership membership) {
        BlockEntity blockEntity = level.getBlockEntity(membership.groupOrigin());
        if (blockEntity instanceof LedScreenBlockEntity origin) {
            return ClientVideoSourceCatalog.INSTANCE.resolve(origin);
        }
        return VideoSourceDescriptor.testPattern();
    }

    private static ScreenPipeline createPipeline(ScreenGroupMembership membership, VideoSourceDescriptor descriptor) {
        int[] size = computeTextureSize(membership.gridWidth(), membership.gridHeight());
        VideoSource source = ClientVideoSourceCatalog.INSTANCE.create(descriptor, size[0], size[1]);
        DynamicTextureHandle texture = new DynamicTextureHandle("group_" + membership.groupKey());
        return new ScreenPipeline(membership, descriptor, source, texture);
    }

    static int[] computeTextureSize(int gridWidth, int gridHeight) {
        int width = BASE_CELL_RESOLUTION * gridWidth;
        int height = BASE_CELL_RESOLUTION * gridHeight;
        int maxResolution = ModConfig.MAX_TEXTURE_RESOLUTION.get();
        int longest = Math.max(width, height);

        if (longest > maxResolution) {
            float scale = maxResolution / (float) longest;
            width = Math.max(64, Math.round(width * scale));
            height = Math.max(64, Math.round(height * scale));
        }

        return new int[]{width, height};
    }

    private DynamicTextureHandle fallbackTexture() {
        if (fallbackTexture == null) {
            fallbackTexture = new DynamicTextureHandle("fallback");
        }
        return fallbackTexture;
    }

    private void pruneInvalid(Level level) {
        Iterator<Map.Entry<Long, ScreenPipeline>> iterator = pipelines.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ScreenPipeline> entry = iterator.next();
            BlockPos origin = BlockPos.of(entry.getKey());
            BlockEntity blockEntity = level.getBlockEntity(origin);

            if (!(blockEntity instanceof LedScreenBlockEntity led)) {
                entry.getValue().close();
                iterator.remove();
                continue;
            }

            ScreenGroupMembership membership = led.getGroupMembership();
            VideoSourceDescriptor descriptor = resolveDescriptor(level, membership);
            ScreenDisplaySettings displaySettings = LedScreenBlockEntity.resolveDisplaySettings(level, membership);
            if (!membership.groupOrigin().equals(origin) || !entry.getValue().matches(membership, descriptor, displaySettings)) {
                entry.getValue().close();
                iterator.remove();
            }
        }
    }

    private static final class ScreenPipeline implements AutoCloseable {
        private final ScreenGroupMembership membership;
        private final VideoSourceDescriptor descriptor;
        private final VideoSource source;
        private final DynamicTextureHandle texture;
        private VideoFrame gradedFrame;

        private int lastFrameWidth;
        private int lastFrameHeight;
        private String displayCacheKey = ScreenDisplaySettings.DEFAULT.cacheKey();

        private ScreenPipeline(ScreenGroupMembership membership, VideoSourceDescriptor descriptor,
                               VideoSource source, DynamicTextureHandle texture) {
            this.membership = membership;
            this.descriptor = descriptor;
            this.source = source;
            this.texture = texture;
            this.gradedFrame = new VideoFrame(source.getWidth(), source.getHeight());
        }

        private boolean matches(ScreenGroupMembership other, VideoSourceDescriptor otherDescriptor,
                                ScreenDisplaySettings displaySettings) {
            return membership.gridWidth() == other.gridWidth()
                    && membership.gridHeight() == other.gridHeight()
                    && membership.groupOrigin().equals(other.groupOrigin())
                    && descriptor.cacheKey().equals(otherDescriptor.cacheKey())
                    && displayCacheKey.equals(displaySettings.cacheKey());
        }

        private DynamicTextureHandle texture() {
            return texture;
        }

        private int[] frameSize() {
            return new int[]{
                    lastFrameWidth > 0 ? lastFrameWidth : source.getWidth(),
                    lastFrameHeight > 0 ? lastFrameHeight : source.getHeight()
            };
        }

        private void tick(Level level) {
            ScreenDisplaySettings displaySettings = LedScreenBlockEntity.resolveDisplaySettings(level, membership);
            displayCacheKey = displaySettings.cacheKey();

            source.tick();
            VideoFrame frame = source.getCurrentFrame();
            lastFrameWidth = frame.getWidth();
            lastFrameHeight = frame.getHeight();

            if (frame.getWidth() != source.getWidth() || frame.getHeight() != source.getHeight()) {
                return;
            }

            if (displaySettings.needsColorGrading()) {
                ensureGradedFrameSize(frame.getWidth(), frame.getHeight());
                DisplayColorGrading.applyInto(frame, gradedFrame, displaySettings);
                texture.upload(gradedFrame);
            } else {
                texture.upload(frame);
            }
        }

        private void ensureGradedFrameSize(int width, int height) {
            if (gradedFrame != null && gradedFrame.getWidth() == width && gradedFrame.getHeight() == height) {
                return;
            }
            gradedFrame = new VideoFrame(width, height);
        }

        @Override
        public void close() {
            source.dispose();
            texture.close();
        }
    }
}
