package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.client.screen.RadialWheelScreen;
import com.awakenedmod.awakened.core.net.SetActiveSlotPayload;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class AbilityWheel {
   private static int lastSlot = -1;
   private static String lastPowerId;

   private AbilityWheel() {
   }

   public static RadialWheelScreen create(Power power) {
      ClientPowerState state = ClientPowerState.get();
      int mastery = state.masteryLevel();
      boolean infinite = state.infiniteStamina();
      float stamina = state.stamina();
      int tapSlot = tapSlot(power);
      List<RadialWheelScreen.RadialOption> options = new ArrayList<>();

      for (int slot = 0; slot < power.abilityCount(); slot++) {
         Ability ability = power.ability(slot);
         if (ability != null) {
            boolean locked = mastery < ability.unlockLevel();
            boolean affordable = infinite || stamina >= pressCost(ability);
            int cooling = state.cooldownLeft(slot);
            Component cost = locked
               ? null
               : (
                  cooling > 0
                     ? Component.translatable("awakened.hud.cooldown_short", new Object[]{(cooling + 19) / 20})
                     : Component.literal(costLabel(ability))
               );
            options.add(
               new RadialWheelScreen.RadialOption(
                  AbilityIcons.of(ability.id()),
                  power.themeColor(),
                  ability.displayName(Minecraft.getInstance().player),
                  locked,
                  locked ? Component.literal("Lv " + ability.unlockLevel()) : null,
                  slot == tapSlot,
                  slot,
                  cost,
                  affordable && cooling <= 0
               )
            );
         }
      }

      return new RadialWheelScreen(Component.translatable("awakened.ability_wheel.title"), options, AbilityWheel::fire, AwakenedKeybinds.ACTIVATE, tapSlot);
   }

   public static void fire(int slot) {
      Minecraft mc = Minecraft.getInstance();
      Power power = power();
      Ability ability = power == null ? null : power.ability(slot);
      if (ability != null && mc.player != null) {
         lastSlot = slot;
         lastPowerId = power.id();
         ClientInputHandler.send(new SetActiveSlotPayload(slot));
         ClientInputHandler.sendPress(mc, ability, slot);
      }
   }

   public static void fireLast(Power power) {
      fire(tapSlot(power));
   }

   private static int tapSlot(Power power) {
      return lastSlot >= 0 && lastSlot < power.abilityCount() && power.id().equals(lastPowerId)
         ? lastSlot
         : Math.min(Math.max(0, ClientPowerState.get().activeSlot()), power.abilityCount() - 1);
   }

   static Power power() {
      ClientPowerState state = ClientPowerState.get();
      return state.awakened() ? PowerRegistry.get(state.powerId()) : null;
   }

   private static float pressCost(Ability ability) {
      return ability.shownCost(Minecraft.getInstance().player);
   }

   private static String costLabel(Ability ability) {
      Minecraft mc = Minecraft.getInstance();
      return mc.player != null ? ability.costLabel(mc.player) : ability.costLabel();
   }
}
