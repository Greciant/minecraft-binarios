package com.devilfruits.registry;

import com.devilfruits.DevilFruits;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class DFDamageTypes {
   public static final ResourceKey<DamageType> DEVIL_FRUIT = key("devil_fruit");
   public static final ResourceKey<DamageType> SECOND_FRUIT = key("second_fruit");
   public static final ResourceKey<DamageType> HAKI_RYUO = key("haki_ryuo");

   public static ResourceKey<DamageType> key(String path) {
      return ResourceKey.create(Registries.DAMAGE_TYPE, DevilFruits.id(path));
   }

   public static DamageSource source(Level level, ResourceKey<DamageType> type, @Nullable Entity attacker) {
      return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), attacker);
   }

   public static DamageSource secondFruit(Level level) {
      return source(level, SECOND_FRUIT, null);
   }

   private DFDamageTypes() {
   }
}
