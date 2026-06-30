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

    private final DynamicTexture texture;
    private final ResourceLocation location;
    private int uploadedWidth;
    private int uploadedHeight;

    public DynamicTextureHandle(String textureId) {
        NativeImage image = new NativeImage(1, 1, false);
        image.setPixelRGBA(0, 0, 0xFF000000);
        this.texture = new DynamicTexture(image);
        this.location = new ResourceLocation(LumaVisionMod.MOD_ID, "dynamic/" + textureId);
        this.uploadedWidth = 1;
        this.uploadedHeight = 1;
        Minecraft.getInstance().getTextureManager().register(location, texture);
    }

    public ResourceLocation location() {
        return location;
    }

    public void upload(VideoFrame frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();

        NativeImage pixels = texture.getPixels();
        if (pixels == null || pixels.getWidth() != width || pixels.getHeight() != height) {
            if (pixels != null) {
                pixels.close();
            }
            texture.setPixels(new NativeImage(width, height, false));
            pixels = texture.getPixels();
            uploadedWidth = width;
            uploadedHeight = height;
        }

        if (pixels == null) {
            return;
        }

        frame.writeTo(pixels);
        texture.upload();
    }

    @Override
    public void close() {
        Minecraft.getInstance().getTextureManager().release(location);
        texture.close();
    }
}
