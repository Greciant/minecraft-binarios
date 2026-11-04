package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.rules.PvpRules;
import com.awakenedmod.awakened.core.team.Teams;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class Harm {
   public static final String CONTROL_REFUSED = "awakened.feedback.pvp_control_off";

   private Harm() {
   }

   public static boolean allowed(Entity source, Entity target) {
      return !Teams.harmForbidden(source, target);
   }

   public static boolean controls(Entity source, Entity target) {
      return allowed(source, target) && !PvpRules.blocksControl(source, target);
   }

   public static <T extends Entity> List<T> only(Entity source, Collection<T> candidates) {
      return filter(source, candidates, Harm::allowed);
   }

   public static <T extends Entity> List<T> controllable(Entity source, Collection<T> candidates) {
      return filter(source, candidates, Harm::controls);
   }

   private static <T extends Entity> List<T> filter(Entity source, Collection<T> candidates, BiPredicate<Entity, Entity> rule) {
      List<T> out = new ArrayList<>(candidates.size());

      for (T candidate : candidates) {
         if (rule.test(source, candidate)) {
            out.add(candidate);
         }
      }

      return out;
   }

   public static <T extends Entity> List<T> hostilesOnly(ServerPlayer player, Collection<T> candidates) {
      List<T> out = new ArrayList<>(candidates.size());

      for (T candidate : candidates) {
         if (allowed(player, candidate) && !Foes.fightsFor(player, candidate)) {
            out.add(candidate);
         }
      }

      return out;
   }

   public static boolean effect(Entity source, LivingEntity target, MobEffectInstance instance) {
      return allowed(source, target) && !PvpRules.blocksEffect(source, target, instance) ? target.addEffect(instance, source) : false;
   }

   public static boolean effect(Entity source, LivingEntity target, Holder<MobEffect> effect, int durationTicks, int amplifier) {
      return effect(source, target, new MobEffectInstance(effect, durationTicks, amplifier));
   }

   public static boolean push(Entity source, Entity target, double dx, double dy, double dz) {
      if (!allowed(source, target)) {
         return false;
      } else {
         target.push(dx, dy, dz);
         markMoved(target);
         return true;
      }
   }

   public static boolean push(Entity source, Entity target, Vec3 delta) {
      return push(source, target, delta.x, delta.y, delta.z);
   }

   public static boolean setMotion(Entity source, Entity target, Vec3 motion) {
      if (!allowed(source, target)) {
         return false;
      } else {
         target.setDeltaMovement(motion);
         markMoved(target);
         return true;
      }
   }

   public static boolean setMotion(Entity source, Entity target, double x, double y, double z) {
      return setMotion(source, target, new Vec3(x, y, z));
   }

   public static boolean knockback(Entity source, LivingEntity target, double strength, double fromX, double fromZ) {
      if (!allowed(source, target)) {
         return false;
      } else {
         target.knockback(strength, fromX, fromZ);
         markMoved(target);
         return true;
      }
   }

   private static void markMoved(Entity target) {
      target.hurtMarked = true;
   }
}
