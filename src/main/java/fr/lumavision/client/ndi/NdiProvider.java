package fr.lumavision.client.ndi;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.client.video.TestPatternVideoSource;
import fr.lumavision.config.ModConfig;
import fr.lumavision.video.VideoSource;
import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceType;
import fr.lumavision.video.provider.CatalogSourceEntry;
import fr.lumavision.video.provider.ProviderConfigOption;
import fr.lumavision.video.provider.ProviderConfigOptionType;
import fr.lumavision.video.provider.VideoSourceProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * NDI media provider backed by Devolay discovery and receivers.
 */
@OnlyIn(Dist.CLIENT)
public final class NdiProvider implements VideoSourceProvider {

    public static final NdiProvider INSTANCE = new NdiProvider();

    private NdiProvider() {
    }

    @Override
    public VideoSourceType type() {
        return VideoSourceType.NDI;
    }

    @Override
    public String providerId() {
        return "ndi";
    }

    @Override
    public String displayName() {
        return "NDI";
    }

    @Override
    public String sourceIdPrefix() {
        return VideoSourceDescriptor.NDI_PREFIX;
    }

    @Override
    public boolean isEnabled() {
        return ModConfig.ENABLE_NDI.get();
    }

    @Override
    public boolean isAvailable() {
        return isEnabled() && NdiRuntime.isAvailable();
    }

    @Override
    public boolean isImplemented() {
        return true;
    }

    @Override
    public void start() {
        if (isEnabled() && NdiRuntime.init()) {
            NdiDiscoveryService.getInstance().start();
        }
    }

    @Override
    public void stop() {
        NdiDiscoveryService.getInstance().shutdown();
    }

    @Override
    public boolean supports(VideoSourceDescriptor descriptor) {
        return descriptor.isNdi();
    }

    @Override
    public VideoSourceDescriptor descriptorFromPayload(String payload) {
        return VideoSourceDescriptor.ndi(payload);
    }

    @Override
    public List<CatalogSourceEntry> listSources() {
        if (!isAvailable()) {
            return List.of();
        }

        List<CatalogSourceEntry> entries = new ArrayList<>();
        for (NdiSourceInfo source : NdiDiscoveryService.getInstance().getDiscoveredSources()) {
            VideoSourceDescriptor descriptor = VideoSourceDescriptor.ndi(source.sourceName());
            entries.add(new CatalogSourceEntry(
                    providerId(),
                    descriptor,
                    source.sourceName(),
                    "NDI network source",
                    true
            ));
        }
        return List.copyOf(entries);
    }

    @Override
    @Nullable
    public VideoSourceDescriptor defaultDescriptor() {
        if (!isAvailable()) {
            return null;
        }

        String defaultSource = ModConfig.NDI_DEFAULT_SOURCE.get();
        if (defaultSource != null && !defaultSource.isBlank()) {
            return VideoSourceDescriptor.ndi(defaultSource.trim());
        }

        if (ModConfig.NDI_AUTO_SELECT_FIRST.get()) {
            String first = NdiDiscoveryService.getInstance().getFirstSourceName();
            if (first != null && !first.isBlank()) {
                return VideoSourceDescriptor.ndi(first);
            }
        }

        return null;
    }

    @Override
    public VideoSource create(VideoSourceDescriptor descriptor, int targetWidth, int targetHeight) {
        if (!supports(descriptor) || !isAvailable()) {
            throw new IllegalStateException("NDI provider cannot create source for " + descriptor.cacheKey());
        }

        try {
            return new NdiVideoSource(descriptor.payload(), targetWidth, targetHeight);
        } catch (Throwable throwable) {
            LumaVisionMod.LOGGER.warn(
                    "NDI source '{}' unavailable, using test pattern",
                    descriptor.payload(),
                    throwable
            );
            return new TestPatternVideoSource(targetWidth, targetHeight);
        }
    }

    @Override
    public List<ProviderConfigOption> getConfigOptions() {
        return List.of(
                new ProviderConfigOption(
                        "enableNdi",
                        "Enable NDI",
                        "Turns NDI input on for this client.",
                        ProviderConfigOptionType.BOOLEAN,
                        String.valueOf(ModConfig.ENABLE_NDI.get()),
                        true
                ),
                new ProviderConfigOption(
                        "ndiDefaultSource",
                        "Default NDI source",
                        "Fallback source name when a wall has no explicit binding.",
                        ProviderConfigOptionType.STRING,
                        ModConfig.NDI_DEFAULT_SOURCE.get(),
                        true
                ),
                new ProviderConfigOption(
                        "ndiAutoSelectFirst",
                        "Auto-select first source",
                        "Use the first discovered NDI source when nothing else is configured.",
                        ProviderConfigOptionType.BOOLEAN,
                        String.valueOf(ModConfig.NDI_AUTO_SELECT_FIRST.get()),
                        true
                ),
                new ProviderConfigOption(
                        "ndiReceiveTimeoutMs",
                        "Receive timeout (ms)",
                        "Per-frame NDI receive timeout.",
                        ProviderConfigOptionType.INTEGER,
                        String.valueOf(ModConfig.NDI_RECEIVE_TIMEOUT_MS.get()),
                        true
                ),
                new ProviderConfigOption(
                        "ndiDiscoveryIntervalMs",
                        "Discovery interval (ms)",
                        "How often to refresh the NDI source list.",
                        ProviderConfigOptionType.INTEGER,
                        String.valueOf(ModConfig.NDI_DISCOVERY_INTERVAL_MS.get()),
                        true
                )
        );
    }
}
