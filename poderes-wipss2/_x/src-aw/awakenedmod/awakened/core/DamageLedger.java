package com.awakenedmod.awakened.core;

import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.world.entity.LivingEntity;

public final class DamageLedger {
   private DamageLedger() {
   }

   public static void record(LivingEntity victim, UUID attacker, float amount) {
      if (!(amount <= 0.0F)) {
         ((Map)victim.getData(AwakenedAttachments.DAMAGE_LEDGER)).merge(attacker, amount, Float::sum);
      }
   }

   public static Map<UUID, Float> contributions(LivingEntity victim) {
      return (Map<UUID, Float>)victim.getData(AwakenedAttachments.DAMAGE_LEDGER);
   }

   public static Set<UUID> creditedPlayers(LivingEntity victim, float fraction) {
      Map<UUID, Float> ledger = (Map<UUID, Float>)victim.getData(AwakenedAttachments.DAMAGE_LEDGER);
      if (ledger.isEmpty()) {
         return Set.of();
      } else {
         float threshold = victim.getMaxHealth() * fraction;
         Set<UUID> out = new HashSet<>();

         for (Entry<UUID, Float> e : ledger.entrySet()) {
            if (e.getValue() >= threshold) {
               out.add(e.getKey());
            }
         }

         return out;
      }
   }
}
