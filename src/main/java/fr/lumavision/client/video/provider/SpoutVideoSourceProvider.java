package fr.lumavision.client.video.provider;

import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SpoutVideoSourceProvider extends AbstractStubVideoSourceProvider {

    public static final SpoutVideoSourceProvider INSTANCE = new SpoutVideoSourceProvider();

    private SpoutVideoSourceProvider() {
        super(VideoSourceType.SPOUT, "spout", "Spout", VideoSourceDescriptor.SPOUT_PREFIX);
    }
}
