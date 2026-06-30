package fr.lumavision.video;

/**
 * Parses persisted {@code sourceId} strings from block entities and config.
 */
public final class VideoSourceDescriptors {

    private static final String[] PREFIXES = {
            VideoSourceDescriptor.NDI_PREFIX,
            VideoSourceDescriptor.FILE_PREFIX,
            VideoSourceDescriptor.GIF_PREFIX,
            VideoSourceDescriptor.IMAGE_PREFIX,
            VideoSourceDescriptor.BROWSER_PREFIX,
            VideoSourceDescriptor.WEBCAM_PREFIX,
            VideoSourceDescriptor.NETWORK_PREFIX,
            VideoSourceDescriptor.SPOUT_PREFIX,
            VideoSourceDescriptor.SYPHON_PREFIX,
            VideoSourceDescriptor.SCREEN_CAPTURE_PREFIX
    };

    private VideoSourceDescriptors() {
    }

    /**
     * Parses a stored source id into a {@link VideoSourceDescriptor}.
     */
    public static VideoSourceDescriptor parse(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return VideoSourceDescriptor.testPattern();
        }

        String trimmed = sourceId.trim();
        if (trimmed.equalsIgnoreCase(VideoSourceDescriptor.TEST_PATTERN_ID)) {
            return VideoSourceDescriptor.testPattern();
        }

        for (String prefix : PREFIXES) {
            if (trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
                String payload = trimmed.substring(prefix.length()).trim();
                if (!payload.isEmpty()) {
                    return descriptorForPrefix(prefix, payload);
                }
            }
        }

        return VideoSourceDescriptor.testPattern();
    }

    public static boolean hasExplicitSource(String sourceId) {
        return sourceId != null && !sourceId.isBlank();
    }

    /**
     * Returns whether a persisted source id uses a known provider prefix or test pattern id.
     */
    public static boolean isRecognizedSourceId(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return false;
        }

        String trimmed = sourceId.trim();
        if (trimmed.equalsIgnoreCase(VideoSourceDescriptor.TEST_PATTERN_ID)) {
            return true;
        }

        for (String prefix : PREFIXES) {
            if (trimmed.regionMatches(true, 0, prefix, 0, prefix.length())) {
                return !trimmed.substring(prefix.length()).trim().isEmpty();
            }
        }

        return false;
    }

    private static VideoSourceDescriptor descriptorForPrefix(String prefix, String payload) {
        if (prefix.equalsIgnoreCase(VideoSourceDescriptor.NDI_PREFIX)) {
            return VideoSourceDescriptor.ndi(payload);
        }
        if (prefix.equalsIgnoreCase(VideoSourceDescriptor.FILE_PREFIX)) {
            return VideoSourceDescriptor.file(payload);
        }
        if (prefix.equalsIgnoreCase(VideoSourceDescriptor.GIF_PREFIX)) {
            return VideoSourceDescriptor.gif(payload);
        }
        if (prefix.equalsIgnoreCase(VideoSourceDescriptor.IMAGE_PREFIX)) {
            return VideoSourceDescriptor.image(payload);
        }
        if (prefix.equalsIgnoreCase(VideoSourceDescriptor.BROWSER_PREFIX)) {
            return VideoSourceDescriptor.browser(payload);
        }
        if (prefix.equalsIgnoreCase(VideoSourceDescriptor.WEBCAM_PREFIX)) {
            return VideoSourceDescriptor.webcam(payload);
        }
        if (prefix.equalsIgnoreCase(VideoSourceDescriptor.NETWORK_PREFIX)) {
            return VideoSourceDescriptor.network(payload);
        }
        if (prefix.equalsIgnoreCase(VideoSourceDescriptor.SPOUT_PREFIX)) {
            return VideoSourceDescriptor.spout(payload);
        }
        if (prefix.equalsIgnoreCase(VideoSourceDescriptor.SYPHON_PREFIX)) {
            return VideoSourceDescriptor.syphon(payload);
        }
        if (prefix.equalsIgnoreCase(VideoSourceDescriptor.SCREEN_CAPTURE_PREFIX)) {
            return VideoSourceDescriptor.screenCapture(payload);
        }
        return VideoSourceDescriptor.testPattern();
    }
}
