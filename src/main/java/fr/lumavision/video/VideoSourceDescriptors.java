package fr.lumavision.video;

/**
 * Parses persisted {@code sourceId} strings from block entities and config.
 */
public final class VideoSourceDescriptors {

    private VideoSourceDescriptors() {
    }

    /**
     * Parses a stored source id.
     * <ul>
     *   <li>{@code ndi:MACHINE (Source)} → NDI descriptor</li>
     *   <li>{@code test} or empty → test pattern</li>
     * </ul>
     */
    public static VideoSourceDescriptor parse(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return VideoSourceDescriptor.testPattern();
        }

        String trimmed = sourceId.trim();
        if (trimmed.equalsIgnoreCase(VideoSourceDescriptor.TEST_PATTERN_ID)) {
            return VideoSourceDescriptor.testPattern();
        }

        if (trimmed.regionMatches(true, 0, VideoSourceDescriptor.NDI_PREFIX, 0, VideoSourceDescriptor.NDI_PREFIX.length())) {
            String name = trimmed.substring(VideoSourceDescriptor.NDI_PREFIX.length()).trim();
            if (!name.isEmpty()) {
                return VideoSourceDescriptor.ndi(name);
            }
        }

        return VideoSourceDescriptor.testPattern();
    }

    public static boolean hasExplicitSource(String sourceId) {
        return sourceId != null && !sourceId.isBlank();
    }
}
