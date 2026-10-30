package dev.tyty.plunderlog.net;

import dev.tyty.plunderlog.world.ClearedMark;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record JournalEntry(
        ResourceLocation structure,
        ResourceLocation dimension,
        int chunkX,
        int chunkZ,
        BlockPos pos,
        long gameTime,
        long clearedAtMillis,
        String reason) {

    public static final StreamCodec<RegistryFriendlyByteBuf, JournalEntry> STREAM_CODEC = StreamCodec.of(
            (buf, entry) -> {
                ResourceLocation.STREAM_CODEC.encode(buf, entry.structure);
                ResourceLocation.STREAM_CODEC.encode(buf, entry.dimension);
                buf.writeVarInt(entry.chunkX);
                buf.writeVarInt(entry.chunkZ);
                BlockPos.STREAM_CODEC.encode(buf, entry.pos);
                buf.writeVarLong(entry.gameTime);
                buf.writeVarLong(entry.clearedAtMillis);
                buf.writeUtf(entry.reason);
            },
            buf -> new JournalEntry(
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    ResourceLocation.STREAM_CODEC.decode(buf),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    BlockPos.STREAM_CODEC.decode(buf),
                    buf.readVarLong(),
                    buf.readVarLong(),
                    buf.readUtf()));

    public static JournalEntry from(ClearedMark mark, ResourceLocation dimension) {
        return new JournalEntry(
                mark.structure(),
                dimension,
                mark.chunkX(),
                mark.chunkZ(),
                mark.pos(),
                mark.gameTime(),
                mark.clearedAtMillis(),
                mark.reason());
    }

    public String waypointId() {
        return structure + "@" + chunkX + "," + chunkZ + "@" + dimension;
    }
}
