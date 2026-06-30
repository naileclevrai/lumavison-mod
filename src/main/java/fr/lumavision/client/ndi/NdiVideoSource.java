package fr.lumavision.client.ndi;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.config.ModConfig;
import fr.lumavision.video.VideoFrame;
import fr.lumavision.video.VideoSource;
import me.walkerknapp.devolay.DevolayFrameType;
import me.walkerknapp.devolay.DevolayReceiver;
import me.walkerknapp.devolay.DevolayVideoFrame;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link VideoSource} backed by an NDI receiver (Devolay).
 */
@OnlyIn(Dist.CLIENT)
public final class NdiVideoSource implements VideoSource {

    private final String sourceName;
    private final int targetWidth;
    private final int targetHeight;
    private final NdiFrameConverter converter = new NdiFrameConverter();
    private final AtomicReference<VideoFrame> displayFrame = new AtomicReference<>();
    private final VideoFrame placeholder;

    private DevolayReceiver receiver;
    private Thread captureThread;
    private volatile boolean running;

    public NdiVideoSource(String sourceName, int targetWidth, int targetHeight) {
        this.sourceName = sourceName;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.placeholder = new VideoFrame(Math.max(1, targetWidth), Math.max(1, targetHeight));
        this.placeholder.fill(0xFF101010);
        this.displayFrame.set(placeholder);
        startCapture();
    }

    @Override
    public int getWidth() {
        VideoFrame frame = displayFrame.get();
        return frame != null ? frame.getWidth() : targetWidth;
    }

    @Override
    public int getHeight() {
        VideoFrame frame = displayFrame.get();
        return frame != null ? frame.getHeight() : targetHeight;
    }

    @Override
    public void tick() {
    }

    @Override
    public VideoFrame getCurrentFrame() {
        VideoFrame frame = displayFrame.get();
        return frame != null ? frame : placeholder;
    }

    @Override
    public void dispose() {
        running = false;
        if (captureThread != null) {
            captureThread.interrupt();
            try {
                captureThread.join(1000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            captureThread = null;
        }
        if (receiver != null) {
            receiver.close();
            receiver = null;
        }
    }

    private void startCapture() {
        if (!NdiRuntime.init()) {
            throw new IllegalStateException("NDI runtime is not available");
        }

        receiver = NdiDiscoveryService.getInstance().openReceiverForSource(sourceName);
        if (receiver == null) {
            throw new IllegalStateException("NDI source not found: " + sourceName);
        }

        running = true;
        captureThread = new Thread(this::captureLoop, "LumaVision-NDI-" + sourceName);
        captureThread.setDaemon(true);
        captureThread.start();
        LumaVisionMod.LOGGER.info("NDI capture started for '{}'", sourceName);
    }

    private void captureLoop() {
        int timeout = ModConfig.NDI_RECEIVE_TIMEOUT_MS.get();
        while (running) {
            DevolayVideoFrame ndiFrame = new DevolayVideoFrame();
            try {
                DevolayFrameType type = receiver.receiveCapture(ndiFrame, null, null, timeout);
                if (type == DevolayFrameType.VIDEO) {
                    VideoFrame converted = converter.convert(ndiFrame, targetWidth, targetHeight);
                    displayFrame.set(converted);
                } else if (type == DevolayFrameType.ERROR) {
                    LumaVisionMod.LOGGER.warn("NDI connection lost for '{}'", sourceName);
                }
            } catch (Throwable throwable) {
                if (running) {
                    LumaVisionMod.LOGGER.error("NDI capture error for '{}'", sourceName, throwable);
                }
            } finally {
                ndiFrame.close();
            }
        }
    }
}
