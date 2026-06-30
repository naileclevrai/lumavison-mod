package fr.lumavision.video;

/**
 * Identifies how a {@link VideoSource} produces frames.
 * <p>
 * Future types (FILE, IMAGE, URL, BROWSER) will plug in without renderer changes.
 */
public enum VideoSourceType {
    TEST_PATTERN,
    NDI
}
