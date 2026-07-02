package fr.lumavision.client.relay;

import fr.lumavision.video.VideoFrame;
import fr.lumavision.video.VideoSource;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Video source backed by server-relayed frames (multiplayer viewers).
 */
@OnlyIn(Dist.CLIENT)
public final class RelayedVideoSource implements VideoSource {

    private final BlockPos groupOrigin;
    private final int width;
    private final int height;
    private final VideoFrame frame;

    public RelayedVideoSource(BlockPos groupOrigin, int width, int height) {
        this.groupOrigin = groupOrigin.immutable();
        this.width = width;
        this.height = height;
        this.frame = new VideoFrame(width, height);
        frame.fill(0xFF101010);
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void tick() {
        VideoFrame relayed = MediaRelayClient.getInstance().pollRelayFrame(groupOrigin, width, height);
        if (relayed != null) {
            copyFrame(relayed);
        }
    }

    @Override
    public VideoFrame getCurrentFrame() {
        return frame;
    }

    @Override
    public void dispose() {
        // No native resources
    }

    private void copyFrame(VideoFrame source) {
        if (source.getWidth() == width && source.getHeight() == height) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    frame.setArgb(x, y, source.getArgb(x, y));
                }
            }
        } else {
            for (int y = 0; y < height; y++) {
                int srcY = y * source.getHeight() / height;
                for (int x = 0; x < width; x++) {
                    int srcX = x * source.getWidth() / width;
                    frame.setArgb(x, y, source.getArgb(srcX, srcY));
                }
            }
        }
        frame.markDirty();
    }
}
