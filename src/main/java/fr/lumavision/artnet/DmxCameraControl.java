package fr.lumavision.artnet;

import fr.lumavision.camera.CameraParameters;
import fr.lumavision.camera.DmxPatch;

/**
 * Maps live Art-Net/DMX values onto a camera's {@link CameraParameters}, following Theatrical's
 * channel-slice pattern (read each patched channel, scale to the parameter's range). 8-bit by default;
 * 16-bit reads a coarse+fine pair for smooth pan/tilt/track. Runs server-side on the camera tick.
 */
public final class DmxCameraControl {

    private DmxCameraControl() {
    }

    /** Applies the patch's DMX values to {@code p}. Returns true if any value changed. */
    public static boolean apply(CameraParameters p, DmxPatch d) {
        if (!d.isActive()) {
            return false;
        }
        boolean changed = false;

        if (d.panChannel() > 0) {
            float pan = -180.0F + read(d, d.panChannel()) * 360.0F;
            if (Math.abs(pan - p.pan()) > 0.05F) {
                p.setPan(pan);
                changed = true;
            }
        }
        if (d.tiltChannel() > 0) {
            float tilt = -90.0F + read(d, d.tiltChannel()) * 180.0F;
            if (Math.abs(tilt - p.tilt()) > 0.05F) {
                p.setTilt(tilt);
                changed = true;
            }
        }
        if (d.zoomChannel() > 0) {
            // 0 = wide (110°), full = tele (25°).
            float fov = 110.0F - read(d, d.zoomChannel()) * 85.0F;
            if (Math.abs(fov - p.fov()) > 0.05F) {
                p.setFov(fov);
                changed = true;
            }
        }
        if (d.trackChannel() > 0) {
            float track = read(d, d.trackChannel());
            if (Math.abs(track - p.trackPosition()) > 0.002F) {
                p.setTrackPosition(track);
                changed = true;
            }
        }
        if (d.enableChannel() > 0) {
            boolean enabled = ArtNetReceiver.channel(d.universe(), d.enableChannel()) >= 128;
            if (enabled != p.enabled()) {
                p.setEnabled(enabled);
                changed = true;
            }
        }
        return changed;
    }

    /** Reads a channel (or coarse+fine pair when 16-bit) as a normalized 0..1 value. */
    private static float read(DmxPatch d, int channel) {
        int coarse = ArtNetReceiver.channel(d.universe(), channel);
        if (d.sixteenBit()) {
            int fine = ArtNetReceiver.channel(d.universe(), channel + 1);
            return ((coarse << 8) | fine) / 65535.0F;
        }
        return coarse / 255.0F;
    }
}
