package fr.lumavision.camera;

import fr.lumavision.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/**
 * Computes where a crane camera actually shoots from: the end of an arm that swings (yaw) and booms
 * (elevation) out from the camera block, plus the direction it looks. The arm reach is how many boom
 * blocks are stacked under the camera (build taller = longer arm). With no boom column it's just the
 * block centre aimed by pan/tilt.
 */
public final class CameraRig {

    /** Visual (and reach) scale multiplier applied to the crane model by the renderer. */
    public static final float CRANE_SCALE = 1.75F;

    /** Arm-tip distance (blocks) of the crane model at scale 1 — the far end of the beam. */
    private static final float CRANE_BASE_REACH = 5.0F;

    /** Fixed arm reach (blocks) of the modelled {@code camera_crane} — matches its scaled 3D model. */
    public static final float CRANE_ARM_LENGTH = CRANE_BASE_REACH * CRANE_SCALE;

    /** Height (blocks) of the crane arm pivot above the block origin — matches the scaled model. */
    public static final double CRANE_PIVOT_Y = CRANE_SCALE * (9.0 / 16.0);

    /** How far the camera hangs below the arm tip (blocks), gimbal-style like a real crane. */
    public static final double CRANE_CAMERA_DROP = 0.5;

    /** Resolved shooting viewpoint: world position + look direction. */
    public record View(double x, double y, double z, float yaw, float pitch) {
    }

    private CameraRig() {
    }

    /** True if the camera at {@code pos} is the modelled 3D crane block. */
    public static boolean isCrane(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.CAMERA_CRANE.get());
    }

    /**
     * Arm-tip position relative to the crane block origin (in blocks) plus the arm yaw, from the
     * current swing/boom. Shared by the renderer (to hang the camera model) and {@link #craneView}
     * (the shot viewpoint) so the visible camera and the shot stay locked together.
     * Returns {@code [x, y, z, armYawDegrees]}.
     */
    public static double[] craneTipRelative(float baseYaw, CameraParameters p) {
        float armYaw = baseYaw + p.boomSwing();
        double yawRad = Math.toRadians(armYaw);
        double pitchRad = Math.toRadians(p.boomPitch());
        double horiz = Math.cos(pitchRad) * CRANE_ARM_LENGTH;
        double x = 0.5 - Math.sin(yawRad) * horiz;
        double z = 0.5 + Math.cos(yawRad) * horiz;
        double y = CRANE_PIVOT_Y + Math.sin(pitchRad) * CRANE_ARM_LENGTH;
        return new double[]{x, y, z, armYaw};
    }

    /** The shot for a crane: from the camera hanging off the arm tip, aimed by swing (+pan) / tilt. */
    public static View craneView(BlockPos pos, float baseYaw, CameraParameters p) {
        double[] tip = craneTipRelative(baseYaw, p);
        double camY = pos.getY() + tip[1] - CRANE_CAMERA_DROP;
        return new View(pos.getX() + tip[0], camY, pos.getZ() + tip[2], (float) tip[3] + p.pan(), p.tilt());
    }

    /** Number of camera_boom blocks stacked directly under the camera = the crane arm reach. */
    public static int boomReach(BlockGetter level, BlockPos cameraPos) {
        int count = 0;
        BlockPos.MutableBlockPos cursor = cameraPos.mutable();
        for (int i = 0; i < 64; i++) {
            cursor.move(Direction.DOWN);
            if (level.getBlockState(cursor).is(ModBlocks.CAMERA_BOOM.get())) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    public static View compute(BlockPos cameraPos, float baseYaw, CameraParameters p, float length) {
        double cx = cameraPos.getX() + 0.5D;
        double cy = cameraPos.getY() + 0.5D;
        double cz = cameraPos.getZ() + 0.5D;

        if (length <= 0.0F) {
            return new View(cx, cy, cz, baseYaw + p.pan(), p.tilt());
        }

        float armYaw = baseYaw + p.boomSwing();
        double yawRad = Math.toRadians(armYaw);
        double pitchRad = Math.toRadians(p.boomPitch());
        double horiz = Math.cos(pitchRad) * length;

        double x = cx - Math.sin(yawRad) * horiz;
        double z = cz + Math.cos(yawRad) * horiz;
        double y = cy + Math.sin(pitchRad) * length;

        // The arm booms up/down to *move* the camera; its aim is independent — it keeps looking
        // outward along the swing (level by default), tilted only by the configured/PTZ pan+tilt.
        return new View(x, y, z, armYaw + p.pan(), p.tilt());
    }
}
