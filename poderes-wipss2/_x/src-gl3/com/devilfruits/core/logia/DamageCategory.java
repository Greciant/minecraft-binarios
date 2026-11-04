package com.devilfruits.core.logia;

import com.devilfruits.registry.DFTags;
import java.util.Locale;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

public enum DamageCategory {
   MELEE,
   PROJECTILE,
   EXPLOSION,
   FALL,
   FIRE,
   LIGHTNING,
   FREEZE,
   MAGIC,
   ABILITY,
   WATER,
   ENVIRONMENT,
   ALWAYS,
   OTHER;

   public String configName() {
      return this.name().toLowerCase(Locale.ROOT);
   }

   public static DamageCategory of(DamageSource source) {
      if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
         || source.is(DamageTypeTags.IS_DROWNING)
         || source.is(DamageTypes.STARVE)
         || source.is(DamageTypes.IN_WALL)
         || source.is(DamageTypes.CRAMMING)
         || source.is(DamageTypes.OUTSIDE_BORDER)
         || source.is(DamageTypes.GENERIC_KILL)
         || source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
         return ALWAYS;
      } else if (source.is(DFTags.LOGIA_WATER_DAMAGE)) {
         return WATER;
      } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
         return EXPLOSION;
      } else if (source.is(DamageTypeTags.IS_FIRE)) {
         return FIRE;
      } else if (source.is(DamageTypeTags.IS_LIGHTNING)) {
         return LIGHTNING;
      } else if (source.is(DamageTypeTags.IS_FREEZING)) {
         return FREEZE;
      } else if (source.is(DFTags.DEVIL_FRUIT_ATTACKS)) {
         return ABILITY;
      } else if (source.is(DamageTypeTags.IS_PROJECTILE)) {
         return PROJECTILE;
      } else if (source.is(DamageTypeTags.IS_FALL)) {
         return FALL;
      } else if (source.is(DamageTypes.MAGIC)
         || source.is(DamageTypes.INDIRECT_MAGIC)
         || source.is(DamageTypes.WITHER)
         || source.is(DamageTypes.DRAGON_BREATH)
         || source.is(DamageTypes.SONIC_BOOM)
         || source.is(DamageTypes.THORNS)) {
         return MAGIC;
      } else if (!source.is(DamageTypeTags.IS_PLAYER_ATTACK)
         && !source.is(DamageTypes.MOB_ATTACK)
         && !source.is(DamageTypes.MOB_ATTACK_NO_AGGRO)
         && !source.is(DamageTypes.STING)
         && (!(source.getDirectEntity() instanceof LivingEntity) || source.getDirectEntity() != source.getEntity())) {
         return !source.is(DamageTypes.CACTUS)
               && !source.is(DamageTypes.SWEET_BERRY_BUSH)
               && !source.is(DamageTypes.STALAGMITE)
               && !source.is(DamageTypes.FALLING_BLOCK)
               && !source.is(DamageTypes.FALLING_ANVIL)
               && !source.is(DamageTypes.FALLING_STALACTITE)
               && !source.is(DamageTypes.FLY_INTO_WALL)
            ? OTHER
            : ENVIRONMENT;
      } else {
         return MELEE;
      }
   }
}
