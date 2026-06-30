package fr.lumavision.client.texture;

import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.client.video.TestPatternVideoSource;
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
 * Per-screen pipeline: {@link VideoSource} → {@link VideoFrame} → {@link DynamicTextureHandle}.
 */
@OnlyIn(Dist.CLIENT)
public final class ScreenTextureManager {

    private static final ScreenTextureManager INSTANCE = new ScreenTextureManager();

    private final Map<BlockPos, ScreenPipeline> pipelines = new HashMap<>();

    private ScreenTextureManager() {
    }

    public static ScreenTextureManager getInstance() {
        return INSTANCE;
    }

    public ResourceLocation getTexture(LedScreenBlockEntity blockEntity) {
        return pipelineFor(blockEntity).texture().location();
    }

    public void tick(Level level) {
        pruneInvalid(level);
        for (ScreenPipeline pipeline : pipelines.values()) {
            pipeline.tick();
        }
    }

    public void remove(BlockPos pos) {
        ScreenPipeline pipeline = pipelines.remove(pos);
        if (pipeline != null) {
            pipeline.close();
        }
    }

    public void clear() {
        for (ScreenPipeline pipeline : pipelines.values()) {
            pipeline.close();
        }
        pipelines.clear();
    }

    private ScreenPipeline pipelineFor(LedScreenBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return pipelines.computeIfAbsent(pos, ignored -> createPipeline(pos));
    }

    private static ScreenPipeline createPipeline(BlockPos pos) {
        VideoSource source = new TestPatternVideoSource(128, 128);
        DynamicTextureHandle texture = new DynamicTextureHandle("screen_" + pos.asLong());
        ScreenPipeline pipeline = new ScreenPipeline(source, texture);
        pipeline.tick();
        return pipeline;
    }

    private void pruneInvalid(Level level) {
        Iterator<Map.Entry<BlockPos, ScreenPipeline>> iterator = pipelines.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, ScreenPipeline> entry = iterator.next();
            BlockEntity blockEntity = level.getBlockEntity(entry.getKey());
            if (!(blockEntity instanceof LedScreenBlockEntity)) {
                entry.getValue().close();
                iterator.remove();
            }
        }
    }

    private static final class ScreenPipeline implements AutoCloseable {
        private final VideoSource source;
        private final DynamicTextureHandle texture;

        private ScreenPipeline(VideoSource source, DynamicTextureHandle texture) {
            this.source = source;
            this.texture = texture;
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
