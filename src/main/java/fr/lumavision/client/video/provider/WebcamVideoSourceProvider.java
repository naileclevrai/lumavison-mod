package fr.lumavision.client.video.provider;

import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class WebcamVideoSourceProvider extends AbstractStubVideoSourceProvider {

    public static final WebcamVideoSourceProvider INSTANCE = new WebcamVideoSourceProvider();

    private WebcamVideoSourceProvider() {
        super(VideoSourceType.WEBCAM, "webcam", "Webcam", VideoSourceDescriptor.WEBCAM_PREFIX);
    }
}
