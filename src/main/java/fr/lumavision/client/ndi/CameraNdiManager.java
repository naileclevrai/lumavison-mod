package fr.lumavision.client.ndi;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.block.CameraBlock;
import fr.lumavision.blockentity.CameraBlockEntity;
import fr.lumavision.camera.CameraParameters;
import fr.lumavision.config.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import fr.lumavision.client.render.CameraViewCapture;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Client-side registry of live NDI camera senders, one per camera block currently ticking on the
 * client. Cameras self-register each client tick via {@link CameraBlockEntity#clientTick} (so no
 * chunk scanning is needed); senders whose camera has stopped ticking (unloaded/removed/disabled)
 * are reaped. Enforces the {@code cameraMaxActive} performance cap and the {@code ndiEnableOutput}
 * toggle. All GPU/NDI work is client-only.
 */
@OnlyIn(Dist.CLIENT)
public final class CameraNdiManager {

    private static final int REAP_AFTER_TICKS = 3;

    private static final CameraNdiManager INSTANCE = new CameraNdiManager();

    private final Map<BlockPos, CameraNdiSender> senders = new HashMap<>();
    private final Map<BlockPos, Long> lastSeen = new HashMap<>();
    private final Set<BlockPos> captureFailureLogged = new HashSet<>();
    private long clientTick;
    private boolean runtimeChecked;
    private boolean runtimeAvailable;
    private boolean unavailableAnnounced;

    private CameraNdiManager() {
    }

    public static CameraNdiManager getInstance() {
        return INSTANCE;
    }

    /** Called from {@link CameraBlockEntity#clientTick} for each ticking camera on the client. */
    public void onCameraClientTick(CameraBlockEntity be) {
        BlockPos pos = be.getBlockPos().immutable();
        if (!ModConfig.NDI_ENABLE_OUTPUT.get() || !be.parameters().enabled()) {
            removeIfPresent(pos);
            return;
        }
        // A crane only produces a feed once a camera is mounted on the arm end.
        BlockState craneState = be.getBlockState();
        if (craneState.is(fr.lumavision.registry.ModBlocks.CAMERA_CRANE.get())
                && !craneState.getValue(fr.lumavision.block.CameraCraneBlock.MOUNTED)) {
            removeIfPresent(pos);
            return;
        }
        if (!ensureRuntime()) {
            return;
        }

        CameraSnapshot snapshot = snapshotOf(be);
        CameraNdiSender existing = senders.get(pos);
        if (existing != null && !existing.name().equals(snapshot.name())) {
            existing.close();
            senders.remove(pos);
            existing = null;
        }

        if (existing == null) {
            if (senders.size() >= ModConfig.CAMERA_MAX_ACTIVE.get()) {
                return; // performance cap reached; skip this camera
            }
            try {
                senders.put(pos, new CameraNdiSender(snapshot));
            } catch (Throwable t) {
                LumaVisionMod.LOGGER.error("Failed to start NDI sender for '{}': {}", snapshot.name(), t.toString());
                return;
            }
        } else {
            existing.update(snapshot);
        }
        lastSeen.put(pos, clientTick);

        // Forward any NDI PTZ command received on this client to the server (authoritative).
        CameraNdiSender sender = senders.get(pos);
        if (sender != null) {
            float[] ptz = sender.pollPtz();
            if (ptz != null) {
                fr.lumavision.network.ModNetworking.CHANNEL.sendToServer(
                        new fr.lumavision.network.CameraPtzInputPacket(pos, ptz[0], ptz[1], ptz[2]));
            }
        }
    }

