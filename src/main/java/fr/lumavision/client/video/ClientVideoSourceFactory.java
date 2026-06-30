package fr.lumavision.client.video;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.client.ndi.NdiVideoSource;
import fr.lumavision.config.ModConfig;
import fr.lumavision.video.VideoSource;
import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceFactory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-side {@link VideoSourceFactory} implementation.
 */
@OnlyIn(Dist.CLIENT)
public final class ClientVideoSourceFactory implements VideoSourceFactory {

    public static final ClientVideoSourceFactory INSTANCE = new ClientVideoSourceFactory();

    private ClientVideoSourceFactory() {
    }

    @Override
    public VideoSource create(VideoSourceDescriptor descriptor, int targetWidth, int targetHeight) {
        if (descriptor.isNdi() && ModConfig.ENABLE_NDI.get()) {
            try {
                return new NdiVideoSource(descriptor.payload(), targetWidth, targetHeight);
            } catch (Throwable throwable) {
                LumaVisionMod.LOGGER.warn(
                        "NDI source '{}' unavailable, using test pattern",
                        descriptor.payload(),
                        throwable
                );
            }
        }
        return new TestPatternVideoSource(targetWidth, targetHeight);
    }
}
