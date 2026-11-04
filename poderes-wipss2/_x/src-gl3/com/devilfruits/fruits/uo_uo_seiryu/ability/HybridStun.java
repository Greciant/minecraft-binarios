package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.combat.Stun;
import net.minecraft.world.entity.LivingEntity;

public final class HybridStun {
   public static void apply(LivingEntity target, int ticks) {
      Stun.apply(target, ticks);
   }

   public static void release(LivingEntity target) {
      Stun.release(target);
   }

   public static boolean stunned(LivingEntity target) {
      return Stun.stunned(target);
   }

   private HybridStun() {
   }
}
