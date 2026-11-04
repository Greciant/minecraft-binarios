package com.awakenedmod.awakened.core.rules;

import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.MasteryData;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.progression.MasteryFormula;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;

final class DeathRules {
   private DeathRules() {
   }

   static void register(IEventBus gameBus) {
      gameBus.addListener(EventPriority.LOWEST, DeathRules::onDeath);
      gameBus.addListener(EventPriority.HIGHEST, DeathRules::onRespawn);
   }

   private static void onDeath(LivingDeathEvent event) {
      if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player) {
         PowerData data = PowerService.data(player);
         if (data.hasPower()) {
            double loss = ServerRules.masteryLossPercent() / 100.0;
            if (loss > 0.0) {
               MasteryData mastery = data.currentMastery();
               double total = MasteryFormula.totalPoints(mastery.level(), mastery.progress()) * (1.0 - loss);
               int level = MasteryFormula.levelAt(total);
               mastery.set(level, total - MasteryFormula.totalPoints(level, 0.0));
            }
         }
      }
   }

   private static void onRespawn(PlayerRespawnEvent event) {
      if (!event.isEndConquered() && ServerRules.losePowerOnDeath() && event.getEntity() instanceof ServerPlayer player && PowerService.data(player).hasPower()
         )
       {
         PowerService.clearPower(player);
         QuestSystem.resetForPowerChange(player);
         player.sendSystemMessage(Component.translatable("awakened.rules.power_lost").withStyle(ChatFormatting.GRAY));
      }
   }
}
