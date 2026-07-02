package fr.lumavision.client;

import fr.lumavision.blockentity.CameraBlockEntity;
import fr.lumavision.network.CameraLiveStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client-only application of server-authored live camera state. Kept in its own class so the
 * client-only {@link Minecraft} reference is never loaded on a dedicated server.
 */
@OnlyIn(Dist.CLIENT)
public final class CameraClientPacketHandler {

    private CameraClientPacketHandler() {
    }

    public static void applyLiveState(CameraLiveStatePacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        BlockEntity be = mc.level.getBlockEntity(packet.pos());
        if (be instanceof CameraBlockEntity camera) {
            camera.parameters().setPan(packet.pan());
            camera.parameters().setTilt(packet.tilt());
            camera.parameters().setFov(packet.fov());
            camera.parameters().setTrackPosition(packet.trackPos());
        }
    }
}
