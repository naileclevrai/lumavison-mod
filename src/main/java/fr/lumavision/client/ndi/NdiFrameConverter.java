package fr.lumavision.client.ndi;

import fr.lumavision.video.VideoFrame;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import me.walkerknapp.devolay.DevolayVideoFrame;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.nio.ByteBuffer;

/**
 * Converts Devolay video frames into {@link VideoFrame} buffers with optional downscaling.
 */
@OnlyIn(Dist.CLIENT)
public final class NdiFrameConverter {

    private VideoFrame output;

    public VideoFrame convert(DevolayVideoFrame ndiFrame, int targetWidth, int targetHeight) {
        int sourceWidth = ndiFrame.getXResolution();
        int sourceHeight = ndiFrame.getYResolution();
        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return ensureOutput(targetWidth, targetHeight);
        }

        VideoFrame frame = ensureOutput(targetWidth, targetHeight);
        ByteBuffer data = ndiFrame.getData();
        if (data == null) {
            frame.fill(0xFF000000);
            return frame;
        }

        DevolayFrameFourCCType fourCc = ndiFrame.getFourCCType();
        if (fourCc == DevolayFrameFourCCType.BGRA || fourCc == DevolayFrameFourCCType.BGRX) {
            copyBgrx(data, sourceWidth, sourceHeight, ndiFrame.getLineStride(), frame, fourCc == DevolayFrameFourCCType.BGRA);
        } else if (fourCc == DevolayFrameFourCCType.RGBA || fourCc == DevolayFrameFourCCType.RGBX) {
            copyRgbx(data, sourceWidth, sourceHeight, ndiFrame.getLineStride(), frame, fourCc == DevolayFrameFourCCType.RGBA);
        } else {
            frame.fill(0xFF202020);
        }
        return frame;
    }

    private VideoFrame ensureOutput(int width, int height) {
        if (output == null || output.getWidth() != width || output.getHeight() != height) {
            output = new VideoFrame(width, height);
        }
        return output;
    }

    private static void copyBgrx(ByteBuffer data, int srcW, int srcH, int lineStride, VideoFrame dest, boolean hasAlpha) {
        int dstW = dest.getWidth();
        int dstH = dest.getHeight();
        int rowBytes = Math.max(lineStride, srcW * 4);
        for (int y = 0; y < dstH; y++) {
            int srcY = y * srcH / dstH;
            int rowBase = srcY * rowBytes;
            if (rowBase + srcW * 4 > data.limit()) {
                break;
            }
            for (int x = 0; x < dstW; x++) {
                int srcX = x * srcW / dstW;
                int index = rowBase + srcX * 4;
                int b = data.get(index) & 0xFF;
                int g = data.get(index + 1) & 0xFF;
                int r = data.get(index + 2) & 0xFF;
                int a = hasAlpha ? data.get(index + 3) & 0xFF : 0xFF;
                dest.setArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }

    private static void copyRgbx(ByteBuffer data, int srcW, int srcH, int lineStride, VideoFrame dest, boolean hasAlpha) {
        int dstW = dest.getWidth();
        int dstH = dest.getHeight();
        int rowBytes = Math.max(lineStride, srcW * 4);
        for (int y = 0; y < dstH; y++) {
            int srcY = y * srcH / dstH;
            int rowBase = srcY * rowBytes;
            if (rowBase + srcW * 4 > data.limit()) {
                break;
            }
            for (int x = 0; x < dstW; x++) {
                int srcX = x * srcW / dstW;
                int index = rowBase + srcX * 4;
                int r = data.get(index) & 0xFF;
                int g = data.get(index + 1) & 0xFF;
                int b = data.get(index + 2) & 0xFF;
                int a = hasAlpha ? data.get(index + 3) & 0xFF : 0xFF;
                dest.setArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }
}
