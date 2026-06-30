package fr.lumavision.video;

/**
 * Creates {@link VideoSource} instances from descriptors.
 * <p>
 * Client implementations are aggregated by {@link fr.lumavision.video.provider.VideoSourceCatalog}.
 */
public interface VideoSourceFactory {

    VideoSource create(VideoSourceDescriptor descriptor, int targetWidth, int targetHeight);
}
