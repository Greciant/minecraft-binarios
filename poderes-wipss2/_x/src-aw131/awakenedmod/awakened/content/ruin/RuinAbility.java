package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.Power;
import com.awakenedmod.awakened.core.power.PowerRegistry;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import net.minecraft.server.level.ServerPlayer;

abstract class RuinAbility extends Ability {
   protected RuinAbility(String id, float staminaCost, int unlockLevel) {
      super(id, staminaCost, unlockLevel);
   }

   protected static double powered(AbilityScaling.Entry entry, ServerPlayer player) {
      return entry.perPointValue(MasteryScaling.abilityPower(player));
   }

   protected static int poweredCount(AbilityScaling.Entry entry, ServerPlayer player) {
      return (int)Math.round(powered(entry, player));
   }

   protected static boolean busy(ServerPlayer player) {
      Power power = PowerRegistry.get("ruin");
      if (power == null) {
         return false;
      } else {
         for (Ability ability : power.abilities()) {
            if (ability.isActive(player) && (!(ability instanceof RuinAbility ruin) || !ruin.yieldToPress(player))) {
               return true;
            }
         }

         return false;
      }
   }

   protected boolean yieldToPress(ServerPlayer player) {
      return false;
   }

   protected boolean drainTick(ServerPlayer player, float amount, int holdTicks) {
      PowerData data = PowerService.data(player);
      if (data.infiniteStamina()) {
         return true;
      } else {
         float cost = this.billed(amount);
         if (data.stamina() < cost) {
            return false;
         } else {
            StaminaSystem.drainSustained(data, player.serverLevel().getGameTime(), cost);
            if (holdTicks % 3 == 0) {
               PowerService.sync(player);
            }

            return true;
         }
      }
   }

   protected static void ranDry(ServerPlayer player) {
      PowerService.feedback(player, 0);
      PowerService.sync(player);
   }

   protected static String holdCostLabel(float pressCost, float drainPerTick) {
      return Math.round(pressCost) + " + " + Math.round(drainPerTick * 20.0F) + "/s";
   }
}
