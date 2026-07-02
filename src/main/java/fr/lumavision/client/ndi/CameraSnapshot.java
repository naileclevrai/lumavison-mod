package fr.lumavision.client.ndi;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Immutable, thread-safe snapshot of the state an NDI camera sender needs, handed from the client
 * tick to the sender thread. Decouples the (synced, mutable) block-entity parameters from the
 * background send loop.
 */
@OnlyIn(Dist.CLIENT)
public record CameraSnapshot(String name, int width, int height, int fps,
                             double renderX, double renderY, double renderZ,
                             float yaw, float pitch, float fov, float pan) {
}
