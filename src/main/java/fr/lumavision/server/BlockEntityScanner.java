package fr.lumavision.server;

import fr.lumavision.blockentity.CameraBlockEntity;
import fr.lumavision.blockentity.LedScreenBlockEntity;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;

import java.util.function.Predicate;

/**
 * Iterates block entities inside an AABB without relying on non-vanilla Level helpers.
 */
final class BlockEntityScanner {

    private BlockEntityScanner() {
    }

    static void forEach(Level level, AABB box, Predicate<BlockEntity> filter, BlockEntityConsumer consumer) {
        int minX = Mth.floor(box.minX);
        int minY = Mth.floor(box.minY);
        int minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX);
        int maxY = Mth.floor(box.maxY);
        int maxZ = Mth.floor(box.maxZ);

        int chunkMinX = minX >> 4;
        int chunkMaxX = maxX >> 4;
        int chunkMinZ = minZ >> 4;
        int chunkMaxZ = maxZ >> 4;

        for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
            for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    BlockPos pos = blockEntity.getBlockPos();
                    if (pos.getX() < minX || pos.getX() > maxX
                            || pos.getY() < minY || pos.getY() > maxY
                            || pos.getZ() < minZ || pos.getZ() > maxZ) {
                        continue;
                    }
                    if (filter == null || filter.test(blockEntity)) {
                        consumer.accept(blockEntity);
                    }
                }
            }
        }
    }

    @FunctionalInterface
    interface BlockEntityConsumer {
        void accept(BlockEntity blockEntity);
    }
}
