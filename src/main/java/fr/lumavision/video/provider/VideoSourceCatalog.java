package fr.lumavision.video.provider;

import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.video.VideoSource;
import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceFactory;

import java.util.List;
import java.util.Optional;

/**
 * Central registry of {@link VideoSourceProvider} instances.
 * <p>
 * The configuration GUI and texture pipeline talk to the catalog only — never to NDI, Devolay,
 * or any other concrete backend.
 */
public interface VideoSourceCatalog extends VideoSourceFactory {

    List<VideoSourceProvider> getProviders();

    List<CatalogSourceEntry> getAllSources();

    Optional<VideoSourceProvider> providerFor(VideoSourceDescriptor descriptor);

    /**
     * Resolves which descriptor a merged wall should render.
     */
    VideoSourceDescriptor resolve(LedScreenBlockEntity originBlock);

    @Override
    VideoSource create(VideoSourceDescriptor descriptor, int targetWidth, int targetHeight);
}
