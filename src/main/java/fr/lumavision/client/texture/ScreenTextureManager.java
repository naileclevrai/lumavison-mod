package fr.lumavision.client.texture;

import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.client.ndi.NdiSourceResolver;
import fr.lumavision.client.video.ClientVideoSourceFactory;
import fr.lumavision.config.ModConfig;
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
import java.util.Iterator;
import java.util.Map;

/**
 * One {@link VideoSource} pipeline per merged screen group (wall).
 */
@OnlyIn(Dist.CLIENT)
public final class ScreenTextureManager {

    private static final int BASE_CELL_RESOLUTION = 128;

    private static final ScreenTextureManager INSTANCE = new ScreenTextureManager();

    private final Map<Long, ScreenPipeline> pipelines = new HashMap<>();

    private ScreenTextureManager() {
    }

    public static ScreenTextureManager getInstance() {
        return INSTANCE;
    }

    public ResourceLocation getTexture(LedScreenBlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            throw new IllegalStateException("Cannot resolve screen texture without a level");
        }
        return pipelineFor(level, blockEntity).texture().location();
    }

    public void tick(Level level) {
        pruneInvalid(level);
        for (ScreenPipeline pipeline : pipelines.values()) {
            pipeline.tick();
        }
    }

    public void clear() {
        for (ScreenPipeline pipeline : pipelines.values()) {
            pipeline.close();
        }
        pipelines.clear();
    }

    private ScreenPipeline pipelineFor(Level level, LedScreenBlockEntity blockEntity) {
        ScreenGroupMembership membership = blockEntity.getGroupMembership();
        long key = membership.groupKey();
        VideoSourceDescriptor descriptor = resolveDescriptor(level, membership);

        ScreenPipeline existing = pipelines.get(key);
        if (existing != null && existing.matches(membership, descriptor)) {
            return existing;
        }
        if (existing != null) {
            existing.close();
        }

        ScreenPipeline pipeline = createPipeline(membership, descriptor);
        pipelines.put(key, pipeline);
        return pipeline;
    }

    private static VideoSourceDescriptor resolveDescriptor(Level level, ScreenGroupMembership membership) {
        BlockEntity blockEntity = level.getBlockEntity(membership.groupOrigin());
        if (blockEntity instanceof LedScreenBlockEntity origin) {
            return NdiSourceResolver.resolve(origin);
        }
        return VideoSourceDescriptor.testPattern();
    }

    private static ScreenPipeline createPipeline(ScreenGroupMembership membership, VideoSourceDescriptor descriptor) {
        int[] size = computeTextureSize(membership.gridWidth(), membership.gridHeight());
        VideoSource source = ClientVideoSourceFactory.INSTANCE.create(descriptor, size[0], size[1]);
        DynamicTextureHandle texture = new DynamicTextureHandle("group_" + membership.groupKey());
        ScreenPipeline pipeline = new ScreenPipeline(membership, descriptor, source, texture);
        pipeline.tick();
        return pipeline;
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
            if (!membership.groupOrigin().equals(origin) || !entry.getValue().matches(membership, descriptor)) {
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

        private ScreenPipeline(ScreenGroupMembership membership, VideoSourceDescriptor descriptor,
                               VideoSource source, DynamicTextureHandle texture) {
            this.membership = membership;
            this.descriptor = descriptor;
            this.source = source;
            this.texture = texture;
        }

        private boolean matches(ScreenGroupMembership other, VideoSourceDescriptor otherDescriptor) {
            return membership.gridWidth() == other.gridWidth()
                    && membership.gridHeight() == other.gridHeight()
                    && membership.groupOrigin().equals(other.groupOrigin())
                    && descriptor.cacheKey().equals(otherDescriptor.cacheKey());
        }

        private DynamicTextureHandle texture() {
            return texture;
        }

        private void tick() {
            source.tick();
            VideoFrame frame = source.getCurrentFrame();
            texture.upload(frame);
        }

        @Override
        public void close() {
            source.dispose();
            texture.close();
        }
    }
}
