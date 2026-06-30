package fr.lumavision.video;

import java.util.Objects;

/**
 * Immutable configuration for creating a {@link VideoSource}.
 */
public record VideoSourceDescriptor(VideoSourceType type, String payload) {

    public static final String NDI_PREFIX = "ndi:";
    public static final String TEST_PATTERN_ID = "test";

    public VideoSourceDescriptor {
        Objects.requireNonNull(type, "type");
        payload = payload == null ? "" : payload;
    }

    public static VideoSourceDescriptor testPattern() {
        return new VideoSourceDescriptor(VideoSourceType.TEST_PATTERN, TEST_PATTERN_ID);
    }

    public static VideoSourceDescriptor ndi(String sourceName) {
        if (sourceName == null || sourceName.isBlank()) {
            throw new IllegalArgumentException("NDI source name must not be blank");
        }
        return new VideoSourceDescriptor(VideoSourceType.NDI, sourceName.trim());
    }

    public boolean isTestPattern() {
        return type == VideoSourceType.TEST_PATTERN;
    }

    public boolean isNdi() {
        return type == VideoSourceType.NDI;
    }

    /**
     * Stable key for pipeline reuse comparisons.
     */
    public String cacheKey() {
        return type.name() + ":" + payload;
    }
}
