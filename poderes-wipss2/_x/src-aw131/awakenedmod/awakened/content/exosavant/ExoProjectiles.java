package com.awakenedmod.awakened.content.exosavant;

import com.awakenedmod.awakened.content.AwakenedEntities;
import com.awakenedmod.awakened.content.entity.projectile.KineticProjectile;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class ExoProjectiles {
   private ExoProjectiles() {
   }

   public static KineticProjectile spawnShell(
      ServerPlayer owner,
      ServerLevel level,
      Item visual,
      Vec3 origin,
      Vec3 dir,
      double speed,
      float inaccuracy,
      float impactDamage,
      float aoeDamage,
      float aoeRadius,
      float knockback,
      double gravity,
      double contactRadius,
      LivingEntity homingTarget,
      double homingStrength
   ) {
      return spawnShell(
         owner,
         level,
         visual,
         origin,
         dir,
         speed,
         inaccuracy,
         impactDamage,
         aoeDamage,
         aoeRadius,
         knockback,
         gravity,
         contactRadius,
         homingTarget,
         homingStrength,
         pos -> ExoFx.blast(level, pos, (double)aoeRadius)
      );
   }

   public static KineticProjectile spawnShell(
      ServerPlayer owner,
      ServerLevel level,
      Item visual,
      Vec3 origin,
      Vec3 dir,
      double speed,
      float inaccuracy,
      float impactDamage,
      float aoeDamage,
      float aoeRadius,
      float knockback,
      double gravity,
      double contactRadius,
      LivingEntity homingTarget,
      double homingStrength,
      Consumer<Vec3> impactFx
   ) {
      return spawnShell(
         AwakenedEntities.KINETIC_CHARGE.get(),
         owner,
         level,
         visual,
         origin,
         dir,
         speed,
         inaccuracy,
         impactDamage,
         aoeDamage,
         aoeRadius,
         knockback,
         gravity,
         contactRadius,
         homingTarget,
         homingStrength,
         impactFx
      );
   }

   public static KineticProjectile spawnShell(
      EntityType<? extends KineticProjectile> type,
      ServerPlayer owner,
      ServerLevel level,
      Item visual,
      Vec3 origin,
      Vec3 dir,
      double speed,
      float inaccuracy,
      float impactDamage,
      float aoeDamage,
      float aoeRadius,
      float knockback,
      double gravity,
      double contactRadius,
      LivingEntity homingTarget,
      double homingStrength,
      Consumer<Vec3> impactFx
   ) {
      KineticProjectile shell = new KineticProjectile(type, owner, level, new ItemStack(visual));
      shell.setPos(origin.x, origin.y, origin.z);
      shell.configure(impactDamage, aoeDamage, aoeRadius, knockback);
      shell.setGravity(gravity);
      shell.setContactRadius(contactRadius);
      shell.setCustomDetonation(true);
      shell.setOnTick(pos -> ExoFx.exhaustTrail(level, pos));
      shell.setOnDetonate(pos -> {
         impactFx.accept(pos);
         if (shell.directHit() instanceof LivingEntity hit) {
            ExoTags.applyTaggedBonus(owner, hit, level, impactDamage);
         }
      });
      if (homingTarget != null) {
         shell.setHoming(homingTarget, homingStrength, speed);
      }

      shell.shoot(dir.x, dir.y, dir.z, (float)speed, inaccuracy);
      level.addFreshEntity(shell);
      return shell;
   }
}
