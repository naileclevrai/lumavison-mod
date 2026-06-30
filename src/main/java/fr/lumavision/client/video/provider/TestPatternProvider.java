package fr.lumavision.client.video.provider;

import fr.lumavision.client.video.TestPatternVideoSource;
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
 * Built-in fallback pattern used when no live source is available.
 */
@OnlyIn(Dist.CLIENT)
public final class TestPatternProvider implements VideoSourceProvider {

    public static final TestPatternProvider INSTANCE = new TestPatternProvider();

    private TestPatternProvider() {
    }

    @Override
    public VideoSourceType type() {
        return VideoSourceType.TEST_PATTERN;
    }

    @Override
    public String providerId() {
        return "test";
    }

    @Override
    public String displayName() {
        return "Test Pattern";
    }

    @Override
    public String sourceIdPrefix() {
        return "";
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
    public boolean isImplemented() {
        return true;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean supports(VideoSourceDescriptor descriptor) {
        return descriptor.isTestPattern();
    }

    @Override
    public VideoSourceDescriptor descriptorFromPayload(String payload) {
        return VideoSourceDescriptor.testPattern();
    }

    @Override
    public List<CatalogSourceEntry> listSources() {
        VideoSourceDescriptor descriptor = VideoSourceDescriptor.testPattern();
        return List.of(new CatalogSourceEntry(
                providerId(),
                descriptor,
                "Test Pattern",
                "Built-in calibration pattern",
                true
        ));
    }

    @Override
    @Nullable
    public VideoSourceDescriptor defaultDescriptor() {
        return null;
    }

    @Override
    public VideoSource create(VideoSourceDescriptor descriptor, int targetWidth, int targetHeight) {
        return new TestPatternVideoSource(targetWidth, targetHeight);
    }

    @Override
    public List<ProviderConfigOption> getConfigOptions() {
        return List.of();
    }
}
