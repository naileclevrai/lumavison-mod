package fr.lumavision.screen;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side hooks that keep merged screen groups consistent when chunks load.
 * <p>
 * Rebuilds are deferred to the server tick to avoid accessing neighbouring chunks
 * while a chunk is still loading (which can deadlock world reload).
 */
@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScreenGroupForgeEvents {

    private static final Set<RebuildRequest> PENDING_REBUILDS = ConcurrentHashMap.newKeySet();

    private ScreenGroupForgeEvents() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }

        if (!(event.getLevel() instanceof Level level)) {
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof LedScreenBlockEntity) {
                PENDING_REBUILDS.add(new RebuildRequest(dimension, blockEntity.getBlockPos()));
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_REBUILDS.isEmpty()) {
            return;
        }

        Set<RebuildRequest> batch = Set.copyOf(PENDING_REBUILDS);
        PENDING_REBUILDS.clear();

        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }

        Set<BlockPos> processed = new HashSet<>();

        for (RebuildRequest request : batch) {
            ServerLevel level = server.getLevel(request.dimension());
            if (level == null || !level.hasChunkAt(request.pos())) {
                continue;
            }

            if (processed.contains(request.pos())) {
                continue;
            }

            Set<Set<BlockPos>> components = ScreenGroupResolver.findAffectedComponents(level, request.pos());
            for (Set<BlockPos> component : components) {
                processed.addAll(component);
            }
            ScreenGroupManager.rebuildAround(level, request.pos());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        PENDING_REBUILDS.clear();
    }

    private record RebuildRequest(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
