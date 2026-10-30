package dev.tyty.plunderlog.world;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import dev.tyty.plunderlog.PlunderConfig;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public final class StructureQueries {
    public enum ChestStatus {
        NO_LOOT,
        REMAINING,
        UNLOADED,
        READY
    }

    public record Hit(StructureStart start, ResourceLocation id) {
        public BlockPos center() {
            BoundingBox box = start.getBoundingBox();
            return new BlockPos(
                    (box.minX() + box.maxX()) / 2,
                    (box.minY() + box.maxY()) / 2,
                    (box.minZ() + box.maxZ()) / 2);
        }
    }

    private StructureQueries() {}

    public static Optional<Hit> findBest(ServerLevel level, BlockPos pos, int margin) {
        StructureManager manager = level.structureManager();
        int chunkRadius = Math.max(0, (margin + 15) >> 4);
        ChunkPos origin = new ChunkPos(pos);
        Hit best = null;
        int bestVolume = Integer.MAX_VALUE;
        Set<StartKey> seen = new HashSet<>();

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                int chunkX = origin.x + dx;
                int chunkZ = origin.z + dz;
                if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                    continue;
                }
                BlockPos probe = new BlockPos(chunkX << 4, pos.getY(), chunkZ << 4);
                Map<Structure, LongSet> references = manager.getAllStructuresAt(probe);
                for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
                    Structure structure = entry.getKey();
                    ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
                    if (id == null) {
                        continue;
                    }
                    var found = new java.util.ArrayList<StructureStart>();
                    manager.fillStartsForStructure(structure, entry.getValue(), start -> {
                        if (start != null && start.isValid()) {
                            found.add(start);
                        }
                    });
                    for (StructureStart start : found) {
                        ChunkPos startChunk = start.getChunkPos();
                        if (!seen.add(new StartKey(id, startChunk.x, startChunk.z))) {
                            continue;
                        }
                        if (!nearPiece(start, pos, margin)) {
                            continue;
                        }
                        int volume = volume(start.getBoundingBox());
                        if (volume < bestVolume) {
                            bestVolume = volume;
                            best = new Hit(start, id);
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(best);
    }

    public static ChestStatus chestStatus(ServerLevel level, StructureStart start) {
        int margin = PlunderConfig.CHEST_RANGE.get();
        LongOpenHashSet chunks = new LongOpenHashSet();
        for (StructurePiece piece : start.getPieces()) {
            BoundingBox box = piece.getBoundingBox();
            int minChunkX = (box.minX() - margin) >> 4;
            int maxChunkX = (box.maxX() + margin) >> 4;
            int minChunkZ = (box.minZ() - margin) >> 4;
            int maxChunkZ = (box.maxZ() + margin) >> 4;
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    chunks.add(ChunkPos.asLong(chunkX, chunkZ));
                }
            }
        }

        boolean unloaded = false;
        int loot = 0;
        int pending = 0;
        for (long packed : chunks) {
            int chunkX = ChunkPos.getX(packed);
            int chunkZ = ChunkPos.getZ(packed);
            if (!level.getChunkSource().hasChunk(chunkX, chunkZ)) {
                unloaded = true;
                continue;
            }
            LevelChunk chunk = level.getChunk(chunkX, chunkZ);
            for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                if (!isTrackedLoot(blockEntity) || !nearPiece(start, blockEntity.getBlockPos(), margin)) {
                    continue;
                }
                loot++;
                if (!isEmptied((RandomizableContainerBlockEntity) blockEntity)) {
                    pending++;
                }
            }
        }
        if (loot == 0) {
            return ChestStatus.NO_LOOT;
        }
        if (pending > 0) {
            return ChestStatus.REMAINING;
        }
        if (unloaded) {
            return ChestStatus.UNLOADED;
        }
        return ChestStatus.READY;
    }

    public static boolean isTrackedLoot(BlockEntity blockEntity) {
        if (blockEntity instanceof HopperBlockEntity || blockEntity instanceof CrafterBlockEntity) {
            return false;
        }
        if (blockEntity instanceof DispenserBlockEntity dispenser) {
            return dispenser.getLootTable() != null || !dispenser.isEmpty();
        }
        return blockEntity instanceof ChestBlockEntity
                || blockEntity instanceof BarrelBlockEntity
                || blockEntity instanceof ShulkerBoxBlockEntity;
    }

    private static boolean isEmptied(RandomizableContainerBlockEntity container) {
        return container.getLootTable() == null && container.isEmpty();
    }

    static boolean nearPiece(StructureStart start, BlockPos pos, int margin) {
        for (StructurePiece piece : start.getPieces()) {
            BoundingBox box = piece.getBoundingBox();
            if (pos.getX() >= box.minX() - margin && pos.getX() <= box.maxX() + margin
                    && pos.getY() >= box.minY() - margin && pos.getY() <= box.maxY() + margin
                    && pos.getZ() >= box.minZ() - margin && pos.getZ() <= box.maxZ() + margin) {
                return true;
            }
        }
        return false;
    }

    private static int volume(BoundingBox box) {
        long volume = (long) box.getXSpan() * box.getYSpan() * box.getZSpan();
        return volume > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) volume;
    }

    private record StartKey(ResourceLocation id, int chunkX, int chunkZ) {}
}
