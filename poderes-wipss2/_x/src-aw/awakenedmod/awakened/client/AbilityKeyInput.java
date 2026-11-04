package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.Arrays;
import net.minecraft.client.Minecraft;

public final class AbilityKeyInput {
   private static final int SLOTS = AwakenedKeybinds.SLOT_KEYS.length;
   private static final int TAP_TICKS = 2;
   private static final boolean[] held = new boolean[SLOTS];
   private static final boolean[] pressed = new boolean[SLOTS];
   private static final boolean[] quickPrev = new boolean[SLOTS];
   private static boolean activatePrev;
   private static final int[] tapTicks = new int[SLOTS];
   private static int lastDriven = -1;

   private AbilityKeyInput() {
   }

   static void tick(Minecraft mc) {
      if (mc.player == null) {
         reset();
      } else {
         ClientPowerState state = ClientPowerState.get();
         Power power = PowerRegistry.get(state.powerId());
         boolean usable = mc.screen == null;
         boolean activateNow = usable && AwakenedKeybinds.ACTIVATE.isDown();
         boolean quickFires = usable && (Boolean)ClientConfig.QUICK_KEY_MODE.get();
         int active = state.activeSlot();

         for (int slot = 0; slot < SLOTS; slot++) {
            boolean quickNow = quickFires && (AwakenedKeybinds.SLOT_KEYS[slot].isDown() || tapTicks[slot] > 0);
            boolean activateDrives = activateNow && slot == active;
            pressed[slot] = quickNow && !quickPrev[slot] || activateDrives && !activatePrev;
            held[slot] = quickNow || activateDrives;
            quickPrev[slot] = quickNow;
            if (tapTicks[slot] > 0) {
               tapTicks[slot]--;
            }
         }

         activatePrev = activateNow;
         if (lastDriven >= 0 && (!held[lastDriven] || !isClientDriven(power, lastDriven))) {
            lastDriven = -1;
         }

         for (int slotx = 0; slotx < SLOTS; slotx++) {
            if (pressed[slotx] && isClientDriven(power, slotx)) {
               lastDriven = slotx;
            }
         }
      }
   }

   static void markTap(int slot) {
      if (slot >= 0 && slot < SLOTS) {
         tapTicks[slot] = 2;
      }
   }

   static void reset() {
      Arrays.fill(held, false);
      Arrays.fill(pressed, false);
      Arrays.fill(quickPrev, false);
      Arrays.fill(tapTicks, 0);
      activatePrev = false;
      lastDriven = -1;
   }

   public static boolean heldFor(int slot) {
      return slot >= 0 && slot < SLOTS && held[slot];
   }

   public static boolean pressedFor(int slot) {
      return slot >= 0 && slot < SLOTS && pressed[slot];
   }

   public static int drivingSlot() {
      return lastDriven >= 0 ? lastDriven : ClientPowerState.get().activeSlot();
   }

   public static Ability driving() {
      ClientPowerState state = ClientPowerState.get();
      if (!state.awakened()) {
         return null;
      } else {
         Power power = PowerRegistry.get(state.powerId());
         return power != null ? power.ability(drivingSlot()) : null;
      }
   }

   public static boolean ownsClick(Minecraft mc) {
      if (mc.screen != null) {
         return false;
      } else if (AwakenedKeybinds.ACTIVATE.isDown()) {
         return true;
      } else {
         Power power = PowerRegistry.get(ClientPowerState.get().powerId());
         if (power == null) {
            return false;
         } else {
            int slots = Math.min(SLOTS, power.abilityCount());

            for (int slot = 0; slot < slots; slot++) {
               Ability ability = power.ability(slot);
               if (held[slot] && ability != null && (ability.usesGripControls() || ability.clientDriven())) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private static boolean isClientDriven(Power power, int slot) {
      if (power != null && slot >= 0 && slot < power.abilityCount()) {
         Ability ability = power.ability(slot);
         return ability != null && ability.clientDriven();
      } else {
         return false;
      }
   }
}
