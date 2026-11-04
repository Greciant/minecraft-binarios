package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.AbilityFeedback;
import net.minecraft.server.level.ServerPlayer;

public final class CommandRules {
   private static final int DEFAULT_SECONDS = 10;

   private CommandRules() {
   }

   public static void decree(ServerPlayer player, int slot, Rule asked) {
      if (player != null) {
         if (!CommandPower.owns(player)) {
            AbilityFeedback.fail(player, "awakened.feedback.command_bad_rule");
         } else if (slot >= 0 && slot < 5) {
            PowerData data = PowerService.data(player);
            int mastery = data.masteryLevel();
            int slotUnlock = CommandPower.slotUnlockLevel(slot);
            if (mastery < slotUnlock) {
               AbilityFeedback.fail(player, "awakened.feedback.locked", slotUnlock);
            } else if (asked == null || asked.isEmpty()) {
               RuleEngine.switchOff(player, slot);
               CommandData.write(player, CommandData.of(player).withSlot(slot, RuleSlot.EMPTY).withName(slot, ""));
            } else if (!asked.resolves()) {
               AbilityFeedback.fail(player, "awakened.feedback.command_bad_rule");
            } else {
               Rule rule = asked.withSeconds(duration(asked.seconds(), mastery)).written();
               RuleEngine.Refusal refusal = RuleEngine.checkWritable(rule, player);
               if (refusal != null) {
                  AbilityFeedback.fail(player, refusal.key(), refusal.args());
               } else {
                  RuleEngine.switchOff(player, slot);
                  CommandTrace.say(player, CommandTrace.slot(slot) + ": decreed \"" + rule.describe().getString() + "\"");
                  long now = CommandSupport.level(player).getGameTime();
                  CommandData.writeSlot(player, slot, RuleSlot.written(rule, now));
               }
            }
         } else {
            AbilityFeedback.fail(player, "awakened.feedback.command_bad_rule");
         }
      }
   }

   public static void rename(ServerPlayer player, int slot, String name) {
      if (player != null
         && CommandPower.owns(player)
         && slot >= 0
         && slot < 5
         && PowerService.data(player).masteryLevel() >= CommandPower.slotUnlockLevel(slot)) {
         CommandData.write(player, CommandData.of(player).withName(slot, name));
      } else {
         AbilityFeedback.fail(player, "awakened.feedback.command_bad_rule");
      }
   }

   private static int duration(int seconds, int mastery) {
      if (seconds == -1) {
         return -1;
      } else {
         return seconds > 0 ? seconds : Math.min(10, CommandCost.durationCap(mastery));
      }
   }
}
