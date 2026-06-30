package fr.lumavision.screen;

import fr.lumavision.LumaVisionMod;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * Server-side hooks that keep merged screen groups consistent when chunks load.
 */
@Mod.EventBusSubscriber(modid = LumaVisionMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ScreenGroupForgeEvents {

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

        Set<BlockPos> ledScreens = new HashSet<>();
        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof LedScreenBlockEntity) {
                ledScreens.add(blockEntity.getBlockPos());
            }
        }

        if (ledScreens.isEmpty()) {
            return;
        }

        Set<BlockPos> processed = new HashSet<>();
        for (BlockPos pos : ledScreens) {
            if (processed.contains(pos)) {
                continue;
            }
            Set<Set<BlockPos>> components = ScreenGroupResolver.findAffectedComponents(level, pos);
            for (Set<BlockPos> component : components) {
                processed.addAll(component);
            }
            ScreenGroupManager.rebuildAround(level, pos);
        }
    }
}
