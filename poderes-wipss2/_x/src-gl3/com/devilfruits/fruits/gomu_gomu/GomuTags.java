package com.devilfruits.fruits.gomu_gomu;

import com.devilfruits.DevilFruits;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

public final class GomuTags {
   public static final TagKey<Item> SHARP_WEAPONS = TagKey.create(Registries.ITEM, DevilFruits.id("sharp_weapons"));
   public static final TagKey<Item> BLUNT_WEAPONS = TagKey.create(Registries.ITEM, DevilFruits.id("blunt_weapons"));
   public static final TagKey<EntityType<?>> BLUNT_ATTACKERS = TagKey.create(Registries.ENTITY_TYPE, DevilFruits.id("blunt_attackers"));
   public static final TagKey<EntityType<?>> SHARP_ATTACKERS = TagKey.create(Registries.ENTITY_TYPE, DevilFruits.id("sharp_attackers"));
   public static final TagKey<EntityType<?>> BULLETS = TagKey.create(Registries.ENTITY_TYPE, DevilFruits.id("bullets"));
   public static final TagKey<DamageType> BLUNT_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE, DevilFruits.id("blunt"));
   public static final TagKey<DamageType> ELECTRIC = TagKey.create(Registries.DAMAGE_TYPE, DevilFruits.id("electric"));

   private GomuTags() {
   }
}
