package fr.lumavision.client.relay;

import fr.lumavision.config.ModConfig;
import fr.lumavision.relay.FrameCompression;
import fr.lumavision.relay.WallRelayRole;
import fr.lumavision.video.VideoFrame;
import fr.lumavision.client.texture.ScreenTextureManager;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Client-side cache of relay roles and received frames from the server bridge.
 */
@OnlyIn(Dist.CLIENT)
public final class MediaRelayClient {

    private static final MediaRelayClient INSTANCE = new MediaRelayClient();

    private final Map<BlockPos, WallRelayRole> wallRoles = new HashMap<>();
    private final Map<BlockPos, RelayedFrameBuffer> frameBuffers = new HashMap<>();
    private final Set<BlockPos> captureCameras = new java.util.HashSet<>();

    private MediaRelayClient() {
    }

    public static MediaRelayClient getInstance() {
        return INSTANCE;
    }

    public void onSync(Map<BlockPos, WallRelayRole> roles, Set<BlockPos> cameras) {
        wallRoles.clear();
        wallRoles.putAll(roles);
        captureCameras.clear();
        captureCameras.addAll(cameras);

        frameBuffers.keySet().retainAll(wallRoles.keySet());
        for (Map.Entry<BlockPos, WallRelayRole> entry : wallRoles.entrySet()) {
            if (entry.getValue() != WallRelayRole.RECEIVE) {
                frameBuffers.remove(entry.getKey());
            }
        }

        ScreenTextureManager.getInstance().invalidateWalls(roles.keySet());
    }

    public void onFrameReceived(BlockPos origin, long sequence, int width, int height, byte[] compressed) {
        if (wallRoles.get(origin) != WallRelayRole.RECEIVE) {
            return;
        }
        try {
            byte[] rgba = FrameCompression.decompress(compressed, width * height * 4);
            RelayedFrameBuffer buffer = frameBuffers.computeIfAbsent(origin, ignored -> new RelayedFrameBuffer(width, height));
            buffer.update(width, height, rgba, sequence);
        } catch (RuntimeException ignored) {
            // Corrupt or stale frame — skip
        }
    }

    public WallRelayRole wallRole(BlockPos groupOrigin) {
        if (!ModConfig.ENABLE_MULTIPLAYER_RELAY.get()) {
            return WallRelayRole.LOCAL;
        }
        return wallRoles.getOrDefault(groupOrigin, WallRelayRole.LOCAL);
    }

    public boolean shouldCaptureCamera(BlockPos cameraPos) {
        if (!ModConfig.ENABLE_MULTIPLAYER_RELAY.get()) {
            return true;
        }
        if (captureCameras.isEmpty() && wallRoles.isEmpty()) {
            return true;
        }
        return captureCameras.contains(cameraPos);
    }

    public VideoFrame pollRelayFrame(BlockPos groupOrigin, int targetWidth, int targetHeight) {
        RelayedFrameBuffer buffer = frameBuffers.get(groupOrigin);
        if (buffer == null) {
            return null;
        }
        return buffer.copyFrame(targetWidth, targetHeight);
    }

    public void clear() {
        wallRoles.clear();
        frameBuffers.clear();
        captureCameras.clear();
    }

    private static final class RelayedFrameBuffer {
        private VideoFrame frame;
        private long sequence;

        RelayedFrameBuffer(int width, int height) {
            this.frame = new VideoFrame(width, height);
        }

        void update(int width, int height, byte[] rgba, long sequence) {
            if (frame.getWidth() != width || frame.getHeight() != height) {
                frame = new VideoFrame(width, height);
            }
            frame.copyFromRgbaBytes(rgba, width, height);
            this.sequence = sequence;
        }

        VideoFrame copyFrame(int targetWidth, int targetHeight) {
            if (frame.getWidth() == targetWidth && frame.getHeight() == targetHeight) {
                return frame;
            }
            VideoFrame scaled = new VideoFrame(targetWidth, targetHeight);
            scaleNearest(frame, scaled);
            return scaled;
        }

        private static void scaleNearest(VideoFrame source, VideoFrame target) {
            int srcW = source.getWidth();
            int srcH = source.getHeight();
            int dstW = target.getWidth();
            int dstH = target.getHeight();
            for (int y = 0; y < dstH; y++) {
                int srcY = y * srcH / dstH;
                for (int x = 0; x < dstW; x++) {
                    int srcX = x * srcW / dstW;
                    target.setArgb(x, y, source.getArgb(srcX, srcY));
                }
            }
            target.markDirty();
        }
    }
}
