package com.awakenedmod.awakened.content.ruin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class RuinArm {
   private RuinArm() {
   }

   public static RuinHand of(Entity entity) {
      return (RuinHand)entity.getData(RuinAttachments.HAND);
   }

   public static void set(ServerPlayer player, float amount) {
      apply(player, RuinHand.of(amount));
   }

   public static void off(ServerPlayer player) {
      apply(player, RuinHand.NONE);
   }

   public static void pulse(ServerPlayer player, float peak, int hold, int fade) {
      RuinHand top = RuinHand.of(peak);
      apply(player, top);
      ServerLevel level = player.serverLevel();
      int span = Math.max(1, fade);
      RuinHand[] last = new RuinHand[]{top};
      RuinFxClock.every(level, hold + span, t -> {
         if (t > hold && !player.isRemoved() && of(player).equals(last[0])) {
            RuinHand next = RuinHand.of(peak * (1.0F - (float)(t - hold) / (float)span));
            last[0] = next;
            apply(player, next);
         }
      });
   }

   private static void apply(ServerPlayer player, RuinHand next) {
      if (!of(player).equals(next)) {
         player.setData(RuinAttachments.HAND, next);
      }
   }
}
