package fr.lumavision.client.ndi;

import fr.lumavision.LumaVisionMod;
import me.walkerknapp.devolay.DevolayFrameFormatType;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import me.walkerknapp.devolay.DevolaySender;
import me.walkerknapp.devolay.DevolayVideoFrame;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns a single {@link DevolaySender} and streams frames for one camera on a dedicated daemon thread,
 * paced to the camera's configured FPS. The frame content is currently the {@link CameraTestPattern}
 * (M2a); M2b will replace {@link #render} with an offscreen world-view GPU readback handed in from the
 * render thread. The sender's NDI name is fixed at construction — a name change means the manager
 * disposes and recreates the sender.
 */
@OnlyIn(Dist.CLIENT)
final class CameraNdiSender {

    private final DevolaySender sender;
    private final DevolayVideoFrame frame = new DevolayVideoFrame();
    private final Thread thread;

    /** A captured world-view frame (BGRA, top-down) handed from the render thread. */
    private record Frame(byte[] data, int w, int h) {
    }

    private final AtomicReference<Frame> capturedFrame = new AtomicReference<>();

    private volatile CameraSnapshot snapshot;
    private volatile boolean running = true;

    private byte[] scratch;
    private ByteBuffer direct;
    private int bufW;
    private int bufH;
    private long frameIndex;
    private boolean sendErrorLogged;

    CameraNdiSender(CameraSnapshot initial) {
        this.snapshot = initial;
        this.sender = new DevolaySender(initial.name());
        this.thread = new Thread(this::loop, "LumaVision-NDI-Send-" + initial.name());
        this.thread.setDaemon(true);
        this.thread.start();
        LumaVisionMod.LOGGER.info("NDI camera source live: '{}' ({}x{}@{})",
                initial.name(), initial.width(), initial.height(), initial.fps());
    }

    String name() {
        return snapshot.name();
    }

    CameraSnapshot snapshot() {
        return snapshot;
    }

    void update(CameraSnapshot next) {
        this.snapshot = next;
    }

    /** Push a freshly captured world-view frame (BGRA, top-down) from the render thread. */
    void submitFrame(byte[] bgra, int w, int h) {
        capturedFrame.set(new Frame(bgra, w, h));
    }

    private void loop() {
        while (running) {
            CameraSnapshot current = snapshot;
            long startNs = System.nanoTime();
            try {
                render(current);
                sender.sendVideoFrame(frame);
            } catch (Throwable t) {
                if (!sendErrorLogged) {
                    sendErrorLogged = true;
                    LumaVisionMod.LOGGER.error("NDI send failed for '{}': {}", current.name(), t.toString());
                }
            }
            frameIndex++;

            long targetNs = 1_000_000_000L / Math.max(1, current.fps());
            long sleepMs = (targetNs - (System.nanoTime() - startNs)) / 1_000_000L;
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void render(CameraSnapshot s) {
        // Frame size follows the captured world view (rendered at the game framebuffer size); the
        // configured resolution only sizes the test-pattern fallback until a real frame arrives.
        Frame captured = capturedFrame.get();
        int w = captured != null ? captured.w() : Math.max(2, s.width());
        int h = captured != null ? captured.h() : Math.max(2, s.height());
        if (direct == null || bufW != w || bufH != h) {
            bufW = w;
            bufH = h;
            scratch = new byte[w * h * 4];
            direct = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
            frame.setResolution(w, h);
            frame.setFourCCType(DevolayFrameFourCCType.BGRA);
            frame.setFormatType(DevolayFrameFormatType.PROGRESSIVE);
            frame.setLineStride(w * 4);
            frame.setAspectRatio((float) w / h);
        }
        frame.setFrameRate(Math.max(1, s.fps()), 1);

        direct.clear();
        if (captured != null && captured.data().length >= w * h * 4) {
            direct.put(captured.data(), 0, w * h * 4); // live world view
        } else {
            CameraTestPattern.fill(scratch, w, h, s, frameIndex); // fallback until a frame is captured
            direct.put(scratch);
        }
        direct.flip();
        frame.setData(direct);
    }

    void close() {
        running = false;
        thread.interrupt();
        try {
            thread.join(250L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        try {
            sender.close();
        } catch (Throwable ignored) {
        }
        try {
            frame.close();
        } catch (Throwable ignored) {
        }
    }
}
