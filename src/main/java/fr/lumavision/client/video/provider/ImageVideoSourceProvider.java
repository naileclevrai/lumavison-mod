package fr.lumavision.client.video.provider;

import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ImageVideoSourceProvider extends AbstractStubVideoSourceProvider {

    public static final ImageVideoSourceProvider INSTANCE = new ImageVideoSourceProvider();

    private ImageVideoSourceProvider() {
        super(VideoSourceType.IMAGE, "image", "Static Image", VideoSourceDescriptor.IMAGE_PREFIX);
    }
}
