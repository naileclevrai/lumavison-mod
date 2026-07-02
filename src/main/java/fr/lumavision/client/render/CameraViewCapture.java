package fr.lumavision.client.render;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import fr.lumavision.LumaVisionMod;
import fr.lumavision.client.ndi.CameraNdiManager;
import fr.lumavision.client.ndi.CameraSnapshot;
import fr.lumavision.config.ModConfig;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders each active camera's view of the world into an offscreen framebuffer and reads the pixels
 * back (BGRA, top-down) for NDI. Runs on the render thread from {@code RenderTickEvent.END} (outside
 * the main {@code renderLevel}, so it isn't re-entrant).
 *
 * <p>Each camera gets its own {@link ViewArea} (chunk-render grid) and visible-chunk list; around the
 * capture we swap the {@link LevelRenderer}'s chunk-render state to the camera's and restore the
 * player's afterward, so cameras render independently and never corrupt the player's view.
 *
 * <p>The camera renders at the configured output resolution (see {@code cameraUseConfiguredResolution})
 * rather than the game window when possible, to keep game FPS acceptable. Post-processing mods that
 * reset the viewport to the window size (Shimmer) can misalign blocks at reduced resolution — restore
 * the viewport after {@code renderLevel} and set {@code cameraUseConfiguredResolution} to false if needed.
 *
 * <p>Pixel readback is asynchronous via double-buffered Pixel Buffer Objects (PBO): the copy is
 * issued into a PBO (non-blocking) and mapped on the next capture, so the render thread never
 * stalls waiting for the GPU. This costs one capture frame of latency on the NDI feed.
 *
 * <p>To spread load, at most one camera is captured per rendered frame (round-robin over the active
 * cameras), and captures are skipped on frames where the game is already lagging.
 */
@OnlyIn(Dist.CLIENT)
public final class CameraViewCapture {

    private static final CameraViewCapture INSTANCE = new CameraViewCapture();

    public static CameraViewCapture getInstance() {
        return INSTANCE;
    }

    private static final class Target {
        RenderTarget rt;
        byte[] frameBytes;
        int w;
        int h;
        long lastCaptureNs;

        // Async PBO readback (double-buffered).
        int[] pbos;
        int pboSize;
        int pboWrite;      // index issued the async read this frame
        boolean pboPending; // a read has been issued and is awaiting map
        int pendingW;
        int pendingH;
        BlockPos pendingPos;

        // Per-camera chunk-render state.
        ViewArea viewArea;
        ObjectArrayList<?> renderChunks;
        int viewDistance = -1;
        double lastX = Double.NaN;
        double lastY = Double.NaN;
        double lastZ = Double.NaN;
        int lastChunkX = Integer.MIN_VALUE;
        int lastChunkY = Integer.MIN_VALUE;
        int lastChunkZ = Integer.MIN_VALUE;
        boolean needsFullUpdate = true;

        double lastSnapX = Double.NaN;
        double lastSnapY = Double.NaN;
        double lastSnapZ = Double.NaN;
        float lastSnapYaw = Float.NaN;
        float lastSnapPitch = Float.NaN;
        float lastSnapFov = Float.NaN;
    }

    private final Map<BlockPos, Target> targets = new HashMap<>();
    private final Camera captureCamera = new Camera();
    private boolean fabulousWarned;
    private int roundRobinCursor;
    private long lastRenderTickNs;

    private CameraViewCapture() {
    }

    public void renderTick(float partialTick) {
        if (!ModConfig.NDI_ENABLE_OUTPUT.get() || !ModConfig.CAMERA_RENDER_WORLD.get()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (mc.options.graphicsMode().get() == GraphicsStatus.FABULOUS) {
            if (!fabulousWarned) {
                fabulousWarned = true;
                LumaVisionMod.LOGGER.warn("Camera world capture is skipped in Fabulous graphics mode; "
                        + "switch to Fancy/Fast for live camera video. Falling back to test pattern.");
                if (mc.player != null) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "[LumaVision] Camera video needs Fancy or Fast graphics (Fabulous isn't supported yet)."), false);
                }
            }
            return;
        }

        // Skip captures on frames where the game is already lagging (avoids a lag spiral).
        long now = System.nanoTime();
        if (ModConfig.CAMERA_SKIP_WHEN_LAGGING.get() && lastRenderTickNs != 0L) {
            long frameMs = (now - lastRenderTickNs) / 1_000_000L;
            if (frameMs > ModConfig.CAMERA_LAG_FRAME_MS.get()) {
                lastRenderTickNs = now;
                return;
            }
        }
        lastRenderTickNs = now;

        // Collect active cameras, then capture at most one per frame (round-robin).
        List<CameraEntry> active = new ArrayList<>();
        CameraNdiManager.getInstance().forEachActiveCamera((pos, snapshot) -> active.add(new CameraEntry(pos, snapshot)));
        if (active.isEmpty()) {
            return;
        }

        int count = active.size();
        // Try starting from the round-robin cursor; capture the first camera due for a frame.
        for (int i = 0; i < count; i++) {
            int idx = (roundRobinCursor + i) % count;
            CameraEntry entry = active.get(idx);
            if (captureOne(mc, entry.pos(), entry.snapshot(), partialTick)) {
                roundRobinCursor = (idx + 1) % count;
                return;
            }
        }
    }

    private record CameraEntry(BlockPos pos, CameraSnapshot snapshot) {
    }

    /** Returns true if a fresh world render was issued this frame (so the round-robin advances). */
    private boolean captureOne(Minecraft mc, BlockPos pos, CameraSnapshot snapshot, float partialTick) {
        int[] dims = computeRenderSize(mc, snapshot);
        int w = dims[0];
        int h = dims[1];

        Target t = targets.get(pos);
        long now = System.nanoTime();
        int effectiveFps = effectiveCaptureFps(snapshot);
        boolean due = (t == null) || (now - t.lastCaptureNs) >= (1_000_000_000L / effectiveFps);

        // Always drain a previously issued async read so the feed keeps flowing (one frame latency),
        // even on frames where this camera is not due for a fresh render.
        if (t != null && t.pboPending) {
            drainPending(t, pos);
        }
        if (!due) {
            return false;
        }
        if (t == null) {
            t = new Target();
            targets.put(pos, t);
        }

        if (t.rt == null || t.w != w || t.h != h) {
            if (t.rt != null) {
                t.rt.destroyBuffers();
            }
            t.rt = new MainTarget(w, h);
            t.frameBytes = new byte[w * h * 4];
            t.w = w;
            t.h = h;
        }
        int viewDistance = Math.min(
                mc.options.getEffectiveRenderDistance(),
                ModConfig.CAMERA_CAPTURE_VIEW_DISTANCE.get()
        );
        if (t.viewArea == null || t.viewDistance != viewDistance) {
            if (t.viewArea != null) {
                t.viewArea.releaseAllBuffers();
            }
            t.viewArea = new ViewArea(mc.levelRenderer.getChunkRenderDispatcher(), mc.level, viewDistance, mc.levelRenderer);
            t.renderChunks = new ObjectArrayList<>();
            t.viewDistance = viewDistance;
            t.needsFullUpdate = true;
            t.lastChunkX = Integer.MIN_VALUE;
            t.lastChunkY = Integer.MIN_VALUE;
            t.lastChunkZ = Integer.MIN_VALUE;
        }

        // Optional: skip the expensive world render entirely when the view has not moved (fixed shot).
        // Off by default because it also freezes moving entities in the feed. Chunk-update state is left
        // to renderLevel (as in the known-good path) so geometry is never rendered at stale offsets.
        if (ModConfig.CAMERA_SKIP_STATIC_FRAMES.get()
                && !viewChanged(t, snapshot) && !t.needsFullUpdate && !Double.isNaN(t.lastSnapX)) {
            t.lastCaptureNs = now;
            return false;
        }

        t.lastCaptureNs = now;

        try {
            renderCameraView(mc, t, snapshot, partialTick);
            issueAsyncReadback(t, pos, w, h);
            rememberView(t, snapshot);
        } catch (Throwable ex) {
            CameraNdiManager.getInstance().onCaptureFailed(pos, ex);
        }
        return true;
    }

    private void drainPending(Target t, BlockPos pos) {
        try {
            byte[] frame = mapPendingReadback(t);
            if (frame != null) {
                CameraNdiManager.getInstance().submitCapturedFrame(t.pendingPos, frame, t.pendingW, t.pendingH);
            }
        } catch (Throwable ex) {
            CameraNdiManager.getInstance().onCaptureFailed(pos, ex);
        }
    }

    private static int effectiveCaptureFps(CameraSnapshot snapshot) {
        int maxFps = ModConfig.CAMERA_MAX_CAPTURE_FPS.get();
        return Math.min(Math.max(1, snapshot.fps()), Math.max(1, maxFps));
    }

    private static int[] computeRenderSize(Minecraft mc, CameraSnapshot snapshot) {
        int maxRes = ModConfig.CAMERA_MAX_RESOLUTION.get();
        int scale = ModConfig.CAMERA_RENDER_SCALE.get();
        int w;
        int h;
        if (ModConfig.CAMERA_USE_CONFIGURED_RESOLUTION.get()) {
            w = Math.max(2, snapshot.width());
            h = Math.max(2, snapshot.height());
        } else {
            w = Math.max(2, mc.getMainRenderTarget().width);
            h = Math.max(2, mc.getMainRenderTarget().height);
        }
        w = Math.max(2, w * scale / 100);
        h = Math.max(2, h * scale / 100);
        int longest = Math.max(w, h);
        if (longest > maxRes) {
            float factor = maxRes / (float) longest;
            w = Math.max(64, Math.round(w * factor));
            h = Math.max(64, Math.round(h * factor));
        }
        w = w & ~1;
        h = h & ~1;
        return new int[]{Math.max(2, w), Math.max(2, h)};
    }

    private static boolean viewChanged(Target t, CameraSnapshot snapshot) {
        if (Double.isNaN(t.lastSnapX)) {
            return true;
        }
        return Math.abs(t.lastSnapX - snapshot.renderX()) > 0.05D
                || Math.abs(t.lastSnapY - snapshot.renderY()) > 0.05D
                || Math.abs(t.lastSnapZ - snapshot.renderZ()) > 0.05D
                || Math.abs(t.lastSnapYaw - snapshot.yaw()) > 0.25F
                || Math.abs(t.lastSnapPitch - snapshot.pitch()) > 0.25F
                || Math.abs(t.lastSnapFov - snapshot.fov()) > 0.25F;
    }

    private static void rememberView(Target t, CameraSnapshot snapshot) {
        t.lastSnapX = snapshot.renderX();
        t.lastSnapY = snapshot.renderY();
        t.lastSnapZ = snapshot.renderZ();
        t.lastSnapYaw = snapshot.yaw();
        t.lastSnapPitch = snapshot.pitch();
        t.lastSnapFov = snapshot.fov();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void renderCameraView(Minecraft mc, Target t, CameraSnapshot snapshot, float partialTick) {
        LevelRenderer lr = mc.levelRenderer;
        RenderTarget previousTarget = mc.mainRenderTarget; // access-transformed to public

        // --- save the player's chunk-render state ---
        ViewArea pViewArea = lr.viewArea;
        ObjectArrayList pRenderChunks = lr.renderChunksInFrustum;
        Frustum pCulling = lr.cullingFrustum;
        boolean pNeedsFull = lr.needsFullRenderChunkUpdate;
        double pLastX = lr.lastCameraX, pLastY = lr.lastCameraY, pLastZ = lr.lastCameraZ;
        int pLastCX = lr.lastCameraChunkX, pLastCY = lr.lastCameraChunkY, pLastCZ = lr.lastCameraChunkZ;

        Marker marker = new Marker(EntityType.MARKER, mc.level);
        double ex = snapshot.renderX();
        double ey = snapshot.renderY();
        double ez = snapshot.renderZ();
        marker.setPos(ex, ey, ez);
        marker.xo = ex;
        marker.yo = ey;
        marker.zo = ez;
        marker.setYRot(snapshot.yaw());
        marker.setXRot(snapshot.pitch());
        marker.yRotO = snapshot.yaw();
        marker.xRotO = snapshot.pitch();

        mc.renderBuffers().bufferSource().endBatch();
        mc.mainRenderTarget = t.rt;

        // --- swap in the camera's chunk-render state ---
        lr.viewArea = t.viewArea;
        lr.renderChunksInFrustum = (ObjectArrayList) t.renderChunks;
        lr.needsFullRenderChunkUpdate = t.needsFullUpdate;
        lr.lastCameraX = t.lastX;
        lr.lastCameraY = t.lastY;
        lr.lastCameraZ = t.lastZ;
        lr.lastCameraChunkX = t.lastChunkX;
        lr.lastCameraChunkY = t.lastChunkY;
        lr.lastCameraChunkZ = t.lastChunkZ;

        try {
            t.rt.viewWidth = t.w;
            t.rt.viewHeight = t.h;
            t.rt.clear(Minecraft.ON_OSX);
            t.rt.bindWrite(true);
            RenderSystem.viewport(0, 0, t.w, t.h);

            captureCamera.setup(mc.level, marker, false, false, partialTick);

            float far = Math.max(mc.gameRenderer.getRenderDistance(), 64.0F) * 4.0F;
            Matrix4f projection = new Matrix4f().perspective(
                    (float) Math.toRadians(snapshot.fov()), (float) t.w / t.h, 0.05F, far);
            RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN);

            PoseStack poseStack = new PoseStack();
            poseStack.mulPose(Axis.XP.rotationDegrees(captureCamera.getXRot()));
            poseStack.mulPose(Axis.YP.rotationDegrees(captureCamera.getYRot() + 180.0F));
            Matrix3f inverseView = new Matrix3f(poseStack.last().normal()).invert();
            RenderSystem.setInverseViewRotationMatrix(inverseView);

            lr.prepareCullFrustum(poseStack, captureCamera.getPosition(), projection);
            lr.renderLevel(poseStack, partialTick, 0L, false, captureCamera, mc.gameRenderer,
                    mc.gameRenderer.lightTexture(), projection);

            // Post-processing mods may reset the viewport to the game window; clamp back before readback.
            RenderSystem.viewport(0, 0, t.w, t.h);
            mc.renderBuffers().bufferSource().endBatch();
            t.rt.unbindWrite();
        } finally {
            // --- save the camera's updated state back, then restore the player's ---
            t.needsFullUpdate = lr.needsFullRenderChunkUpdate;
            t.lastX = lr.lastCameraX;
            t.lastY = lr.lastCameraY;
            t.lastZ = lr.lastCameraZ;
            t.lastChunkX = lr.lastCameraChunkX;
            t.lastChunkY = lr.lastCameraChunkY;
            t.lastChunkZ = lr.lastCameraChunkZ;

            lr.viewArea = pViewArea;
            lr.renderChunksInFrustum = pRenderChunks;
            lr.cullingFrustum = pCulling;
            lr.needsFullRenderChunkUpdate = pNeedsFull;
            lr.lastCameraX = pLastX;
            lr.lastCameraY = pLastY;
            lr.lastCameraZ = pLastZ;
            lr.lastCameraChunkX = pLastCX;
            lr.lastCameraChunkY = pLastCY;
            lr.lastCameraChunkZ = pLastCZ;

            mc.mainRenderTarget = previousTarget;
            marker.discard();
            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    /**
     * Issues a non-blocking pixel read of the camera target into a PBO. Returns immediately; the
     * data is fetched on the next capture via {@link #mapPendingReadback(Target)}.
     */
    private void issueAsyncReadback(Target t, BlockPos pos, int w, int h) {
        int size = w * h * 4;
        if (t.pbos == null) {
            t.pbos = new int[]{GL15.glGenBuffers(), GL15.glGenBuffers()};
            t.pboSize = 0;
        }
        if (t.pboSize != size) {
            for (int id : t.pbos) {
                GL30.glBindBuffer(GL30.GL_PIXEL_PACK_BUFFER, id);
                GL30.glBufferData(GL30.GL_PIXEL_PACK_BUFFER, size, GL15.GL_STREAM_READ);
            }
            GL30.glBindBuffer(GL30.GL_PIXEL_PACK_BUFFER, 0);
            t.pboSize = size;
        }

        int writeIndex = t.pboWrite;
        GL30.glBindBuffer(GL30.GL_PIXEL_PACK_BUFFER, t.pbos[writeIndex]);
        GlStateManager._bindTexture(t.rt.getColorTextureId());
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        // Read into the bound PACK buffer (offset 0) — asynchronous, does not block the render thread.
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, 0L);
        GlStateManager._bindTexture(0);
        GL30.glBindBuffer(GL30.GL_PIXEL_PACK_BUFFER, 0);

        t.pboPending = true;
        t.pendingW = w;
        t.pendingH = h;
        t.pendingPos = pos;
    }

    /**
     * Maps the PBO from the previously issued async read and copies it (vertically flipped) into
     * {@link Target#frameBytes}. Data is already resident, so this does not stall.
     */
    private byte[] mapPendingReadback(Target t) {
        if (!t.pboPending || t.pbos == null) {
            return null;
        }
        int w = t.pendingW;
        int h = t.pendingH;
        int rowBytes = w * 4;
        int size = rowBytes * h;

        GL30.glBindBuffer(GL30.GL_PIXEL_PACK_BUFFER, t.pbos[t.pboWrite]);
        ByteBuffer mapped = GL30.glMapBufferRange(GL30.GL_PIXEL_PACK_BUFFER, 0L, size, GL30.GL_MAP_READ_BIT);
        byte[] out = null;
        if (mapped != null) {
            if (t.frameBytes == null || t.frameBytes.length != size) {
                t.frameBytes = new byte[size];
            }
            mapped.order(ByteOrder.nativeOrder());
            for (int y = 0; y < h; y++) {
                mapped.position((h - 1 - y) * rowBytes);
                mapped.get(t.frameBytes, y * rowBytes, rowBytes);
            }
            out = t.frameBytes;
        }
        GL30.glUnmapBuffer(GL30.GL_PIXEL_PACK_BUFFER);
        GL30.glBindBuffer(GL30.GL_PIXEL_PACK_BUFFER, 0);

        // Alternate buffers so the next issue writes to the other PBO.
        t.pboWrite ^= 1;
        t.pboPending = false;
        return out;
    }

    /** Must run on the render thread (destroys GL buffers). */
    public void clear() {
        for (Target t : targets.values()) {
            releaseTarget(t);
        }
        targets.clear();
    }

    public void remove(BlockPos pos) {
        Target t = targets.remove(pos);
        if (t != null) {
            releaseTarget(t);
        }
    }

    private void releaseTarget(Target t) {
        if (t.rt != null) {
            t.rt.destroyBuffers();
        }
        if (t.viewArea != null) {
            t.viewArea.releaseAllBuffers();
        }
        if (t.pbos != null) {
            for (int id : t.pbos) {
                GL15.glDeleteBuffers(id);
            }
            t.pbos = null;
        }
    }
}
