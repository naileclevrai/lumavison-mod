package fr.lumavision.video;

import net.minecraft.core.BlockPos;

import java.util.Objects;

/**
 * Immutable configuration for creating a {@link VideoSource}.
 */
public record VideoSourceDescriptor(VideoSourceType type, String payload) {

    public static final String TEST_PATTERN_ID = "test";

    public static final String NDI_PREFIX = "ndi:";
    public static final String FILE_PREFIX = "file:";
    public static final String GIF_PREFIX = "gif:";
    public static final String IMAGE_PREFIX = "image:";
    public static final String BROWSER_PREFIX = "browser:";
    public static final String WEBCAM_PREFIX = "webcam:";
    public static final String NETWORK_PREFIX = "network:";
    public static final String SPOUT_PREFIX = "spout:";
    public static final String SYPHON_PREFIX = "syphon:";
    public static final String SCREEN_CAPTURE_PREFIX = "capture:";
    public static final String RELAY_PREFIX = "relay:";

    /** Internal relay key: {@code relay:x,y,z} for a wall group origin. */
    public static String relayKey(BlockPos origin) {
        return origin.getX() + "," + origin.getY() + "," + origin.getZ();
    }

    public VideoSourceDescriptor {
        Objects.requireNonNull(type, "type");
        payload = payload == null ? "" : payload;
    }

    public static VideoSourceDescriptor testPattern() {
        return new VideoSourceDescriptor(VideoSourceType.TEST_PATTERN, TEST_PATTERN_ID);
    }

    public static VideoSourceDescriptor ndi(String sourceName) {
        return typed(VideoSourceType.NDI, sourceName);
    }

    public static VideoSourceDescriptor file(String path) {
        return typed(VideoSourceType.FILE, path);
    }

    public static VideoSourceDescriptor gif(String path) {
        return typed(VideoSourceType.GIF, path);
    }

    public static VideoSourceDescriptor image(String path) {
        return typed(VideoSourceType.IMAGE, path);
    }

    public static VideoSourceDescriptor browser(String url) {
        return typed(VideoSourceType.BROWSER, url);
    }

    public static VideoSourceDescriptor webcam(String deviceId) {
        return typed(VideoSourceType.WEBCAM, deviceId);
    }

    public static VideoSourceDescriptor network(String url) {
        return typed(VideoSourceType.NETWORK, url);
    }

    public static VideoSourceDescriptor spout(String senderName) {
        return typed(VideoSourceType.SPOUT, senderName);
    }

    public static VideoSourceDescriptor syphon(String serverName) {
        return typed(VideoSourceType.SYPHON, serverName);
    }

    public static VideoSourceDescriptor screenCapture(String monitorId) {
        return typed(VideoSourceType.SCREEN_CAPTURE, monitorId);
    }

    public static VideoSourceDescriptor relay(BlockPos groupOrigin) {
        return typed(VideoSourceType.RELAY, relayKey(groupOrigin));
    }

    public static BlockPos parseRelayOrigin(String payload) {
        String[] parts = payload.split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid relay payload: " + payload);
        }
        return new BlockPos(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim()));
    }

    private static VideoSourceDescriptor typed(VideoSourceType type, String payload) {
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException(type + " payload must not be blank");
        }
        return new VideoSourceDescriptor(type, payload.trim());
    }

    public boolean isTestPattern() {
        return type == VideoSourceType.TEST_PATTERN;
    }

    public boolean isNdi() {
        return type == VideoSourceType.NDI;
    }

    public boolean isRelay() {
        return type == VideoSourceType.RELAY;
    }

    /**
     * Stable key for pipeline reuse comparisons.
     */
    public String cacheKey() {
        return type.name() + ":" + payload;
    }

    /**
     * Serializes this descriptor into a persisted {@code sourceId} string.
     */
    public String toSourceId() {
        if (isTestPattern()) {
            return TEST_PATTERN_ID;
        }
        return prefixFor(type) + payload;
    }

    public static String prefixFor(VideoSourceType type) {
        return switch (type) {
            case TEST_PATTERN -> "";
            case NDI -> NDI_PREFIX;
            case FILE -> FILE_PREFIX;
            case GIF -> GIF_PREFIX;
            case IMAGE -> IMAGE_PREFIX;
            case BROWSER -> BROWSER_PREFIX;
            case WEBCAM -> WEBCAM_PREFIX;
            case NETWORK -> NETWORK_PREFIX;
            case SPOUT -> SPOUT_PREFIX;
            case SYPHON -> SYPHON_PREFIX;
            case SCREEN_CAPTURE -> SCREEN_CAPTURE_PREFIX;
            case RELAY -> RELAY_PREFIX;
        };
    }
}
