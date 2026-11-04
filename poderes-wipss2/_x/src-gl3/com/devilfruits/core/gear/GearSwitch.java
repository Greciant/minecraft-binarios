package com.devilfruits.core.gear;

import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.form.FormManager;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.registry.DFAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class GearSwitch {
   public static GearSwitch.Result request(ServerPlayer player, String gearName, String subName) {
      PlayerFruitData data = DFAttachments.data(player);
      DevilFruit fruit = data.fruit();
      if (!GearRegistry.hasGears(fruit)) {
         return GearSwitch.Result.NO_GEARS;
      } else {
         GearDefinition gear = GearRegistry.find(fruit, gearName);
         if (gear == null) {
            return GearSwitch.Result.UNKNOWN;
         } else {
            GearDefinition target = gear;
            if (!gear.leaf()) {
               target = subName.isEmpty() ? gear.subGears().get(0) : gear.sub(subName);
               if (target == null) {
                  return GearSwitch.Result.UNKNOWN;
               }
            }

            if (gear.available() && target.available()) {
               int needed = Math.max(gear.unlockMastery(), target.unlockMastery());
               if (data.mastery() < needed) {
                  player.displayClientMessage(Component.translatable("gear.devilfruits.locked", new Object[]{needed}).withStyle(ChatFormatting.RED), true);
                  return GearSwitch.Result.LOCKED;
               } else {
                  String form = target.form() == null ? fruit.baseForm().name() : target.form();

                  return switch (FormManager.request(player, form, FormManager.Reason.PLAYER)) {
                     case SWITCHED, WINDUP -> GearSwitch.Result.SWITCHED;
                     case ALREADY -> GearSwitch.Result.ALREADY;
                     default -> GearSwitch.Result.REFUSED;
                  };
               }
            } else {
               player.displayClientMessage(Component.translatable("gear.devilfruits.coming_soon").withStyle(ChatFormatting.GRAY), true);
               return GearSwitch.Result.UNAVAILABLE;
            }
         }
      }
   }

   public static void handle(ServerPlayer player, GearSwitchPayload payload) {
      request(player, payload.gear(), payload.subGear());
   }

   private GearSwitch() {
   }

   public static enum Result {
      SWITCHED,
      ALREADY,
      NO_GEARS,
      UNKNOWN,
      UNAVAILABLE,
      LOCKED,
      REFUSED;
   }
}
