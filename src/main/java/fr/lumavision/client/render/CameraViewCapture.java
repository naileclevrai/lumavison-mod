package fr.lumavision.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import fr.lumavision.LumaVisionMod;
import fr.lumavision.client.ndi.CameraNdiManager;
import fr.lumavision.client.ndi.CameraSnapshot;
import fr.lumavision.config.ModConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders each active camera's view of the world into an offscreen framebuffer and reads the pixels
 * back (BGRA, top-down) so {@link CameraNdiManager} can stream them over NDI. Runs on the render
 * thread from {@code RenderTickEvent.END} (outside the main {@code renderLevel}, so it isn't
 * re-entrant), following the approach proven by SecurityCraft: swap the camera entity to a throwaway
 * {@link Marker}, redirect {@code Minecraft.mainRenderTarget} to our target, let vanilla render, then
 * restore everything. If capture fails or the graphics mode is Fabulous (extra render targets we
 * don't manage), the camera falls back to the test pattern so the game stays stable.
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
    }

    private final Map<BlockPos, Target> targets = new HashMap<>();
    /** A dedicated camera so we never mutate the game's main camera (which caused view-bob). */
    private final Camera captureCamera = new Camera();
    private boolean fabulousWarned;

    private CameraViewCapture() {
    }

    /** Called on the render thread at {@code RenderTickEvent.END}. */
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
                LumaVisionMod.LOGGER.warn("Camera world capture is skipped in Fabulous graphics mode "
                        + "(uses extra render targets); switch to Fancy/Fast for live camera video. Falling back to test pattern.");
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
            t.rt = new TextureTarget(w, h, true, Minecraft.ON_OSX);
            t.readBuffer = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder());
            t.w = w;
            t.h = h;
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

    private void renderCameraView(Minecraft mc, Target t, CameraSnapshot snapshot, float partialTick) {
        // Only the main render target is temporarily redirected; the camera used is our own dedicated
        // instance, so the game's main camera state is never touched (that previously bobbed the view).
        RenderTarget previousTarget = mc.mainRenderTarget; // access-transformed to public

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

        mc.renderBuffers().bufferSource().endBatch(); // flush any batched geometry from the main frame
        mc.mainRenderTarget = t.rt;

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

            mc.levelRenderer.prepareCullFrustum(poseStack, captureCamera.getPosition(), projection);
            mc.levelRenderer.renderLevel(poseStack, partialTick, 0L, false, captureCamera, mc.gameRenderer,
                    mc.gameRenderer.lightTexture(), projection);

            mc.renderBuffers().bufferSource().endBatch(); // flush our camera render
            t.rt.unbindWrite();
        } finally {
            mc.mainRenderTarget = previousTarget;
            marker.discard();
            mc.getMainRenderTarget().bindWrite(true);
        }
    }

    private byte[] readback(Target t) {
        ByteBuffer buf = t.readBuffer;
        buf.clear();
        t.rt.bindRead();
        GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, t.w, t.h, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buf);
        t.rt.unbindRead();

        // OpenGL returns rows bottom-to-top; NDI wants top-to-bottom, so flip while copying out.
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
            if (t.rt != null) {
                t.rt.destroyBuffers();
            }
        }
        targets.clear();
    }

    /** Drops a single camera's render target (e.g. when it unloads). */
    public void remove(BlockPos pos) {
        Target t = targets.remove(pos);
        if (t != null && t.rt != null) {
            t.rt.destroyBuffers();
        }
    }
}
