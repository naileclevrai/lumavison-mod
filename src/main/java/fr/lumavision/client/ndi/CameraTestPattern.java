package fr.lumavision.client.ndi;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Procedural BGRA test pattern used as the M2 frame source. It exists to validate the whole NDI
 * send path (naming, resolution, frame rate, threading) on real hardware before the offscreen
 * world-view capture (M2b) replaces it. Pattern: SMPTE-style vertical colour bars, a white line
 * that scans top-to-bottom each second (proves live frames), and a border tinted by a hash of the
 * source name so multiple cameras are visually distinct in a switcher.
 */
@OnlyIn(Dist.CLIENT)
final class CameraTestPattern {

    private static final int[] BARS = {
            0xC0C0C0, 0xC0C000, 0x00C0C0, 0x00C000, 0xC000C0, 0xC00000, 0x0000C0, 0x101010
    };

    private CameraTestPattern() {
    }

    /** Fills {@code out} (length w*h*4) with BGRA bytes. */
    static void fill(byte[] out, int w, int h, CameraSnapshot snapshot, long frameIndex) {
        int scanLine = (int) (frameIndex % Math.max(1, h));
        int tint = identityColor(snapshot);
        for (int y = 0; y < h; y++) {
            boolean isScan = Math.abs(y - scanLine) <= 1;
            int rowBase = y * w * 4;
            for (int x = 0; x < w; x++) {
                int rgb;
                if (x < 4 || x >= w - 4 || y < 4 || y >= h - 4) {
                    rgb = tint;
                } else if (isScan) {
                    rgb = 0xFFFFFF;
                } else {
                    rgb = BARS[Math.min(BARS.length - 1, (x * BARS.length) / w)];
                }
                int i = rowBase + x * 4;
                out[i] = (byte) (rgb & 0xFF);          // B
                out[i + 1] = (byte) ((rgb >> 8) & 0xFF); // G
                out[i + 2] = (byte) ((rgb >> 16) & 0xFF); // R
                out[i + 3] = (byte) 0xFF;               // A
            }
        }
    }

    private static int identityColor(CameraSnapshot snapshot) {
        int h = snapshot.name().hashCode();
        return (h & 0xFFFFFF) | 0x404040;
    }
}
