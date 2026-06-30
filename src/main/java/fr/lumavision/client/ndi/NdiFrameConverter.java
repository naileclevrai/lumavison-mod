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
            frame.copyFromBgrx(data, sourceWidth, sourceHeight, ndiFrame.getLineStride(), fourCc == DevolayFrameFourCCType.BGRA);
        } else if (fourCc == DevolayFrameFourCCType.RGBA || fourCc == DevolayFrameFourCCType.RGBX) {
            frame.copyFromRgbx(data, sourceWidth, sourceHeight, ndiFrame.getLineStride(), fourCc == DevolayFrameFourCCType.RGBA);
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
}
