package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class VelocityGearHint {
   private static final int MAX_HINTS = 2;
   private static final int RUN_TICKS = 30;
   private static int runTicks;
   private static boolean shownThisRun;

   private VelocityGearHint() {
   }

   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player == null || !"velocity".equals(ClientPowerState.get().powerId())) {
         runTicks = 0;
         shownThisRun = false;
      } else if (!VelocityVisualState.of(player).running()) {
         runTicks = 0;
         shownThisRun = false;
      } else {
         runTicks++;
         if (runTicks >= 30 && !shownThisRun) {
            shownThisRun = true;
            show(player);
         }
      }
   }

   public static void learned() {
      if ((Integer)ClientConfig.VELOCITY_GEAR_HINTS_SHOWN.get() < 2) {
         ClientConfig.VELOCITY_GEAR_HINTS_SHOWN.set(2);
      }
   }

   private static void show(LocalPlayer player) {
      if ((Boolean)ClientConfig.AWAKEN_CONTROLS_CHAT.get()) {
         int shown = (Integer)ClientConfig.VELOCITY_GEAR_HINTS_SHOWN.get();
         if (shown < 2) {
            ClientConfig.VELOCITY_GEAR_HINTS_SHOWN.set(shown + 1);
            player.displayClientMessage(
               Component.translatable("awakened.velocity.gear_hint", new Object[]{AwakenedKeybinds.SECONDARY_ABILITY.getTranslatedKeyMessage()})
                  .withStyle(ChatFormatting.RED),
               false
            );
         }
      }
   }
}
