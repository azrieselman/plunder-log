package dev.tyty.plunderlog.world;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record ClearedMark(
        ResourceLocation structure,
        int chunkX,
        int chunkZ,
        BlockPos pos,
        long gameTime,
        long clearedAtMillis,
        String reason) {

    public String key() {
        return structure + "@" + chunkX + "," + chunkZ;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", structure.toString());
        tag.putInt("ChunkX", chunkX);
        tag.putInt("ChunkZ", chunkZ);
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        tag.putLong("GameTime", gameTime);
        tag.putLong("UnixTime", clearedAtMillis);
        tag.putString("Reason", reason);
        return tag;
    }

    public static ClearedMark load(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.parse(tag.getString("Id"));
        return new ClearedMark(
                id,
                tag.getInt("ChunkX"),
                tag.getInt("ChunkZ"),
                new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")),
                tag.getLong("GameTime"),
                tag.getLong("UnixTime"),
                tag.getString("Reason"));
    }
}
