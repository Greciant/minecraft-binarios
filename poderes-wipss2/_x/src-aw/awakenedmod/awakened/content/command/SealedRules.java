package com.awakenedmod.awakened.content.command;

import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

final class SealedRules {
   private static final Map<UUID, Map<Integer, RuleSlot>> PAUSED = new ConcurrentHashMap<>();

   private SealedRules() {
   }

   static void pause(ServerPlayer player) {
      CommandData rules = CommandData.of(player);
      Map<Integer, RuleSlot> running = new LinkedHashMap<>();

      for (int i = 0; i < 5; i++) {
         RuleSlot slot = rules.slot(i);
         if (slot.on()) {
            running.put(i, slot);
         }
      }

      RuleEngine.clearCaster(player);
      if (!running.isEmpty()) {
         PAUSED.put(player.getUUID(), running);
      }
   }

   static void resume(ServerPlayer player, PowerData data) {
      Map<Integer, RuleSlot> running = PAUSED.remove(player.getUUID());
      if (running != null && player.isAlive()) {
         boolean any = false;

         for (Entry<Integer, RuleSlot> entry : running.entrySet()) {
            any |= RuleEngine.resume(player, data, entry.getKey(), entry.getValue());
         }

         if (any) {
            if (!data.infiniteStamina()) {
               float ceiling = Math.max(0.0F, StaminaSystem.max(player) - CommandStamina.activeSum(player, CommandData.of(player)));
               if (data.stamina() > ceiling) {
                  data.setStamina(ceiling);
               }
            }

            CommandStamina.hold(player);
         }
      }
   }

   static void forget(UUID casterId) {
      PAUSED.remove(casterId);
   }

   static void forgetAll() {
      PAUSED.clear();
   }
}
