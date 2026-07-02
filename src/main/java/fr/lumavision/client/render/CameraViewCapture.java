package fr.lumavision.client.render;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders each active camera's view of the world into an offscreen framebuffer and reads the pixels
 * back (BGRA, top-down) for NDI. Runs on the render thread from {@code RenderTickEvent.END} (outside
 * the main {@code renderLevel}, so it isn't re-entrant).
 *
 * <p>Each camera gets its <em>own</em> {@link ViewArea} (chunk-render grid) and visible-chunk list.
 * Around the capture we swap the {@link LevelRenderer}'s chunk-render state to the camera's and
 * restore the player's afterward, so cameras render independently and never corrupt the player's
 * view. Chunks around the camera are compiled by the shared dispatcher over a few frames, so a
 * newly-placed / newly-loaded camera fills in progressively.
 */
@OnlyIn(Dist.CLIENT)
public final class CameraViewCapture {

    private static final CameraViewCapture INSTANCE = new CameraViewCapture();

    public static CameraViewCapture getInstance() {
        return INSTANCE;
    }

    private static final class Target {
        RenderTarget rt;
        ByteBuffer readBuffer;
        int w;
        int h;
        long lastCaptureNs;

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
    }

    private final Map<BlockPos, Target> targets = new HashMap<>();
    private final Camera captureCamera = new Camera();
    private boolean fabulousWarned;

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
        CameraNdiManager.getInstance().forEachActiveCamera((pos, snapshot) -> captureOne(mc, pos, snapshot, partialTick));
    }

    private void captureOne(Minecraft mc, BlockPos pos, CameraSnapshot snapshot, float partialTick) {
        int w = Math.max(2, snapshot.width());
        int h = Math.max(2, snapshot.height());

        Target t = targets.get(pos);
        long now = System.nanoTime();
        if (t != null && (now - t.lastCaptureNs) < (1_000_000_000L / Math.max(1, snapshot.fps()))) {
            return; // pace capture to the camera's configured FPS
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
            t.readBuffer = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
            t.w = w;
            t.h = h;
        }
        int viewDistance = mc.options.getEffectiveRenderDistance();
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
        t.lastCaptureNs = now;

        try {
            renderCameraView(mc, t, snapshot, partialTick);
            byte[] frame = readback(t);
            CameraNdiManager.getInstance().submitCapturedFrame(pos, frame, w, h);
        } catch (Throwable ex) {
            CameraNdiManager.getInstance().onCaptureFailed(pos, ex);
        }
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
        double ex = snapshot.x() + 0.5D;
        double ey = snapshot.y() + 0.5D;
        double ez = snapshot.z() + 0.5D;
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

        // The world render's viewport follows the window size; point it at the camera resolution so
        // the whole target is rendered (not just a window-sized corner). Restored in finally.
        Window window = mc.getWindow();
        int prevWinW = window.getWidth();
        int prevWinH = window.getHeight();
        window.setWidth(t.w);
        window.setHeight(t.h);

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
            t.rt.clear(Minecraft.ON_OSX);
            t.rt.bindWrite(true);

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

            window.setWidth(prevWinW);
            window.setHeight(prevWinH);
            mc.mainRenderTarget = previousTarget;
            marker.discard();
            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    private byte[] readback(Target t) {
        ByteBuffer buf = t.readBuffer;
        buf.clear();
        GlStateManager._bindTexture(t.rt.getColorTextureId());
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buf);
        GlStateManager._bindTexture(0);

        int rowBytes = t.w * 4;
        byte[] out = new byte[rowBytes * t.h];
        for (int y = 0; y < t.h; y++) {
            buf.position((t.h - 1 - y) * rowBytes);
            buf.get(out, y * rowBytes, rowBytes);
        }
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
    }
}
