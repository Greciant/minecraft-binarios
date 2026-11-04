package com.awakenedmod.awakened.core;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class TempFlight {
   private static final int LAND_SLOW_FALL_TICKS = 100;

   private TempFlight() {
   }

   public static void grant(ServerPlayer player) {
      player.getAbilities().mayfly = true;
      player.getAbilities().flying = true;
      player.onUpdateAbilities();
   }

   public static void revoke(ServerPlayer player) {
      revoke(player, true);
   }

   public static void revoke(ServerPlayer player, boolean slowFall) {
      if (!player.isCreative() && !player.isSpectator()) {
         player.getAbilities().mayfly = false;
         player.getAbilities().flying = false;
         player.onUpdateAbilities();
         if (slowFall) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0));
         }
      }

      player.resetFallDistance();
   }
}
