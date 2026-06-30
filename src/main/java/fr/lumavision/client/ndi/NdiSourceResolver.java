package fr.lumavision.client.ndi;

import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.config.ModConfig;
import fr.lumavision.video.VideoSourceDescriptor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Resolves which media source a merged wall should use.
 */
@OnlyIn(Dist.CLIENT)
public final class NdiSourceResolver {

    private NdiSourceResolver() {
    }

    public static VideoSourceDescriptor resolve(LedScreenBlockEntity originBlock) {
        if (!ModConfig.ENABLE_NDI.get() || !NdiRuntime.init()) {
            return VideoSourceDescriptor.testPattern();
        }

        if (originBlock.hasExplicitSourceId()) {
            VideoSourceDescriptor wallDescriptor = originBlock.getSourceDescriptor();
            if (wallDescriptor.isNdi()) {
                return wallDescriptor;
            }
        }

        String defaultSource = ModConfig.NDI_DEFAULT_SOURCE.get();
        if (defaultSource != null && !defaultSource.isBlank()) {
            return VideoSourceDescriptor.ndi(defaultSource.trim());
        }

        if (ModConfig.NDI_AUTO_SELECT_FIRST.get()) {
            String first = NdiDiscoveryService.getInstance().getFirstSourceName();
            if (first != null && !first.isBlank()) {
                return VideoSourceDescriptor.ndi(first);
            }
        }

        return VideoSourceDescriptor.testPattern();
    }
}
