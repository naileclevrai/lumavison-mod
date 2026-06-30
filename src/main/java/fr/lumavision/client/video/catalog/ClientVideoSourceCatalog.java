package fr.lumavision.client.video.catalog;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.client.ndi.NdiProvider;
import fr.lumavision.client.video.provider.BrowserVideoSourceProvider;
import fr.lumavision.client.video.provider.FileVideoSourceProvider;
import fr.lumavision.client.video.provider.GifVideoSourceProvider;
import fr.lumavision.client.video.provider.ImageVideoSourceProvider;
import fr.lumavision.client.video.provider.NetworkVideoSourceProvider;
import fr.lumavision.client.video.provider.ScreenCaptureVideoSourceProvider;
import fr.lumavision.client.video.provider.SpoutVideoSourceProvider;
import fr.lumavision.client.video.provider.SyphonVideoSourceProvider;
import fr.lumavision.client.video.provider.TestPatternProvider;
import fr.lumavision.client.video.provider.WebcamVideoSourceProvider;
import fr.lumavision.video.VideoSource;
import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceDescriptors;
import fr.lumavision.video.provider.CatalogSourceEntry;
import fr.lumavision.video.provider.VideoSourceCatalog;
import fr.lumavision.video.provider.VideoSourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Client-side aggregator of all {@link VideoSourceProvider} backends.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientVideoSourceCatalog implements VideoSourceCatalog {

    public static final ClientVideoSourceCatalog INSTANCE = new ClientVideoSourceCatalog();

    private final List<VideoSourceProvider> providers = new ArrayList<>();
    private boolean started;

    private ClientVideoSourceCatalog() {
        register(NdiProvider.INSTANCE);
        register(FileVideoSourceProvider.INSTANCE);
        register(GifVideoSourceProvider.INSTANCE);
        register(ImageVideoSourceProvider.INSTANCE);
        register(BrowserVideoSourceProvider.INSTANCE);
        register(WebcamVideoSourceProvider.INSTANCE);
        register(NetworkVideoSourceProvider.INSTANCE);
        register(SpoutVideoSourceProvider.INSTANCE);
        register(SyphonVideoSourceProvider.INSTANCE);
        register(ScreenCaptureVideoSourceProvider.INSTANCE);
        register(TestPatternProvider.INSTANCE);
    }

    public void register(VideoSourceProvider provider) {
        providers.add(provider);
    }

    public synchronized void start() {
        if (started) {
            return;
        }
        for (VideoSourceProvider provider : providers) {
            try {
                provider.start();
            } catch (Throwable throwable) {
                LumaVisionMod.LOGGER.error("Failed to start provider {}", provider.providerId(), throwable);
            }
        }
        started = true;
        LumaVisionMod.LOGGER.info("Video source catalog started ({} providers)", providers.size());
    }

    public synchronized void shutdown() {
        if (!started) {
            return;
        }
        for (VideoSourceProvider provider : providers) {
            try {
                provider.stop();
            } catch (Throwable throwable) {
                LumaVisionMod.LOGGER.warn("Failed to stop provider {}", provider.providerId(), throwable);
            }
        }
        started = false;
    }

    @Override
    public List<VideoSourceProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }

    @Override
    public List<CatalogSourceEntry> getAllSources() {
        List<CatalogSourceEntry> entries = new ArrayList<>();
        for (VideoSourceProvider provider : providers) {
            if (!provider.isImplemented()) {
                continue;
            }
            entries.addAll(provider.listSources());
        }
        return List.copyOf(entries);
    }

    @Override
    public Optional<VideoSourceProvider> providerFor(VideoSourceDescriptor descriptor) {
        return providers.stream()
                .filter(provider -> provider.supports(descriptor))
                .findFirst();
    }

    @Override
    public Optional<VideoSourceProvider> findProviderById(String providerId) {
        return providers.stream()
                .filter(provider -> provider.providerId().equals(providerId))
                .findFirst();
    }

    @Override
    public List<CatalogSourceEntry> listSourcesForProvider(String providerId) {
        return findProviderById(providerId)
                .map(VideoSourceProvider::listSources)
                .orElseGet(List::of);
    }

    @Override
    public void refreshProvider(String providerId) {
        findProviderById(providerId).ifPresent(VideoSourceProvider::refreshSources);
    }

    @Override
    public VideoSourceDescriptor resolve(LedScreenBlockEntity originBlock) {
        if (originBlock.hasExplicitSourceId()) {
            VideoSourceDescriptor explicit = VideoSourceDescriptors.parse(originBlock.getSourceId());
            Optional<VideoSourceProvider> provider = providerFor(explicit);
            if (provider.isPresent() && provider.get().supports(explicit)) {
                return explicit;
            }
        }

        for (VideoSourceProvider provider : providers) {
            if (!provider.isEnabled() || !provider.isImplemented()) {
                continue;
            }
            VideoSourceDescriptor fallback = provider.defaultDescriptor();
            if (fallback != null && provider.isAvailable()) {
                return fallback;
            }
        }

        return VideoSourceDescriptor.testPattern();
    }

    @Override
    public VideoSource create(VideoSourceDescriptor descriptor, int targetWidth, int targetHeight) {
        Optional<VideoSourceProvider> provider = providerFor(descriptor);
        if (provider.isPresent()) {
            VideoSourceProvider resolved = provider.get();
            if (resolved.isImplemented() && resolved.isAvailable()) {
                return resolved.create(descriptor, targetWidth, targetHeight);
            }
        }
        return TestPatternProvider.INSTANCE.create(VideoSourceDescriptor.testPattern(), targetWidth, targetHeight);
    }
}
