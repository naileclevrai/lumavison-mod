package fr.lumavision.client.relay;

import fr.lumavision.config.ModConfig;
import fr.lumavision.network.ModNetworking;
import fr.lumavision.network.ScreenFrameChunkHandler;
import fr.lumavision.relay.FrameCompression;
import fr.lumavision.relay.WallRelayRole;
import fr.lumavision.video.VideoFrame;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * Uploads captured frames from the elected capture client to the server bridge.
 */
@OnlyIn(Dist.CLIENT)
public final class ScreenFrameUploader {

    private static final ScreenFrameUploader INSTANCE = new ScreenFrameUploader();

    private final Map<BlockPos, Long> lastUploadMs = new HashMap<>();
    private final Map<BlockPos, Long> sequence = new HashMap<>();

    private ScreenFrameUploader() {
    }

    public static ScreenFrameUploader getInstance() {
        return INSTANCE;
    }

    public void maybeUpload(BlockPos groupOrigin, VideoFrame frame) {
        if (MediaRelayClient.getInstance().wallRole(groupOrigin) != WallRelayRole.UPLOAD) {
            return;
        }
        if (frame == null) {
            return;
        }

        long nowMs = System.currentTimeMillis();
        int maxFps = ModConfig.RELAY_MAX_FPS.get();
        if (maxFps > 0) {
            Long last = lastUploadMs.get(groupOrigin);
            long minInterval = 1000L / maxFps;
            if (last != null && nowMs - last < minInterval) {
                return;
            }
        }

        int maxRes = ModConfig.RELAY_MAX_FRAME_RESOLUTION.get();
        VideoFrame payload = downscaleIfNeeded(frame, maxRes);
        byte[] rgba = payload.copyRgbaBytes();
        byte[] compressed = FrameCompression.compress(rgba);
        long seq = sequence.merge(groupOrigin, 1L, Long::sum);

        ScreenFrameChunkHandler.sendToServer(
                ModNetworking.CHANNEL,
                groupOrigin,
                seq,
                payload.getWidth(),
                payload.getHeight(),
                compressed
        );
        lastUploadMs.put(groupOrigin, nowMs);
    }

    public void clear() {
        lastUploadMs.clear();
        sequence.clear();
    }

    private static VideoFrame downscaleIfNeeded(VideoFrame frame, int maxRes) {
        int w = frame.getWidth();
        int h = frame.getHeight();
        int longest = Math.max(w, h);
        if (longest <= maxRes) {
            return frame;
        }
        float scale = maxRes / (float) longest;
        int nw = Math.max(64, Math.round(w * scale));
        int nh = Math.max(64, Math.round(h * scale));
        VideoFrame scaled = new VideoFrame(nw, nh);
        for (int y = 0; y < nh; y++) {
            int srcY = y * h / nh;
            for (int x = 0; x < nw; x++) {
                int srcX = x * w / nw;
                scaled.setArgb(x, y, frame.getArgb(srcX, srcY));
            }
        }
        scaled.markDirty();
        return scaled;
    }
}
