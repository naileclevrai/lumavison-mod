package fr.lumavision.video;

/**
 * Creates {@link VideoSource} instances from descriptors.
 * <p>
 * Client implementations live in {@code fr.lumavision.client.video}.
 */
public interface VideoSourceFactory {

    VideoSource create(VideoSourceDescriptor descriptor, int targetWidth, int targetHeight);
}
