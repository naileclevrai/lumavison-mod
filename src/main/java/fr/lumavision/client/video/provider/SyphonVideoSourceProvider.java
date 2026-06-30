package fr.lumavision.client.video.provider;

import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SyphonVideoSourceProvider extends AbstractStubVideoSourceProvider {

    public static final SyphonVideoSourceProvider INSTANCE = new SyphonVideoSourceProvider();

    private SyphonVideoSourceProvider() {
        super(VideoSourceType.SYPHON, "syphon", "Syphon", VideoSourceDescriptor.SYPHON_PREFIX);
    }
}
