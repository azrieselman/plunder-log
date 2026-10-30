package dev.tyty.plunderlog;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class PlunderTags {
    public static final TagKey<EntityType<?>> STRUCTURE_BOSSES = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(PlunderLog.MOD_ID, "structure_bosses"));

    private PlunderTags() {}
}
