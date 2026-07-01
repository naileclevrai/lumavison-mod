package fr.lumavision.video;

import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Single frame of pixel data. Stored internally in NativeImage RGBA integer layout
 * ({@code 0xAABBGGRR}); public accessors use ARGB ({@code 0xAARRGGBB}).
 * <p>
 * This is the boundary between content producers ({@link VideoSource}) and the rendering layer.
 */
public final class VideoFrame {

    private final int width;
    private final int height;
    private final int[] pixels;
    private long revision;
    private int mapSrcW;
    private int mapSrcH;
    private int mapStride;
    private int[] srcXOffsets;
    private int[] srcRowOffsets;

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
        return nativeRgbaToArgb(pixels[index(x, y)]);
    }

    public void setArgb(int x, int y, int argb) {
        pixels[index(x, y)] = argbToNativeRgba(argb);
    }

    public void fill(int argb) {
        int nativeRgba = argbToNativeRgba(argb);
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = nativeRgba;
        }
        markDirty();
    }

    public long getRevision() {
        return revision;
    }

    public void markDirty() {
        revision++;
    }

    /**
     * Copies BGRA/BGRX NDI data into this frame with optional downscaling (direct array write).
     */
    public void copyFromBgrx(ByteBuffer data, int srcW, int srcH, int lineStride, boolean hasAlpha) {
        int dstW = width;
        int dstH = height;
        ensureScaleMaps(srcW, srcH, lineStride);
        int rowBase = 0;
        long baseAddress = bufferBaseAddress(data);
        for (int y = 0; y < dstH; y++) {
            int srcRowBase = srcRowOffsets[y];
            if (srcRowBase + srcW * 4 > data.limit()) {
                break;
            }
            for (int x = 0; x < dstW; x++) {
                int index = srcRowBase + srcXOffsets[x];
                int b;
                int g;
                int r;
                int a;
                if (baseAddress != 0L) {
                    long address = baseAddress + index;
                    b = MemoryUtil.memGetByte(address) & 0xFF;
                    g = MemoryUtil.memGetByte(address + 1L) & 0xFF;
                    r = MemoryUtil.memGetByte(address + 2L) & 0xFF;
                    a = hasAlpha ? MemoryUtil.memGetByte(address + 3L) & 0xFF : 0xFF;
                } else {
                    b = data.get(index) & 0xFF;
                    g = data.get(index + 1) & 0xFF;
                    r = data.get(index + 2) & 0xFF;
                    a = hasAlpha ? data.get(index + 3) & 0xFF : 0xFF;
                }
                pixels[rowBase + x] = (a << 24) | (b << 16) | (g << 8) | r;
            }
            rowBase += dstW;
        }
        markDirty();
    }

    /**
     * Copies RGBA/RGBX NDI data into this frame with optional downscaling (direct array write).
     */
    public void copyFromRgbx(ByteBuffer data, int srcW, int srcH, int lineStride, boolean hasAlpha) {
        int dstW = width;
        int dstH = height;
        ensureScaleMaps(srcW, srcH, lineStride);
        int rowBase = 0;
        long baseAddress = bufferBaseAddress(data);
        for (int y = 0; y < dstH; y++) {
            int srcRowBase = srcRowOffsets[y];
            if (srcRowBase + srcW * 4 > data.limit()) {
                break;
            }
            for (int x = 0; x < dstW; x++) {
                int index = srcRowBase + srcXOffsets[x];
                if (baseAddress != 0L) {
                    int nativeRgba = MemoryUtil.memGetInt(baseAddress + index);
                    pixels[rowBase + x] = hasAlpha ? nativeRgba : nativeRgba | 0xFF000000;
                } else {
                    int r = data.get(index) & 0xFF;
                    int g = data.get(index + 1) & 0xFF;
                    int b = data.get(index + 2) & 0xFF;
                    int a = hasAlpha ? data.get(index + 3) & 0xFF : 0xFF;
                    pixels[rowBase + x] = (a << 24) | (b << 16) | (g << 8) | r;
                }
            }
            rowBase += dstW;
        }
        markDirty();
    }

    /**
     * Copies this frame into a {@link NativeImage} for upload to a Minecraft dynamic texture.
     * NativeImage expects ABGR byte order per pixel.
     */
    public void writeTo(NativeImage target) {
        if (target.getWidth() != width || target.getHeight() != height) {
            throw new IllegalArgumentException("Target image size mismatch");
        }
        if (NativeImageAccess.copyNativeRgbaTo(target, pixels)) {
            return;
        }
        for (int y = 0, rowBase = 0; y < height; y++, rowBase += width) {
            for (int x = 0; x < width; x++) {
                target.setPixelRGBA(x, y, pixels[rowBase + x]);
            }
        }
    }

    private int index(int x, int y) {
        return y * width + x;
    }

    private static int argbToNativeRgba(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static int nativeRgbaToArgb(int nativeRgba) {
        int a = (nativeRgba >>> 24) & 0xFF;
        int b = (nativeRgba >>> 16) & 0xFF;
        int g = (nativeRgba >>> 8) & 0xFF;
        int r = nativeRgba & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void ensureScaleMaps(int srcW, int srcH, int lineStride) {
        int rowBytes = Math.max(lineStride, srcW * 4);
        if (srcXOffsets != null
                && srcRowOffsets != null
                && mapSrcW == srcW
                && mapSrcH == srcH
                && mapStride == rowBytes) {
            return;
        }
        mapSrcW = srcW;
        mapSrcH = srcH;
        mapStride = rowBytes;
        srcXOffsets = new int[width];
        srcRowOffsets = new int[height];
        for (int x = 0; x < width; x++) {
            srcXOffsets[x] = (x * srcW / width) * 4;
        }
        for (int y = 0; y < height; y++) {
            srcRowOffsets[y] = (y * srcH / height) * rowBytes;
        }
    }

    private static long bufferBaseAddress(ByteBuffer data) {
        if (data == null || !data.isDirect()) {
            return 0L;
        }
        try {
            return MemoryUtil.memAddress(data) - data.position();
        } catch (RuntimeException ignored) {
            return 0L;
        }
    }
}
