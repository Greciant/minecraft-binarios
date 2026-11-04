package com.devilfruits.fruits.kage_kage;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public final class KageTags {
   public static final TagKey<EntityType<?>> NO_SHADOW = TagKey.create(Registries.ENTITY_TYPE, KageKage.id("no_shadow"));
   public static final TagKey<EntityType<?>> FLESHY = TagKey.create(Registries.ENTITY_TYPE, KageKage.id("fleshy"));

   private KageTags() {
   }
}
