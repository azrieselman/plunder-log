package dev.tyty.plunderlog.world;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class PlunderSavedData extends SavedData {
    private static final SavedData.Factory<PlunderSavedData> FACTORY = new SavedData.Factory<>(
            PlunderSavedData::new,
            PlunderSavedData::load,
            null);

    private final Map<String, ClearedMark> marks = new LinkedHashMap<>();

    public static PlunderSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, "plunderlog_cleared");
    }

    public boolean isCleared(String key) {
        return marks.containsKey(key);
    }

    public boolean mark(ClearedMark mark) {
        if (marks.containsKey(mark.key())) {
            return false;
        }
        marks.put(mark.key(), mark);
        setDirty();
        return true;
    }

    public Collection<ClearedMark> marks() {
        return marks.values();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (ClearedMark mark : marks.values()) {
            list.add(mark.save());
        }
        tag.put("Cleared", list);
        return tag;
    }

    public static PlunderSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PlunderSavedData data = new PlunderSavedData();
        ListTag list = tag.getList("Cleared", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.contains("Id", Tag.TAG_STRING)) {
                continue;
            }
            try {
                ClearedMark mark = ClearedMark.load(entry);
                data.marks.put(mark.key(), mark);
            } catch (RuntimeException ignored) {
                // Skip a corrupt entry rather than dropping the whole log.
            }
        }
        return data;
    }
}
