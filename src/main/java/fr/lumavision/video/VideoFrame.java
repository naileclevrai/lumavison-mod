package fr.lumavision.video;

import com.mojang.blaze3d.platform.NativeImage;

/**
 * Single frame of pixel data in ARGB format ({@code 0xAARRGGBB}).
 * <p>
 * This is the boundary between content producers ({@link VideoSource}) and the rendering layer.
 */
public final class VideoFrame {

    private final int width;
    private final int height;
    private final int[] pixels;

    public VideoFrame(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Frame dimensions must be positive");
        }
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getArgb(int x, int y) {
        return pixels[index(x, y)];
    }

    public void setArgb(int x, int y, int argb) {
        pixels[index(x, y)] = argb;
    }

    public void fill(int argb) {
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = argb;
        }
    }

    /**
     * Copies this frame into a {@link NativeImage} for upload to a Minecraft dynamic texture.
     * NativeImage expects ABGR byte order per pixel.
     */
    public void writeTo(NativeImage target) {
        if (target.getWidth() != width || target.getHeight() != height) {
            throw new IllegalArgumentException("Target image size mismatch");
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                target.setPixelRGBA(x, y, argbToAbgr(getArgb(x, y)));
            }
        }
    }

    private int index(int x, int y) {
        return y * width + x;
    }

    private static int argbToAbgr(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }
}
