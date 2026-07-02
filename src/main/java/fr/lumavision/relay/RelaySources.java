package fr.lumavision.relay;

import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceType;

/**
 * Determines which media sources require a multiplayer relay (local-machine capture).
 */
public final class RelaySources {

    private RelaySources() {
    }

    public static boolean needsRelay(VideoSourceDescriptor descriptor) {
        if (descriptor == null || descriptor.isTestPattern()) {
            return false;
        }
        return switch (descriptor.type()) {
            case NDI, SPOUT, SYPHON, WEBCAM, SCREEN_CAPTURE, NETWORK, FILE, GIF, IMAGE, BROWSER -> true;
            case TEST_PATTERN, RELAY -> false;
        };
    }

    public static boolean needsRelay(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return false;
        }
        return needsRelay(fr.lumavision.video.VideoSourceDescriptors.parse(sourceId));
    }
}
