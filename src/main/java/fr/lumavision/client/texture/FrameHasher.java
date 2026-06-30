package fr.lumavision.client.texture;

import fr.lumavision.video.VideoFrame;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Fast fingerprint for video frame change detection before GPU upload.
 */
@OnlyIn(Dist.CLIENT)
public final class FrameHasher {

    private FrameHasher() {
    }

    public static int sampleHash(VideoFrame frame, int sampleWidth, int sampleHeight) {
        int frameWidth = frame.getWidth();
        int frameHeight = frame.getHeight();
        if (frameWidth <= 0 || frameHeight <= 0) {
            return 0;
        }

        sampleWidth = Math.max(1, sampleWidth);
        sampleHeight = Math.max(1, sampleHeight);

        int hash = 1;
        for (int sy = 0; sy < sampleHeight; sy++) {
            int y = Math.min(frameHeight - 1, (sy * frameHeight) / sampleHeight);
            for (int sx = 0; sx < sampleWidth; sx++) {
                int x = Math.min(frameWidth - 1, (sx * frameWidth) / sampleWidth);
                hash = 31 * hash + frame.getArgb(x, y);
            }
        }
        return hash;
    }
}
