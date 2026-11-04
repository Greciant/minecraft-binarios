package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.pyrokinetic.PyroHeatState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

public final class PyroHeatHint {
   private static final String POWER_ID = "pyrokinetic";
   private static final float OVERDRIVE_SLACK = 0.005F;

   private PyroHeatHint() {
   }

   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null) {
         ClientPowerState state = ClientPowerState.get();
         if ("pyrokinetic".equals(state.powerId())) {
            float heat = state.resourceFraction();
            if (heat > PyroHeatState.cap(state.masteryLevel()) + 0.005F) {
               show(player, ClientConfig.PYRO_OVERDRIVE_HINT_SHOWN, "awakened.pyrokinetic.hint.overdrive");
            }

            if (heat >= 0.7F) {
               show(player, ClientConfig.PYRO_BLUE_HINT_SHOWN, "awakened.pyrokinetic.hint.blue");
            }
         }
      }
   }

   private static void show(LocalPlayer player, BooleanValue shown, String key) {
      if (!(Boolean)shown.get()) {
         shown.set(true);
         ClientConfig.save();
         player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD), false);
      }
   }
}
