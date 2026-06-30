package fr.lumavision.client.texture;

import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.client.video.TestPatternVideoSource;
import fr.lumavision.config.ModConfig;
import fr.lumavision.screen.ScreenGroupMembership;
import fr.lumavision.video.VideoFrame;
import fr.lumavision.video.VideoSource;
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
 * <p>
 * Individual blocks sample the same dynamic texture with per-cell UV offsets.
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
        return pipelineFor(blockEntity.getGroupMembership()).texture().location();
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

    private ScreenPipeline pipelineFor(ScreenGroupMembership membership) {
        long key = membership.groupKey();
        ScreenPipeline existing = pipelines.get(key);
        if (existing != null && existing.matches(membership)) {
            return existing;
        }
        if (existing != null) {
            existing.close();
        }

        ScreenPipeline pipeline = createPipeline(membership);
        pipelines.put(key, pipeline);
        return pipeline;
    }

    private static ScreenPipeline createPipeline(ScreenGroupMembership membership) {
        int[] size = computeTextureSize(membership.gridWidth(), membership.gridHeight());
        VideoSource source = new TestPatternVideoSource(size[0], size[1]);
        DynamicTextureHandle texture = new DynamicTextureHandle("group_" + membership.groupKey());
        ScreenPipeline pipeline = new ScreenPipeline(membership, source, texture);
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
            if (!membership.groupOrigin().equals(origin) || !entry.getValue().matches(membership)) {
                entry.getValue().close();
                iterator.remove();
            }
        }
    }

    private static final class ScreenPipeline implements AutoCloseable {
        private final ScreenGroupMembership membership;
        private final VideoSource source;
        private final DynamicTextureHandle texture;

        private ScreenPipeline(ScreenGroupMembership membership, VideoSource source, DynamicTextureHandle texture) {
            this.membership = membership;
            this.source = source;
            this.texture = texture;
        }

        private boolean matches(ScreenGroupMembership other) {
            return membership.gridWidth() == other.gridWidth()
                    && membership.gridHeight() == other.gridHeight()
                    && membership.groupOrigin().equals(other.groupOrigin());
        }

        private DynamicTextureHandle texture() {
            return texture;
        }

        private void tick() {
            source.tick();
            texture.upload(source.getCurrentFrame());
        }

        @Override
        public void close() {
            source.dispose();
            texture.close();
        }
    }
}
