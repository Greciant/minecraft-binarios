package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;

public final class VelocityColor {
   private static final String VAR = "velocity_color";
   private static final float UNSET = -1.0F;

   private VelocityColor() {
   }

   public static DyeColor of(PowerData data) {
      float stored = data == null ? -1.0F : data.var("velocity_color", -1.0F);
      return stored < 0.0F ? VelocityPalette.DEFAULT_DYE : DyeColor.byId(Math.round(stored));
   }

   public static boolean rolled(PowerData data) {
      return data != null && data.var("velocity_color", -1.0F) >= 0.0F;
   }

   public static DyeColor set(ServerPlayer player, DyeColor dye) {
      if (player != null && dye != null) {
         PowerService.data(player).setVar("velocity_color", (float)dye.getId());
         VelocityVisualState.sync(player);
         return dye;
      } else {
         return VelocityPalette.DEFAULT_DYE;
      }
   }

   public static DyeColor roll(ServerPlayer player) {
      DyeColor[] all = DyeColor.values();
      return set(player, all[player.getRandom().nextInt(all.length)]);
   }

   public static void rollIfUnset(ServerPlayer player, PowerData data) {
      if (!rolled(data)) {
         roll(player);
      }
   }

   public static Component recoloured(DyeColor dye) {
      return Component.translatable("awakened.velocity.recolored", new Object[]{Component.translatable("color.minecraft." + dye.getSerializedName())});
   }
}
