package fr.lumavision.client.relay;

import fr.lumavision.client.video.provider.TestPatternProvider;
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
 * Provides relay-backed video sources for multiplayer viewers.
 */
@OnlyIn(Dist.CLIENT)
public final class RelayVideoSourceProvider implements VideoSourceProvider {

    public static final RelayVideoSourceProvider INSTANCE = new RelayVideoSourceProvider();
    private static final String PROVIDER_ID = "relay";

    private RelayVideoSourceProvider() {
    }

    @Override
    public VideoSourceType type() {
        return VideoSourceType.RELAY;
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public String displayName() {
        return "Server Relay";
    }

    @Override
    public String sourceIdPrefix() {
        return VideoSourceDescriptor.RELAY_PREFIX;
    }

    @Override
    public boolean isImplemented() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean supports(VideoSourceDescriptor descriptor) {
        return descriptor.isRelay();
    }

    @Override
    public VideoSourceDescriptor descriptorFromPayload(String payload) {
        return VideoSourceDescriptor.relay(VideoSourceDescriptor.parseRelayOrigin(payload));
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
        if (!descriptor.isRelay()) {
            return TestPatternProvider.INSTANCE.create(VideoSourceDescriptor.testPattern(), targetWidth, targetHeight);
        }
        return new RelayedVideoSource(VideoSourceDescriptor.parseRelayOrigin(descriptor.payload()), targetWidth, targetHeight);
    }

    @Override
    public List<ProviderConfigOption> getConfigOptions() {
        return List.of();
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void refreshSources() {
    }
}
