package fr.lumavision.client.video.provider;

import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class NetworkVideoSourceProvider extends AbstractStubVideoSourceProvider {

    public static final NetworkVideoSourceProvider INSTANCE = new NetworkVideoSourceProvider();

    private NetworkVideoSourceProvider() {
        super(VideoSourceType.NETWORK, "network", "Network Stream", VideoSourceDescriptor.NETWORK_PREFIX);
    }
}
