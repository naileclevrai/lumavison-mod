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
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
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

    private static final int[] FALLBACK_FRAME_SIZE = {16, 16};
    /** Pipelines beyond this distance skip source tick and GPU upload. */
    private static final int MAX_PIPELINE_TICK_DISTANCE = 96;
    private static final int FRAME_HASH_SAMPLE_SIZE = 8;
    private static final int PRUNE_INTERVAL_TICKS = 40;

    private static final ScreenTextureManager INSTANCE = new ScreenTextureManager();

    private enum QualityTier {
        NEAR,
        MID,
        FAR
    }

    private final Map<Long, ScreenPipeline> pipelines = new HashMap<>();
    private final Set<BlockPos> pendingOrigins = new HashSet<>();
    private DynamicTextureHandle fallbackTexture;
    private int pruneTickCounter;

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

    public WallRenderContext getWallRenderContext(long groupKey) {
        ScreenPipeline pipeline = pipelines.get(groupKey);
        return pipeline == null ? null : pipeline.renderContext();
    }

    public record WallRenderContext(
            ScreenDisplaySettings settings,
            int frameWidth,
            int frameHeight,
            int[] vertexColor
    ) {
    }

    public void tick(Level level) {
        processPendingPipelines(level);
        if (++pruneTickCounter >= PRUNE_INTERVAL_TICKS) {
            pruneTickCounter = 0;
            pruneInvalid(level);
        }
        Vec3 playerPos = playerPosition();
        for (ScreenPipeline pipeline : pipelines.values()) {
            if (playerPos != null && !pipeline.isWithinTickRange(playerPos)) {
                continue;
            }
            pipeline.tick(level, playerPos);
        }
    }

    private static Vec3 playerPosition() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : player.position();
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
        pipeline.tick(level, playerPosition());
    }

    private static VideoSourceDescriptor resolveDescriptor(Level level, ScreenGroupMembership membership) {
        BlockEntity blockEntity = level.getBlockEntity(membership.groupOrigin());
        if (blockEntity instanceof LedScreenBlockEntity origin) {
            return ClientVideoSourceCatalog.INSTANCE.resolve(origin);
        }
        return VideoSourceDescriptor.testPattern();
    }

    private static ScreenPipeline createPipeline(ScreenGroupMembership membership, VideoSourceDescriptor descriptor) {
        int[] size = computeTextureSize(membership.gridWidth(), membership.gridHeight(), QualityTier.NEAR);
        VideoSource source = ClientVideoSourceCatalog.INSTANCE.create(descriptor, size[0], size[1]);
        DynamicTextureHandle texture = new DynamicTextureHandle("group_" + membership.groupKey());
        return new ScreenPipeline(membership, descriptor, source, texture, QualityTier.NEAR);
    }

    static int[] computeTextureSize(int gridWidth, int gridHeight) {
        return computeTextureSize(gridWidth, gridHeight, QualityTier.NEAR);
    }

    private static int[] computeTextureSize(int gridWidth, int gridHeight, QualityTier qualityTier) {
        int cellResolution = ModConfig.BASE_CELL_RESOLUTION.get();
        int width = cellResolution * gridWidth;
        int height = cellResolution * gridHeight;
        int maxResolution = ModConfig.MAX_TEXTURE_RESOLUTION.get();
        int minResolution = Math.min(minResolutionForTier(qualityTier), maxResolution);
        int longest = Math.max(width, height);

        if (longest < minResolution) {
            float scale = minResolution / (float) longest;
            width = Math.round(width * scale);
            height = Math.round(height * scale);
            longest = Math.max(width, height);
        }

        if (longest > maxResolution) {
            float scale = maxResolution / (float) longest;
            width = Math.max(64, Math.round(width * scale));
            height = Math.max(64, Math.round(height * scale));
        }

        return new int[]{width, height};
    }

    private static int minResolutionForTier(QualityTier qualityTier) {
        if (!ModConfig.ENABLE_DYNAMIC_LOD.get()) {
            return ModConfig.MIN_TEXTURE_RESOLUTION.get();
        }
        return switch (qualityTier) {
            case NEAR -> ModConfig.MIN_TEXTURE_RESOLUTION.get();
            case MID -> Math.min(ModConfig.MID_TEXTURE_RESOLUTION.get(), ModConfig.MIN_TEXTURE_RESOLUTION.get());
            case FAR -> Math.min(ModConfig.FAR_TEXTURE_RESOLUTION.get(), ModConfig.MID_TEXTURE_RESOLUTION.get());
        };
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
        private VideoSource source;
        private final DynamicTextureHandle texture;
        private QualityTier qualityTier;
        private VideoFrame gradedFrame;

        private int lastFrameWidth;
        private int lastFrameHeight;
        private String displayCacheKey = ScreenDisplaySettings.DEFAULT.cacheKey();
        private int lastUploadedContentHash;
        private VideoFrame lastUploadedFrame;
        private long lastUploadedFrameRevision = -1L;
        private String lastUploadedDisplayKey = "";
        private long lastUploadMs;
        private WallRenderContext renderContext;
        private String lastRenderContextKey = "";

        private ScreenPipeline(ScreenGroupMembership membership, VideoSourceDescriptor descriptor,
                               VideoSource source, DynamicTextureHandle texture, QualityTier qualityTier) {
            this.membership = membership;
            this.descriptor = descriptor;
            this.source = source;
            this.texture = texture;
            this.qualityTier = qualityTier;
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

        private WallRenderContext renderContext() {
            return renderContext;
        }

        private void updateRenderContextIfNeeded(ScreenDisplaySettings displaySettings) {
            int[] size = frameSize();
            String key = displaySettings.cacheKey() + "@" + size[0] + "x" + size[1];
            if (key.equals(lastRenderContextKey) && renderContext != null) {
                return;
            }
            lastRenderContextKey = key;
            renderContext = new WallRenderContext(
                    displaySettings,
                    size[0],
                    size[1],
                    DisplayColorGrading.vertexColor(displaySettings)
            );
        }

        private boolean isWithinTickRange(Vec3 playerPos) {
            BlockPos origin = membership.groupOrigin();
            double dx = playerPos.x - (origin.getX() + 0.5);
            double dy = playerPos.y - (origin.getY() + 0.5);
            double dz = playerPos.z - (origin.getZ() + 0.5);
            double maxDist = MAX_PIPELINE_TICK_DISTANCE + Math.max(membership.gridWidth(), membership.gridHeight());
            return dx * dx + dy * dy + dz * dz <= maxDist * maxDist;
        }

        private void tick(Level level, Vec3 playerPos) {
            updateQualityTierIfNeeded(playerPos);

            ScreenDisplaySettings displaySettings = LedScreenBlockEntity.resolveDisplaySettings(level, membership);
            displayCacheKey = displaySettings.cacheKey();

            source.tick();
            VideoFrame frame = source.getCurrentFrame();
            lastFrameWidth = frame.getWidth();
            lastFrameHeight = frame.getHeight();
            updateRenderContextIfNeeded(displaySettings);

            if (frame.getWidth() != source.getWidth() || frame.getHeight() != source.getHeight()) {
                return;
            }

            long nowMs = System.currentTimeMillis();
            int maxUploadsPerSecond = ModConfig.MAX_TEXTURE_UPDATES_PER_SECOND.get();
            if (maxUploadsPerSecond > 0 && lastUploadMs > 0) {
                long minIntervalMs = 1000L / maxUploadsPerSecond;
                if (nowMs - lastUploadMs < minIntervalMs) {
                    return;
                }
            }

            long frameRevision = frame.getRevision();
            if (frame == lastUploadedFrame
                    && frameRevision == lastUploadedFrameRevision
                    && displayCacheKey.equals(lastUploadedDisplayKey)) {
                return;
            }

            int contentHash = computeUploadContentHash(frame, displaySettings);
            if (contentHash == lastUploadedContentHash
                    && displayCacheKey.equals(lastUploadedDisplayKey)) {
                lastUploadedFrame = frame;
                lastUploadedFrameRevision = frameRevision;
                return;
            }

            if (displaySettings.needsColorGrading()) {
                ensureGradedFrameSize(frame.getWidth(), frame.getHeight());
                DisplayColorGrading.applyInto(frame, gradedFrame, displaySettings);
                texture.upload(gradedFrame);
            } else {
                texture.upload(frame);
            }

            lastUploadedContentHash = contentHash;
            lastUploadedFrame = frame;
            lastUploadedFrameRevision = frameRevision;
            lastUploadedDisplayKey = displayCacheKey;
            lastUploadMs = nowMs;
        }

        private static int computeUploadContentHash(VideoFrame frame, ScreenDisplaySettings displaySettings) {
            int hash = FrameHasher.sampleHash(frame, FRAME_HASH_SAMPLE_SIZE, FRAME_HASH_SAMPLE_SIZE);
            if (displaySettings.needsColorGrading()) {
                hash = 31 * hash + displaySettings.cacheKey().hashCode();
            }
            return hash;
        }

        private void updateQualityTierIfNeeded(Vec3 playerPos) {
            QualityTier desired = qualityTierForPlayer(playerPos);
            if (desired == qualityTier) {
                return;
            }

            int[] size = computeTextureSize(membership.gridWidth(), membership.gridHeight(), desired);
            if (size[0] == source.getWidth() && size[1] == source.getHeight()) {
                qualityTier = desired;
                return;
            }

            source.dispose();
            source = ClientVideoSourceCatalog.INSTANCE.create(descriptor, size[0], size[1]);
            qualityTier = desired;
            gradedFrame = new VideoFrame(source.getWidth(), source.getHeight());
            lastFrameWidth = 0;
            lastFrameHeight = 0;
            lastUploadedContentHash = 0;
            lastUploadedFrame = null;
            lastUploadedFrameRevision = -1L;
            lastUploadedDisplayKey = "";
            lastRenderContextKey = "";
        }

        private QualityTier qualityTierForPlayer(Vec3 playerPos) {
            if (!ModConfig.ENABLE_DYNAMIC_LOD.get() || playerPos == null) {
                return QualityTier.NEAR;
            }
            BlockPos origin = membership.groupOrigin();
            double dx = playerPos.x - (origin.getX() + 0.5);
            double dy = playerPos.y - (origin.getY() + 0.5);
            double dz = playerPos.z - (origin.getZ() + 0.5);
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz)
                    - Math.max(membership.gridWidth(), membership.gridHeight()) * 0.5D;
            int near = ModConfig.LOD_NEAR_DISTANCE.get();
            int mid = Math.max(near + 1, ModConfig.LOD_MID_DISTANCE.get());
            if (distance <= near) {
                return QualityTier.NEAR;
            }
            if (distance <= mid) {
                return QualityTier.MID;
            }
            return QualityTier.FAR;
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
