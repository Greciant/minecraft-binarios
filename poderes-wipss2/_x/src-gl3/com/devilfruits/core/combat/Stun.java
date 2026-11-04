package com.devilfruits.core.combat;

import com.devilfruits.core.fx.CoreFx;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.ServerScheduler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class Stun {
   private static final Map<UUID, Long> UNTIL = new HashMap<>();

   public static void apply(LivingEntity target, int ticks) {
      apply(target, ticks, true);
   }

   public static void apply(LivingEntity target, int ticks, boolean label) {
      if (ticks > 0 && target.level() instanceof ServerLevel level) {
         long var9 = level.getGameTime();
         Long current = UNTIL.get(target.getUUID());
         long until = Math.max(var9 + (long)ticks, current == null ? 0L : current);
         UNTIL.put(target.getUUID(), until);
         target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (int)(until - var9), 9, false, false, false));
         Interrupt.apply(target);
         if (label) {
            ServerFx.send(target, CoreFx.STUN, target.position(), Vec3.ZERO, 0.0F, 0L, (int)(until - var9));
         }

         if (current == null) {
            ServerScheduler.schedule(level, 0, age -> hold(target));
         }
      }
   }

   public static void release(LivingEntity target) {
      if (UNTIL.remove(target.getUUID()) != null) {
         target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
         ServerFx.send(target, CoreFx.STUN, target.position(), Vec3.ZERO, 0.0F, 0L, 0);
      }
   }

   public static boolean stunned(LivingEntity target) {
      Long until = UNTIL.get(target.getUUID());
      return until != null && until > target.level().getGameTime();
   }

   private static boolean hold(LivingEntity target) {
      Long until = UNTIL.get(target.getUUID());
      if (until == null) {
         return true;
      } else if (target.isAlive() && !target.isRemoved() && target.level().getGameTime() < until) {
         Vec3 v = target.getDeltaMovement();
         if (v.x * v.x + v.z * v.z > 1.0E-6 || v.y > 0.0) {
            target.setDeltaMovement(0.0, Math.min(0.0, v.y), 0.0);
            target.hurtMarked = true;
         }

         target.setJumping(false);
         if (target instanceof Mob mob) {
            mob.getNavigation().stop();
            mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0);
         }

         return false;
      } else {
         UNTIL.remove(target.getUUID());
         return true;
      }
   }

   private Stun() {
   }
}
