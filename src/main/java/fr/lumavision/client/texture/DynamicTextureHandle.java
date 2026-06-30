package fr.lumavision.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import fr.lumavision.LumaVisionMod;
import fr.lumavision.video.VideoFrame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * GPU-backed texture updated from {@link VideoFrame} data.
 * <p>
 * This is the last step before rendering — {@link fr.lumavision.client.render.ScreenRenderer}
 * only binds the returned {@link ResourceLocation}.
 */
public final class DynamicTextureHandle implements AutoCloseable {

    private DynamicTexture texture;
    private final ResourceLocation location;
    private int uploadedWidth;
    private int uploadedHeight;

    public DynamicTextureHandle(String textureId) {
        this.location = new ResourceLocation(LumaVisionMod.MOD_ID, "dynamic/" + textureId);
        this.texture = createTexture(1, 1);
        this.uploadedWidth = 1;
        this.uploadedHeight = 1;
        register();
    }

    public ResourceLocation location() {
        return location;
    }

    public void upload(VideoFrame frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        if (uploadedWidth != width || uploadedHeight != height) {
            recreateTexture(width, height);
        }

        NativeImage pixels = texture.getPixels();
        if (pixels == null
                || pixels.getWidth() != width
                || pixels.getHeight() != height) {
            return;
        }

        frame.writeTo(pixels);
        texture.upload();
    }

    private void recreateTexture(int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTextureManager().release(location);
        if (texture != null) {
            texture.close();
        }
        texture = createTexture(width, height);
        uploadedWidth = width;
        uploadedHeight = height;
        register();
    }

    private void register() {
        Minecraft.getInstance().getTextureManager().register(location, texture);
    }

    private static DynamicTexture createTexture(int width, int height) {
        NativeImage image = new NativeImage(width, height, false);
        image.fillRect(0, 0, width, height, 0xFF000000);
        return new DynamicTexture(image);
    }

    @Override
    public void close() {
        Minecraft minecraft = Minecraft.getInstance();
        Runnable cleanup = () -> {
            minecraft.getTextureManager().release(location);
            texture.close();
        };
        if (minecraft.isSameThread()) {
            cleanup.run();
        } else {
            minecraft.execute(cleanup);
        }
    }
}
