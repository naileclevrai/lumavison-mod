package fr.lumavision.video;

import com.mojang.blaze3d.platform.NativeImage;

import java.nio.ByteBuffer;

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
     * Copies BGRA/BGRX NDI data into this frame with optional downscaling (direct array write).
     */
    public void copyFromBgrx(ByteBuffer data, int srcW, int srcH, int lineStride, boolean hasAlpha) {
        int dstW = width;
        int dstH = height;
        int rowBytes = Math.max(lineStride, srcW * 4);
        int rowBase = 0;
        for (int y = 0; y < dstH; y++) {
            int srcY = y * srcH / dstH;
            int srcRowBase = srcY * rowBytes;
            if (srcRowBase + srcW * 4 > data.limit()) {
                break;
            }
            for (int x = 0; x < dstW; x++) {
                int srcX = x * srcW / dstW;
                int index = srcRowBase + srcX * 4;
                int b = data.get(index) & 0xFF;
                int g = data.get(index + 1) & 0xFF;
                int r = data.get(index + 2) & 0xFF;
                int a = hasAlpha ? data.get(index + 3) & 0xFF : 0xFF;
                pixels[rowBase + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            rowBase += dstW;
        }
    }

    /**
     * Copies RGBA/RGBX NDI data into this frame with optional downscaling (direct array write).
     */
    public void copyFromRgbx(ByteBuffer data, int srcW, int srcH, int lineStride, boolean hasAlpha) {
        int dstW = width;
        int dstH = height;
        int rowBytes = Math.max(lineStride, srcW * 4);
        int rowBase = 0;
        for (int y = 0; y < dstH; y++) {
            int srcY = y * srcH / dstH;
            int srcRowBase = srcY * rowBytes;
            if (srcRowBase + srcW * 4 > data.limit()) {
                break;
            }
            for (int x = 0; x < dstW; x++) {
                int srcX = x * srcW / dstW;
                int index = srcRowBase + srcX * 4;
                int r = data.get(index) & 0xFF;
                int g = data.get(index + 1) & 0xFF;
                int b = data.get(index + 2) & 0xFF;
                int a = hasAlpha ? data.get(index + 3) & 0xFF : 0xFF;
                pixels[rowBase + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            rowBase += dstW;
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
        int rowBase = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                target.setPixelRGBA(x, y, argbToAbgr(pixels[rowBase + x]));
            }
            rowBase += width;
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
