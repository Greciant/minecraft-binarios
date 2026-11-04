package com.devilfruits.registry;

import com.devilfruits.DevilFruits;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class DFTags {
   public static final TagKey<Item> DEVIL_FRUITS = TagKey.create(Registries.ITEM, DevilFruits.id("devil_fruits"));
   public static final TagKey<Item> CASKET_KEYS = TagKey.create(Registries.ITEM, DevilFruits.id("casket_keys"));
   public static final TagKey<Item> SEASTONE_ITEMS = TagKey.create(Registries.ITEM, DevilFruits.id("seastone"));
   public static final TagKey<Block> SEASTONE_BLOCKS = TagKey.create(Registries.BLOCK, DevilFruits.id("seastone"));
   public static final TagKey<Block> DESTRUCTION_IMMUNE = TagKey.create(Registries.BLOCK, DevilFruits.id("destruction_immune"));
   public static final TagKey<Block> RESISTS_TECHNIQUES = TagKey.create(Registries.BLOCK, DevilFruits.id("resists_techniques"));
   public static final TagKey<Item> SEASTONE_WEAPONS = TagKey.create(Registries.ITEM, DevilFruits.id("seastone_weapon"));
   public static final TagKey<Item> LOGIA_BYPASS_ITEMS = TagKey.create(Registries.ITEM, DevilFruits.id("logia_bypass"));
   public static final TagKey<DamageType> LOGIA_BYPASS_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE, DevilFruits.id("logia_bypass"));
   public static final TagKey<DamageType> LOGIA_WATER_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE, DevilFruits.id("logia_water"));
   public static final TagKey<DamageType> DEVIL_FRUIT_ATTACKS = TagKey.create(Registries.DAMAGE_TYPE, DevilFruits.id("devil_fruit_attacks"));

   private DFTags() {
   }
}
