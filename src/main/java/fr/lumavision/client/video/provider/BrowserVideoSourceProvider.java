package fr.lumavision.client.video.provider;

import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BrowserVideoSourceProvider extends AbstractStubVideoSourceProvider {

    public static final BrowserVideoSourceProvider INSTANCE = new BrowserVideoSourceProvider();

    private BrowserVideoSourceProvider() {
        super(VideoSourceType.BROWSER, "browser", "Web Browser", VideoSourceDescriptor.BROWSER_PREFIX);
    }
}
