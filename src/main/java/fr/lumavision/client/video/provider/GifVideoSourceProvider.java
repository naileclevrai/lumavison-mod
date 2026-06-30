package fr.lumavision.client.video.provider;

import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GifVideoSourceProvider extends AbstractStubVideoSourceProvider {

    public static final GifVideoSourceProvider INSTANCE = new GifVideoSourceProvider();

    private GifVideoSourceProvider() {
        super(VideoSourceType.GIF, "gif", "Animated GIF", VideoSourceDescriptor.GIF_PREFIX);
    }
}
