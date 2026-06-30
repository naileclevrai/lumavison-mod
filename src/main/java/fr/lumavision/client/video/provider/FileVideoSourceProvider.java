package fr.lumavision.client.video.provider;

import fr.lumavision.video.VideoSourceDescriptor;
import fr.lumavision.video.VideoSourceType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class FileVideoSourceProvider extends AbstractStubVideoSourceProvider {

    public static final FileVideoSourceProvider INSTANCE = new FileVideoSourceProvider();

    private FileVideoSourceProvider() {
        super(VideoSourceType.FILE, "file", "Video File", VideoSourceDescriptor.FILE_PREFIX);
    }
}
