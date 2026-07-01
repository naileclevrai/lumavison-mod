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

        int[] redMap = new int[256];
        int[] greenMap = new int[256];
        int[] blueMap = new int[256];
        buildLookupTables(settings, redMap, greenMap, blueMap);
        target.copyColorGradedFrom(source, redMap, greenMap, blueMap);
    }

    public static int[] vertexColor(ScreenDisplaySettings settings) {
        return new int[]{255, 255, 255, 255};
    }

    private static void buildLookupTables(ScreenDisplaySettings settings, int[] redMap, int[] greenMap, int[] blueMap) {
        float brightness = settings.brightness();
        float contrast = settings.contrast();
        float gamma = settings.gamma();
        float colorTemp = settings.colorTemp();
        float invGamma = 1.0F / gamma;

        float warmR = 1.0F + colorTemp * 0.25F;
        float warmB = 1.0F - colorTemp * 0.25F;

        for (int i = 0; i < 256; i++) {
            float channel = i / 255.0F;
            redMap[i] = toByte((float) Math.pow(Math.max(0.0F,
                    applyContrast(channel, contrast) * brightness * warmR), invGamma));
            greenMap[i] = toByte((float) Math.pow(Math.max(0.0F,
                    applyContrast(channel, contrast) * brightness), invGamma));
            blueMap[i] = toByte((float) Math.pow(Math.max(0.0F,
                    applyContrast(channel, contrast) * brightness * warmB), invGamma));
        }
    }

    private static float applyContrast(float channel, float contrast) {
        return (channel - 0.5F) * contrast + 0.5F;
    }

    private static int toByte(float value) {
        return Math.max(0, Math.min(255, Math.round(value * 255.0F)));
    }
}