    /** Called once per client tick to reap senders whose camera stopped ticking. */
    public void tick() {
        clientTick++;
        Iterator<Map.Entry<BlockPos, CameraNdiSender>> it = senders.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, CameraNdiSender> entry = it.next();
            Long seen = lastSeen.get(entry.getKey());
            if (seen == null || clientTick - seen > REAP_AFTER_TICKS) {
                entry.getValue().close();
                CameraViewCapture.getInstance().remove(entry.getKey());
                lastSeen.remove(entry.getKey());
                captureFailureLogged.remove(entry.getKey());
                it.remove();
            }
        }
    }

    /** Iterates the currently live cameras (render thread), for offscreen capture. */
    public void forEachActiveCamera(BiConsumer<BlockPos, CameraSnapshot> action) {
        for (Map.Entry<BlockPos, CameraNdiSender> entry : senders.entrySet()) {
            action.accept(entry.getKey(), entry.getValue().snapshot());
        }
    }

    /** Hands a captured world-view frame (BGRA, top-down) to the camera's sender. */
    public void submitCapturedFrame(BlockPos pos, byte[] bgra, int w, int h) {
        CameraNdiSender sender = senders.get(pos);
        if (sender != null) {
            sender.submitFrame(bgra, w, h);
        }
    }

    /** Called when offscreen capture throws; logs once per camera and lets it fall back to the test pattern. */
    public void onCaptureFailed(BlockPos pos, Throwable error) {
        if (captureFailureLogged.add(pos)) {
            LumaVisionMod.LOGGER.error("Camera world capture failed at {} (falling back to test pattern): {}",
                    pos, error.toString(), error);
            StackTraceElement[] trace = error.getStackTrace();
            String where = trace.length > 0 ? " @ " + trace[0] : "";
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(
                        "[LumaVision] Camera render failed: " + error.getClass().getSimpleName()
                                + ": " + error.getMessage() + where), false);
            }
        }
    }

    public void shutdown() {
        for (CameraNdiSender sender : senders.values()) {
            sender.close();
        }
        senders.clear();
        lastSeen.clear();
        captureFailureLogged.clear();
    }

    private void removeIfPresent(BlockPos pos) {
        CameraNdiSender sender = senders.remove(pos);
        if (sender != null) {
            sender.close();
            lastSeen.remove(pos);
        }
    }

    private boolean ensureRuntime() {
        if (!runtimeChecked) {
            runtimeChecked = true;
            runtimeAvailable = NdiRuntime.init();
            if (!runtimeAvailable) {
                LumaVisionMod.LOGGER.warn("NDI camera output unavailable: {}", NdiRuntime.getFailureReason());
            } else {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal(
                            "[LumaVision] NDI runtime loaded: " + NdiRuntime.getVersion()), false);
                }
            }
        }
        if (!runtimeAvailable && !unavailableAnnounced) {
            unavailableAnnounced = true;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal(
                        "[LumaVision] NDI output unavailable — install the NDI Runtime (ndi.video/tools) and restart. "
                                + "See logs for details."), false);
            }
        }
        return runtimeAvailable;
    }

    private static CameraSnapshot snapshotOf(CameraBlockEntity be) {
        CameraParameters p = be.parameters();
        BlockPos pos = be.getBlockPos();
        BlockState state = be.getBlockState();
        float baseYaw = state.hasProperty(CameraBlock.FACING) ? state.getValue(CameraBlock.FACING).toYRot() : 0.0F;
        String name = p.ndiSourceName().isEmpty() ? CameraBlockEntity.defaultSourceName(pos) : p.ndiSourceName();

        // Crane arm: reach = the modelled crane's fixed length, or boom blocks stacked under the camera;
        // shoot from the arm end. Otherwise: block centre or rail dolly, aimed by pan/tilt.
        float reach = be.getLevel() != null ? fr.lumavision.camera.CameraRig.reachFor(be.getLevel(), pos) : 0.0F;
        fr.lumavision.camera.CameraRig.View view;
        if (reach > 0.0F) {
            view = fr.lumavision.camera.CameraRig.compute(pos, baseYaw, p, reach);
        } else {
            double rx = pos.getX() + 0.5D;
            double ry = pos.getY() + 0.5D;
            double rz = pos.getZ() + 0.5D;
            if (be.getLevel() != null) {
                net.minecraft.world.phys.Vec3 dolly =
                        fr.lumavision.block.RailTrack.resolve(be.getLevel(), pos, p.trackPosition());
                if (dolly != null) {
                    rx = dolly.x;
                    ry = dolly.y;
                    rz = dolly.z;
                }
            }
            view = new fr.lumavision.camera.CameraRig.View(rx, ry, rz, baseYaw + p.pan(), p.tilt());
        }

        return new CameraSnapshot(name, p.resolutionWidth(), p.resolutionHeight(), p.fps(),
                view.x(), view.y(), view.z(), view.yaw(), view.pitch(), p.effectiveFov(), p.pan());
    }
}
