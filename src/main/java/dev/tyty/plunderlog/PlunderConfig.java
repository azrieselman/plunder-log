package dev.tyty.plunderlog;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PlunderConfig {
    public enum ClearMode {
        CHESTS,
        BOSSES,
        BOTH;

        public boolean chests() {
            return this == CHESTS || this == BOTH;
        }

        public boolean bosses() {
            return this == BOSSES || this == BOTH;
        }
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<ClearMode> CLEAR_MODE = BUILDER
            .comment(
                    "What marks a structure cleared.",
                    "CHESTS: every loot chest, barrel, and shulker box in the structure pieces is empty, and those piece chunks are loaded.",
                    "BOSSES: a mob in the plunderlog:structure_bosses entity tag dies inside a structure piece.",
                    "BOTH: either condition is enough.")
            .defineEnum("clearMode", ClearMode.BOTH);

    public static final ModConfigSpec.IntValue CHEST_RANGE = BUILDER
            .comment("How many blocks outside a structure piece a chest, barrel, or shulker box may sit and still count as part of that structure.")
            .defineInRange("chestRange", 4, 0, 64);

    static final ModConfigSpec SPEC = BUILDER.build();

    private PlunderConfig() {}
}
