package dev.tyty.plunderlog.world;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.tyty.plunderlog.PlunderConfig;
import dev.tyty.plunderlog.PlunderLog;
import dev.tyty.plunderlog.PlunderTags;
import dev.tyty.plunderlog.StructureNames;
import dev.tyty.plunderlog.net.JournalSync;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class ClearTracker {
    public static final String REASON_CHESTS = "chests";
    public static final String REASON_BOSS = "boss";

    private ClearTracker() {}

    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!PlunderConfig.CLEAR_MODE.get().chests()) {
            return;
        }
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        int margin = PlunderConfig.CHEST_RANGE.get();
        for (BlockPos pos : containerPositions(player, event.getContainer())) {
            try {
                StructureQueries.findBest(level, pos, margin).ifPresent(hit -> tryChests(level, hit));
            } catch (RuntimeException exception) {
                PlunderLog.LOGGER.error("Failed to check loot at {}", pos, exception);
            }
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!PlunderConfig.CLEAR_MODE.get().bosses()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !entity.getType().is(PlunderTags.STRUCTURE_BOSSES)) {
            return;
        }
        BlockPos pos = entity.blockPosition();
        try {
            StructureQueries.findBest(level, pos, 0).ifPresent(hit -> mark(level, hit, REASON_BOSS));
        } catch (RuntimeException exception) {
            PlunderLog.LOGGER.error("Failed to check boss death at {}", pos, exception);
        }
    }

    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            JournalSync.send(player, false);
        }
    }

    private static void tryChests(ServerLevel level, StructureQueries.Hit hit) {
        String key = new ClearedMark(hit.id(), hit.start().getChunkPos().x, hit.start().getChunkPos().z, hit.center(), 0L, 0L, REASON_CHESTS).key();
        if (PlunderSavedData.get(level).isCleared(key)) {
            return;
        }
        StructureQueries.ChestStatus status = StructureQueries.chestStatus(level, hit.start());
        if (status == StructureQueries.ChestStatus.READY) {
            mark(level, hit, REASON_CHESTS);
        } else {
            PlunderLog.LOGGER.debug("Structure {} at {} is not cleared yet ({})", hit.id(), hit.center(), status);
        }
    }

    private static void mark(ServerLevel level, StructureQueries.Hit hit, String reason) {
        ClearedMark cleared = new ClearedMark(
                hit.id(),
                hit.start().getChunkPos().x,
                hit.start().getChunkPos().z,
                hit.center(),
                level.getGameTime(),
                System.currentTimeMillis(),
                reason);
        if (!PlunderSavedData.get(level).mark(cleared)) {
            return;
        }
        PlunderLog.LOGGER.info("Cleared {} at {} in {}", hit.id(), hit.center(), level.dimension().location());
        Component message = Component.translatable(
                "plunderlog.message.cleared",
                StructureNames.structure(hit.id()),
                StructureNames.dimension(level.dimension().location()),
                hit.center().getX(),
                hit.center().getY(),
                hit.center().getZ(),
                Component.translatable(reason.equals(REASON_BOSS) ? "plunderlog.reason.boss" : "plunderlog.reason.chests"));
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.sendSystemMessage(message);
        }
        JournalSync.sendAll(level.getServer(), false);
    }

    private static List<BlockPos> containerPositions(Player player, AbstractContainerMenu menu) {
        List<BlockPos> positions = new ArrayList<>();
        Set<Container> seen = new HashSet<>();
        for (Slot slot : menu.slots) {
            Container container = slot.container;
            if (container == null || container == player.getInventory() || !seen.add(container)) {
                continue;
            }
            if (container instanceof BlockEntity blockEntity && StructureQueries.isTrackedLoot(blockEntity)) {
                positions.add(blockEntity.getBlockPos().immutable());
                continue;
            }
            if (container instanceof CompoundContainer compound && player.level() instanceof ServerLevel level) {
                BlockPos origin = player.blockPosition();
                for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -3, -8), origin.offset(8, 3, 8))) {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (blockEntity instanceof Container chest && compound.contains(chest) && StructureQueries.isTrackedLoot(blockEntity)) {
                        positions.add(blockEntity.getBlockPos().immutable());
                    }
                }
            }
        }
        return positions;
    }
}
