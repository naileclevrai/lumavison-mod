package fr.lumavision.client.shimmer;

import fr.lumavision.video.VideoFrame;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Computes average screen color from a downsampled grid over a {@link VideoFrame}.
 */
@OnlyIn(Dist.CLIENT)
public final class FrameAverageSampler {

    private FrameAverageSampler() {
    }

    /**
     * @return packed ARGB with alpha 255 (Shimmer intensity channel)
     */
    public static int averageArgb(VideoFrame frame, int sampleWidth, int sampleHeight) {
        return averageArgb(frame, sampleWidth, sampleHeight, 0.0F, 0.0F, 1.0F, 1.0F);
    }

    /**
     * Samples a normalized UV rectangle of the frame (u/v in 0–1, v=0 is top).
     *
     * @return packed ARGB with alpha 255
     */
    public static int averageArgb(
            VideoFrame frame,
            int sampleWidth,
            int sampleHeight,
            float u0,
            float v0,
            float u1,
            float v1
    ) {
        int frameWidth = frame.getWidth();
        int frameHeight = frame.getHeight();
        if (frameWidth <= 0 || frameHeight <= 0) {
            return 0xFF000000;
        }

        sampleWidth = Math.max(1, sampleWidth);
        sampleHeight = Math.max(1, sampleHeight);

        float minU = Math.min(u0, u1);
        float maxU = Math.max(u0, u1);
        float minV = Math.min(v0, v1);
        float maxV = Math.max(v0, v1);

        long redSum = 0;
        long greenSum = 0;
        long blueSum = 0;
        int count = 0;

        for (int sy = 0; sy < sampleHeight; sy++) {
            float v = minV + (maxV - minV) * (sy + 0.5F) / sampleHeight;
            int y = Math.min(frameHeight - 1, Math.max(0, (int) (v * frameHeight)));
            for (int sx = 0; sx < sampleWidth; sx++) {
                float u = minU + (maxU - minU) * (sx + 0.5F) / sampleWidth;
                int x = Math.min(frameWidth - 1, Math.max(0, (int) (u * frameWidth)));
                int argb = frame.getArgb(x, y);
                redSum += (argb >>> 16) & 0xFF;
                greenSum += (argb >>> 8) & 0xFF;
                blueSum += argb & 0xFF;
                count++;
            }
        }

        if (count == 0) {
            return 0xFF000000;
        }

        int red = (int) (redSum / count);
        int green = (int) (greenSum / count);
        int blue = (int) (blueSum / count);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    public static int sampleHash(VideoFrame frame, int sampleWidth, int sampleHeight) {
        return sampleHash(frame, sampleWidth, sampleHeight, 0.0F, 0.0F, 1.0F, 1.0F);
    }

    /**
     * Fast fingerprint for frame-change detection (same sample grid as {@link #averageArgb}).
     */
    public static int sampleHash(
            VideoFrame frame,
            int sampleWidth,
            int sampleHeight,
            float u0,
            float v0,
            float u1,
            float v1
    ) {
        int frameWidth = frame.getWidth();
        int frameHeight = frame.getHeight();
        if (frameWidth <= 0 || frameHeight <= 0) {
            return 0;
        }

        sampleWidth = Math.max(1, sampleWidth);
        sampleHeight = Math.max(1, sampleHeight);

        float minU = Math.min(u0, u1);
        float maxU = Math.max(u0, u1);
        float minV = Math.min(v0, v1);
        float maxV = Math.max(v0, v1);

        int hash = 1;
        for (int sy = 0; sy < sampleHeight; sy++) {
            float v = minV + (maxV - minV) * (sy + 0.5F) / sampleHeight;
            int y = Math.min(frameHeight - 1, Math.max(0, (int) (v * frameHeight)));
            for (int sx = 0; sx < sampleWidth; sx++) {
                float u = minU + (maxU - minU) * (sx + 0.5F) / sampleWidth;
                int x = Math.min(frameWidth - 1, Math.max(0, (int) (u * frameWidth)));
                hash = 31 * hash + frame.getArgb(x, y);
            }
        }
        return hash;
    }
}
