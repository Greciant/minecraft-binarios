package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.RadialWheelScreen;
import com.awakenedmod.awakened.core.power.Power;
import net.minecraft.client.Minecraft;

public final class AbilityWheelInput {
   private static final int OPEN_TICKS = 2;
   private static int heldTicks;
   private static boolean wheelOpen;
   private static boolean awaitingRelease;
   private static boolean wasDown;

   private AbilityWheelInput() {
   }

   static void handle(Minecraft mc, Power power) {
      boolean clicked = false;

      while (AwakenedKeybinds.ACTIVATE.consumeClick()) {
         clicked = true;
      }

      boolean down = RadialWheelScreen.keyHeld(AwakenedKeybinds.ACTIVATE);
      boolean fresh = down && !wasDown || clicked;
      wasDown = down;
      if (fresh) {
         awaitingRelease = false;
      }

      if (wheelOpen) {
         if (mc.screen instanceof RadialWheelScreen) {
            return;
         }

         wheelOpen = false;
         heldTicks = 0;
         awaitingRelease = down && !fresh;
      }

      if (awaitingRelease) {
         awaitingRelease = down;
         heldTicks = 0;
      } else if (down) {
         if (mc.screen != null) {
            heldTicks = 0;
         } else {
            if (++heldTicks >= 2) {
               mc.setScreen(AbilityWheel.create(power));
               wheelOpen = true;
            }
         }
      } else {
         if ((heldTicks > 0 || clicked) && mc.screen == null) {
            AbilityWheel.fireLast(power);
         }

         heldTicks = 0;
      }
   }

   static void reset() {
      heldTicks = 0;
      wheelOpen = false;
      awaitingRelease = false;
      wasDown = false;
   }
}
