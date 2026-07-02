package fr.lumavision.server;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.blockentity.CameraBlockEntity;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import fr.lumavision.config.ModConfig;
import fr.lumavision.network.MediaRelaySyncPacket;
import fr.lumavision.network.ModNetworking;
import fr.lumavision.network.ScreenFrameChunkHandler;
import fr.lumavision.relay.RelaySources;
import fr.lumavision.relay.WallRelayRole;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side NDI bridge: elects one capture client per wall/camera and relays frames to viewers.
 */
public final class MediaRelayManager {

    private static final MediaRelayManager INSTANCE = new MediaRelayManager();

    private static final int REFRESH_INTERVAL_TICKS = 40;

    private int tickCounter;
    private final Map<WallKey, UUID> wallSenders = new HashMap<>();
    private final Map<CameraKey, UUID> cameraCapturers = new HashMap<>();
    private final Map<WallKey, CachedFrame> latestFrames = new HashMap<>();

    private MediaRelayManager() {
    }

    public static MediaRelayManager getInstance() {
        return INSTANCE;
    }

    public void onServerTick(MinecraftServer server) {
        if (!ModConfig.ENABLE_MULTIPLAYER_RELAY.get()) {
            return;
        }
        if (++tickCounter < REFRESH_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;
        refreshAll(server);
    }

    public void onPlayerJoin(ServerPlayer player) {
        if (!ModConfig.ENABLE_MULTIPLAYER_RELAY.get()) {
            return;
        }
        sendSyncToPlayer(player);
    }

    public void onWallSourceChanged(ServerLevel level, BlockPos origin) {
        if (!ModConfig.ENABLE_MULTIPLAYER_RELAY.get()) {
            return;
        }
        refreshLevel(level);
        for (ServerPlayer player : level.players()) {
            sendSyncToPlayer(player);
        }
    }

    public void onFrameUpload(ServerPlayer sender, BlockPos origin, long sequence, int width, int height, byte[] compressed) {
        if (!ModConfig.ENABLE_MULTIPLAYER_RELAY.get()) {
            return;
        }
        ServerLevel level = sender.serverLevel();
        WallKey key = new WallKey(level.dimension(), origin);
        UUID elected = wallSenders.get(key);
        if (elected == null || !elected.equals(sender.getUUID())) {
            return;
        }

        latestFrames.put(key, new CachedFrame(sequence, width, height, compressed));

        double range = ModConfig.RELAY_PLAYER_RANGE.get();
        AABB box = new AABB(origin).inflate(range);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box)) {
            if (player.getUUID().equals(sender.getUUID())) {
                continue;
            }
            ScreenFrameChunkHandler.sendToPlayer(
                    ModNetworking.CHANNEL,
                    player,
                    origin,
                    sequence,
                    width,
                    height,
                    compressed
            );
        }
    }

    public void clear() {
        wallSenders.clear();
        cameraCapturers.clear();
        latestFrames.clear();
        tickCounter = 0;
        ScreenFrameChunkHandler.clearServer();
    }

    private void refreshAll(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            refreshLevel(level);
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendSyncToPlayer(player);
        }
    }

    private void refreshLevel(ServerLevel level) {
        int range = ModConfig.RELAY_PLAYER_RANGE.get();
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) {
            return;
        }

        Set<WallKey> activeWalls = new HashSet<>();
        Set<CameraKey> activeCameras = new HashSet<>();

        for (ServerPlayer player : players) {
            AABB box = player.getBoundingBox().inflate(range);
            BlockEntityScanner.forEach(level, box, be -> true, blockEntity -> {
                if (blockEntity instanceof LedScreenBlockEntity screen) {
                    BlockPos origin = screen.getGroupMembership().groupOrigin();
                    if (!origin.equals(blockEntity.getBlockPos())) {
                        return;
                    }
                    if (RelaySources.needsRelay(screen.getSourceId())) {
                        activeWalls.add(new WallKey(level.dimension(), origin));
                    }
                } else if (blockEntity instanceof CameraBlockEntity camera && camera.parameters().enabled()) {
                    activeCameras.add(new CameraKey(level.dimension(), camera.getBlockPos()));
                }
            });
        }

        Map<WallKey, UUID> newWallSenders = new HashMap<>();
        for (WallKey wall : activeWalls) {
            UUID sender = electWallSender(level, wall.origin(), players, range);
            if (sender != null) {
                newWallSenders.put(wall, sender);
            }
        }

        Map<CameraKey, UUID> newCameraCapturers = new HashMap<>();
        for (CameraKey camera : activeCameras) {
            UUID capturer = electCameraCapturer(level, camera.pos(), players, range);
            if (capturer != null) {
                newCameraCapturers.put(camera, capturer);
            }
        }

        wallSenders.keySet().retainAll(activeWalls);
        wallSenders.putAll(newWallSenders);
        cameraCapturers.keySet().retainAll(activeCameras);
        cameraCapturers.putAll(newCameraCapturers);
        latestFrames.keySet().retainAll(activeWalls);
    }

    private static UUID electWallSender(ServerLevel level, BlockPos origin, List<ServerPlayer> players, int range) {
        List<ServerPlayer> nearby = playersNear(level, origin, players, range);
        if (nearby.size() <= 1) {
            return null;
        }

        UUID owner = null;
        BlockEntity be = level.getBlockEntity(origin);
        if (be instanceof LedScreenBlockEntity screen && screen.getOwnerUuid() != null) {
            owner = screen.getOwnerUuid();
        }

        if (owner != null) {
            for (ServerPlayer player : nearby) {
                if (player.getUUID().equals(owner)) {
                    return owner;
                }
            }
        }

        return nearby.stream()
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(origin.getX() + 0.5, origin.getY() + 0.5, origin.getZ() + 0.5)))
                .map(ServerPlayer::getUUID)
                .orElse(null);
    }

    private static UUID electCameraCapturer(ServerLevel level, BlockPos pos, List<ServerPlayer> players, int range) {
        List<ServerPlayer> nearby = playersNear(level, pos, players, range);
        if (nearby.isEmpty()) {
            return null;
        }
        if (nearby.size() == 1) {
            return nearby.get(0).getUUID();
        }
        return nearby.stream()
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)))
                .map(ServerPlayer::getUUID)
                .orElse(null);
    }

    private static List<ServerPlayer> playersNear(ServerLevel level, BlockPos pos, List<ServerPlayer> players, int range) {
        double maxDistSq = (double) range * range;
        List<ServerPlayer> result = new ArrayList<>();
        for (ServerPlayer player : players) {
            if (player.serverLevel() != level) {
                continue;
            }
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= maxDistSq) {
                result.add(player);
            }
        }
        return result;
    }

    private void sendSyncToPlayer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID playerId = player.getUUID();
        Map<BlockPos, WallRelayRole> wallRoles = new HashMap<>();
        Set<BlockPos> captureCameras = new HashSet<>();

        int range = ModConfig.RELAY_PLAYER_RANGE.get();
        AABB box = player.getBoundingBox().inflate(range);

        BlockEntityScanner.forEach(level, box, be -> true, blockEntity -> {
            if (blockEntity instanceof LedScreenBlockEntity screen) {
                BlockPos origin = screen.getGroupMembership().groupOrigin();
                if (!origin.equals(blockEntity.getBlockPos()) || !RelaySources.needsRelay(screen.getSourceId())) {
                    return;
                }
                WallKey key = new WallKey(level.dimension(), origin);
                UUID sender = wallSenders.get(key);
                WallRelayRole role = resolveWallRole(sender, playerId);
                wallRoles.put(origin, role);
            } else if (blockEntity instanceof CameraBlockEntity camera && camera.parameters().enabled()) {
                CameraKey key = new CameraKey(level.dimension(), camera.getBlockPos());
                UUID capturer = cameraCapturers.get(key);
                if (playerId.equals(capturer)) {
                    captureCameras.add(camera.getBlockPos());
                }
            }
        });

        ModNetworking.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new MediaRelaySyncPacket(wallRoles, captureCameras)
        );

        for (Map.Entry<BlockPos, WallRelayRole> entry : wallRoles.entrySet()) {
            if (entry.getValue() != WallRelayRole.RECEIVE) {
                continue;
            }
            WallKey key = new WallKey(level.dimension(), entry.getKey());
            CachedFrame cached = latestFrames.get(key);
            if (cached != null) {
                ScreenFrameChunkHandler.sendToPlayer(
                        ModNetworking.CHANNEL,
                        player,
                        entry.getKey(),
                        cached.sequence(),
                        cached.width(),
                        cached.height(),
                        cached.compressed()
                );
            }
        }
    }

    private static WallRelayRole resolveWallRole(UUID sender, UUID playerId) {
        if (sender == null) {
            return WallRelayRole.LOCAL;
        }
        if (sender.equals(playerId)) {
            return WallRelayRole.UPLOAD;
        }
        return WallRelayRole.RECEIVE;
    }

    private record WallKey(ResourceKey<Level> dimension, BlockPos origin) {
    }

    private record CameraKey(ResourceKey<Level> dimension, BlockPos pos) {
    }

    private record CachedFrame(long sequence, int width, int height, byte[] compressed) {
    }
}
