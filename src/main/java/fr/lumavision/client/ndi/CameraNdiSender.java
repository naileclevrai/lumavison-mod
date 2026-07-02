package fr.lumavision.client.ndi;

import fr.lumavision.LumaVisionMod;
import me.walkerknapp.devolay.DevolayFrameFormatType;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import me.walkerknapp.devolay.DevolayFrameType;
import me.walkerknapp.devolay.DevolayMetadataFrame;
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

    // --- NDI PTZ control (commands received from an NDI receiver) ---
    private final DevolayMetadataFrame metadataIn = new DevolayMetadataFrame();
    /** Latest absolute PTZ target {pan°, tilt°, fov°} to push to the server, or null if none pending. */
    private final AtomicReference<float[]> pendingPtz = new AtomicReference<>();
    private boolean ptzActive;
    private float ptzPan;
    private float ptzTilt;
    private float ptzFov;
    private float panSpeed;
    private float tiltSpeed;
    private float zoomSpeed;

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
        advertisePtzSupport();
        this.thread = new Thread(this::loop, "LumaVision-NDI-Send-" + initial.name());
        this.thread.setDaemon(true);
        this.thread.start();
        LumaVisionMod.LOGGER.info("NDI camera source live: '{}' ({}x{}@{})",
                initial.name(), initial.width(), initial.height(), initial.fps());
    }

    /** Tells connecting NDI receivers this source accepts PTZ control. */
    private void advertisePtzSupport() {
        try (DevolayMetadataFrame capability = new DevolayMetadataFrame()) {
            capability.setData("<ndi_capabilities ntk_ptz=\"true\"/>");
            sender.addConnectionMetadata(capability);
        } catch (Throwable ignored) {
        }
    }

    /** Client thread polls this each tick; returns a pending absolute PTZ target {pan,tilt,fov} or null. */
    float[] pollPtz() {
        return pendingPtz.getAndSet(null);
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
        capturedFrame.set(new Frame(bgra.clone(), w, h));
    }

    private void loop() {
        while (running) {
            CameraSnapshot current = snapshot;
            long startNs = System.nanoTime();
            try {
                render(current);
                sender.sendVideoFrame(frame);
                drainPtz(current);
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

    /** Captures PTZ metadata from NDI receivers, integrates speeds, and queues an absolute target. */
    private void drainPtz(CameraSnapshot s) {
        float dt = 1.0F / Math.max(1, s.fps());
        if (!ptzActive) {
            // Track the current (server-authoritative) values until the receiver takes control.
            ptzPan = s.pan();
            ptzTilt = s.pitch();
            ptzFov = s.fov();
        }

        boolean got = false;
        while (sender.sendCapture(metadataIn, 0) == DevolayFrameType.METADATA) {
            String xml = metadataIn.getData();
            if (xml != null && parsePtz(xml)) {
                got = true;
            }
        }

        if (panSpeed != 0.0F) {
            ptzPan = wrapDegrees(ptzPan + panSpeed * 90.0F * dt);
            got = true;
        }
        if (tiltSpeed != 0.0F) {
            ptzTilt = clamp(ptzTilt + tiltSpeed * 45.0F * dt, -90.0F, 90.0F);
            got = true;
        }
        if (zoomSpeed != 0.0F) {
            ptzFov = clamp(ptzFov - zoomSpeed * 60.0F * dt, 10.0F, 150.0F);
            got = true;
        }

        if (got) {
            ptzActive = true;
            pendingPtz.set(new float[]{ptzPan, ptzTilt, ptzFov});
        }
    }

    /** Parses one PTZ metadata message. Returns true if it was a recognized PTZ command. */
    private boolean parsePtz(String xml) {
        boolean handled = false;
        if (xml.contains("ntk_ptz_pan_tilt_speed")) {
            panSpeed = attr(xml, "pan_speed", 0.0F);
            tiltSpeed = attr(xml, "tilt_speed", 0.0F);
            handled = true;
        } else if (xml.contains("ntk_ptz_pan_tilt")) {
            ptzPan = attr(xml, "pan", 0.0F) * 180.0F;   // -1..1 -> -180..180
            ptzTilt = attr(xml, "tilt", 0.0F) * 90.0F;  // -1..1 -> -90..90
            panSpeed = 0.0F;
            tiltSpeed = 0.0F;
            handled = true;
        }
        if (xml.contains("ntk_ptz_zoom_speed")) {
            zoomSpeed = attr(xml, "zoom_speed", 0.0F);
            handled = true;
        } else if (xml.contains("ntk_ptz_zoom")) {
            ptzFov = 30.0F + attr(xml, "zoom", 0.5F) * 80.0F; // 0(in/tele) .. 1(out/wide)
            zoomSpeed = 0.0F;
            handled = true;
        }
        return handled;
    }

    private static float attr(String xml, String name, float fallback) {
        int i = xml.indexOf(name + "=\"");
        if (i < 0) {
            return fallback;
        }
        i += name.length() + 2;
        int j = xml.indexOf('"', i);
        if (j < 0) {
            return fallback;
        }
        try {
            return Float.parseFloat(xml.substring(i, j));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : Math.min(v, max);
    }

    private static float wrapDegrees(float deg) {
        float d = deg % 360.0F;
        if (d >= 180.0F) {
            d -= 360.0F;
        }
        if (d < -180.0F) {
            d += 360.0F;
        }
        return d;
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
            // BGRX (not BGRA): the feed is opaque; ignore the framebuffer alpha, which the sun
            // (additive) and rain (blended) leave non-opaque, causing artifacts / transparency.
            frame.setFourCCType(DevolayFrameFourCCType.BGRX);
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
        try {
            metadataIn.close();
        } catch (Throwable ignored) {
        }
    }
}
