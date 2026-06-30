package fr.lumavision.video;

/**
 * Identifies how a {@link VideoSource} produces frames.
 * <p>
 * Each value maps to a {@link fr.lumavision.video.provider.VideoSourceProvider} on the client.
 */
public enum VideoSourceType {
    TEST_PATTERN,
    NDI,
    FILE,
    GIF,
    IMAGE,
    BROWSER,
    WEBCAM,
    NETWORK,
    SPOUT,
    SYPHON,
    SCREEN_CAPTURE
}
