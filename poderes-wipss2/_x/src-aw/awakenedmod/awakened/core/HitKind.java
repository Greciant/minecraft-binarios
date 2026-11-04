package com.awakenedmod.awakened.core;

import javax.annotation.Nullable;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

public final class HitKind {
   private HitKind() {
   }

   public static HitKind.Shape shape(DamageSource source) {
      Entity direct = source.getDirectEntity();
      Entity causing = source.getEntity();
      if (direct instanceof Projectile || direct instanceof OwnedProjectile) {
         return HitKind.Shape.PROJECTILE;
      } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
         return HitKind.Shape.EXPLOSION;
      } else if (direct != null && direct == causing && causing instanceof LivingEntity) {
         return HitKind.Shape.BODY;
      } else {
         return direct == null && source.is(DamageTypes.PLAYER_ATTACK) && causing instanceof LivingEntity ? HitKind.Shape.FIST : HitKind.Shape.OTHER;
      }
   }

   @Nullable
   public static LivingEntity responsible(LivingEntity victim, DamageSource source) {
      if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.is(DamageTypes.THORNS)) {
         Entity causing = source.getEntity();
         if (causing == null) {
            causing = shooterOf(source.getDirectEntity());
         }

         if (causing instanceof LivingEntity attacker && attacker != victim && Harm.allowed(victim, attacker)) {
            return attacker;
         }

         return null;
      } else {
         return null;
      }
   }

   @Nullable
   public static Entity shooterOf(@Nullable Entity shot) {
      if (shot instanceof Projectile projectile) {
         return projectile.getOwner();
      } else {
         return shot instanceof OwnedProjectile owned ? owned.shooter() : null;
      }
   }

   public static boolean inReach(Entity victim, Entity attacker, double reach) {
      return victim.distanceToSqr(attacker) <= reach * reach;
   }

   public static enum Shape {
      PROJECTILE,
      EXPLOSION,
      BODY,
      FIST,
      OTHER;
   }
}
