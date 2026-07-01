package fr.lumavision.client.display;

import fr.lumavision.screen.ScreenDisplaySettings;
import fr.lumavision.video.VideoFrame;

/**
 * Applies brightness, contrast, gamma, and color temperature to a frame copy.
 * Runs in the display layer — does not modify {@link fr.lumavision.video.VideoSource}.
 */
public final class DisplayColorGrading {

    private DisplayColorGrading() {
    }

    public static void applyInto(VideoFrame source, VideoFrame target, ScreenDisplaySettings settings) {
        if (source.getWidth() != target.getWidth() || source.getHeight() != target.getHeight()) {
            throw new IllegalArgumentException("Frame size mismatch");
        }

        float contrast = settings.contrast();
        float gamma = settings.gamma();
        float invGamma = 1.0F / gamma;

        int width = source.getWidth();
        int height = source.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = source.getArgb(x, y);
                int a = (argb >>> 24) & 0xFF;
                float r = ((argb >>> 16) & 0xFF) / 255.0F;
                float g = ((argb >>> 8) & 0xFF) / 255.0F;
                float b = (argb & 0xFF) / 255.0F;

                r = applyContrast(r, contrast);
                g = applyContrast(g, contrast);
                b = applyContrast(b, contrast);

                r = (float) Math.pow(Math.max(0.0F, r), invGamma);
                g = (float) Math.pow(Math.max(0.0F, g), invGamma);
                b = (float) Math.pow(Math.max(0.0F, b), invGamma);

                target.setArgb(x, y, (a << 24)
                        | (toByte(r) << 16)
                        | (toByte(g) << 8)
                        | toByte(b));
            }
        }
        target.markDirty();
    }

    public static int[] vertexColor(ScreenDisplaySettings settings) {
        float brightness = settings.brightness();
        float colorTemp = settings.colorTemp();

        float warmR = 1.0F + colorTemp * 0.12F;
        float warmB = 1.0F - colorTemp * 0.12F;

        int r = toByte(brightness * warmR);
        int g = toByte(brightness);
        int b = toByte(brightness * warmB);
        return new int[]{r, g, b, 255};
    }

    private static float applyContrast(float channel, float contrast) {
        return (channel - 0.5F) * contrast + 0.5F;
    }

    private static int toByte(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
    }
}
