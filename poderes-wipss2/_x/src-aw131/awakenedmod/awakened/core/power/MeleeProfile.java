package com.awakenedmod.awakened.core.power;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public interface MeleeProfile {
   MeleeProfile.ClientAim clientAim();

   double reach(ServerPlayer var1);

   double coneHalfDegrees(ServerPlayer var1);

   int cadenceTicks(ServerPlayer var1);

   float damage(ServerPlayer var1, LivingEntity var2);

   default boolean accepts(ServerPlayer player, LivingEntity target) {
      return true;
   }

   default boolean requiresEmptyHand() {
      return true;
   }

   default double knockback(ServerPlayer player, LivingEntity target) {
      return 0.0;
   }

   default double separation(ServerPlayer player, LivingEntity target) {
      return 0.0;
   }

   default int reopenTicks(ServerPlayer player) {
      return 0;
   }

   void onHit(ServerPlayer var1, LivingEntity var2, boolean var3);

   default void onMiss(ServerPlayer player) {
   }

   public static record ClientAim(double reach, double coneHalfDegrees, int cadenceTicks) {
   }
}
