package com.awakenedmod.awakened.core.glow;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class LimbGlow {
   private LimbGlow() {
   }

   public static GlowState of(Entity entity) {
      return (GlowState)entity.getData(AwakenedAttachments.LIMB_GLOW);
   }

   public static void set(ServerPlayer player, Limb limb, float intensity) {
      apply(player, of(player).with(limb, intensity));
   }

   public static void set(ServerPlayer player, Limb limb, float intensity, int color) {
      apply(player, of(player).withColor(color).with(limb, intensity));
   }

   public static void color(ServerPlayer player, int color) {
      apply(player, of(player).withColor(color));
   }

   public static void flash(ServerPlayer player, Limb limb) {
      apply(player, of(player).flashed(limb));
   }

   public static void clear(ServerPlayer player) {
      GlowState current = of(player);
      if (current.any()) {
         apply(player, current.with(Limb.ALL, 0.0F));
      }
   }

   private static void apply(ServerPlayer player, GlowState next) {
      if (!of(player).equals(next)) {
         player.setData(AwakenedAttachments.LIMB_GLOW, next);
      }
   }
}
