package fr.lumavision.client.video.provider;

import fr.lumavision.video.VideoSource;
import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceType;
import fr.lumavision.video.provider.CatalogSourceEntry;
import fr.lumavision.video.provider.ProviderConfigOption;
import fr.lumavision.video.provider.VideoSourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base class for planned providers that are registered in the catalog but not implemented yet.
 */
@OnlyIn(Dist.CLIENT)
public abstract class AbstractStubVideoSourceProvider implements VideoSourceProvider {

    private final VideoSourceType type;
    private final String providerId;
    private final String displayName;
    private final String sourceIdPrefix;

    protected AbstractStubVideoSourceProvider(
            VideoSourceType type,
            String providerId,
            String displayName,
            String sourceIdPrefix
    ) {
        this.type = type;
        this.providerId = providerId;
        this.displayName = displayName;
        this.sourceIdPrefix = sourceIdPrefix;
    }

    @Override
    public VideoSourceType type() {
        return type;
    }

    @Override
    public String providerId() {
        return providerId;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String sourceIdPrefix() {
        return sourceIdPrefix;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean isImplemented() {
        return false;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean supports(VideoSourceDescriptor descriptor) {
        return descriptor.type() == type;
    }

    @Override
    public VideoSourceDescriptor descriptorFromPayload(String payload) {
        return new VideoSourceDescriptor(type, payload == null ? "" : payload.trim());
    }

    @Override
    public List<CatalogSourceEntry> listSources() {
        return List.of();
    }

    @Override
    @Nullable
    public VideoSourceDescriptor defaultDescriptor() {
        return null;
    }

    @Override
    public VideoSource create(VideoSourceDescriptor descriptor, int targetWidth, int targetHeight) {
        throw new UnsupportedOperationException(displayName + " provider is not implemented yet");
    }

    @Override
    public List<ProviderConfigOption> getConfigOptions() {
        return List.of();
    }
}
