package dev.tyty.plunderlog.net;

import java.util.List;

import dev.tyty.plunderlog.PlunderLog;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncJournalPayload(List<JournalEntry> entries, boolean openScreen) implements CustomPacketPayload {
    public static final Type<SyncJournalPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(PlunderLog.MOD_ID, "sync_journal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncJournalPayload> STREAM_CODEC = StreamCodec.composite(
            JournalEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SyncJournalPayload::entries,
            ByteBufCodecs.BOOL,
            SyncJournalPayload::openScreen,
            SyncJournalPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
