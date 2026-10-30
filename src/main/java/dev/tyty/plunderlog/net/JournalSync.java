package dev.tyty.plunderlog.net;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import dev.tyty.plunderlog.world.PlunderSavedData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class JournalSync {
    private JournalSync() {}

    public static void send(ServerPlayer player, boolean openScreen) {
        PacketDistributor.sendToPlayer(player, new SyncJournalPayload(collect(player.server), openScreen));
    }

    public static void sendAll(MinecraftServer server, boolean openScreen) {
        PacketDistributor.sendToAllPlayers(new SyncJournalPayload(collect(server), openScreen));
    }

    public static List<JournalEntry> collect(MinecraftServer server) {
        List<JournalEntry> entries = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            ResourceLocation dimension = level.dimension().location();
            for (var mark : PlunderSavedData.get(level).marks()) {
                entries.add(JournalEntry.from(mark, dimension));
            }
        }
        entries.sort(Comparator.comparingLong(JournalEntry::clearedAtMillis).reversed());
        return List.copyOf(entries);
    }
}
