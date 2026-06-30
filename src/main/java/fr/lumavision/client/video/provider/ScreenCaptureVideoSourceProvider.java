package fr.lumavision.client.video.provider;

import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ScreenCaptureVideoSourceProvider extends AbstractStubVideoSourceProvider {

    public static final ScreenCaptureVideoSourceProvider INSTANCE = new ScreenCaptureVideoSourceProvider();

    private ScreenCaptureVideoSourceProvider() {
        super(
                VideoSourceType.SCREEN_CAPTURE,
                "capture",
                "Screen Capture",
                VideoSourceDescriptor.SCREEN_CAPTURE_PREFIX
        );
    }
}
