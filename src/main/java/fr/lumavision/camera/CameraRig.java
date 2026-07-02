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

    /** Resolved shooting viewpoint: world position + look direction. */
    public record View(double x, double y, double z, float yaw, float pitch) {
    }

    private CameraRig() {
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

        // Camera on the arm end looks outward along the swing, tilting down as the arm rises.
        return new View(x, y, z, armYaw, p.boomPitch());
    }
}
