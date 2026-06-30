package fr.lumavision.video;

import net.minecraft.core.BlockPos;

import java.util.Optional;

/**
 * Future abstraction for polling display frames for a wall.
 * <p>
 * Today: client-local capture via NDI. Tomorrow: server-relayed frames (Option B).
 */
public interface ScreenFrameProvider {

    Optional<VideoFrame> pollFrame(BlockPos groupOrigin);
}
