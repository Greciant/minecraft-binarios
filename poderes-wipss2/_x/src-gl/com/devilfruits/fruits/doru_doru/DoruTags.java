package com.devilfruits.fruits.doru_doru;

import com.devilfruits.DevilFruits;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;

public final class DoruTags {
   public static final TagKey<Block> WAX_HEAT = TagKey.create(Registries.BLOCK, DevilFruits.id("wax_heat"));
   public static final TagKey<DamageType> HEAT_DAMAGE = TagKey.create(Registries.DAMAGE_TYPE, DevilFruits.id("heat_damage"));
   public static final TagKey<DimensionType> TOO_HOT = TagKey.create(Registries.DIMENSION_TYPE, DevilFruits.id("too_hot"));

   private DoruTags() {
   }
}
