package com.awakenedmod.awakened.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class AbilityGate {
   private static final List<Predicate<LivingEntity>> SUPPRESSORS = new CopyOnWriteArrayList<>();
   private static final List<Predicate<ServerPlayer>> PRESS_LOCKS = new CopyOnWriteArrayList<>();

   private AbilityGate() {
   }

   public static void register(Predicate<ServerPlayer> suppressor) {
      SUPPRESSORS.add(caster -> {
         if (caster instanceof ServerPlayer player && suppressor.test(player)) {
            return true;
         }

         return false;
      });
   }

   public static void registerAny(Predicate<LivingEntity> suppressor) {
      SUPPRESSORS.add(suppressor);
   }

   public static void registerPressLock(Predicate<ServerPlayer> lock) {
      PRESS_LOCKS.add(lock);
   }

   public static boolean refusesPress(ServerPlayer player) {
      if (isSealed(player)) {
         return true;
      } else {
         for (Predicate<ServerPlayer> lock : PRESS_LOCKS) {
            if (lock.test(player)) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean isSealed(LivingEntity caster) {
      if (PowerSuppression.test(caster)) {
         return true;
      } else {
         for (Predicate<LivingEntity> suppressor : SUPPRESSORS) {
            if (suppressor.test(caster)) {
               return true;
            }
         }

         return false;
      }
   }
}
