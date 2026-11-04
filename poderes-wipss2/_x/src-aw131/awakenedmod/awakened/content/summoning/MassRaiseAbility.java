package com.awakenedmod.awakened.content.summoning;

import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public final class MassRaiseAbility extends Ability {
   private static final double AIM_RANGE = 8.0;
   private static final double RING_SPACING = 1.6;
   private static final int DEFAULT_BATCH = 5;

   public MassRaiseAbility() {
      super("mass_raise", 15.0F, 6);
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      SoulData souls = SummonAttachments.souls(player);
      int remaining = SummonSouls.maxMinions(player) - MinionSupport.count(player);
      int total = 0;

      for (Entry<String, Integer> e : plannedWave(souls).entrySet()) {
         int n = Math.min(Math.min(e.getValue(), souls.count(e.getKey())), remaining);
         if (n > 0) {
            total += SummonSouls.summonCost(e.getKey()) * n;
            remaining -= n;
         }
      }

      return (float)total;
   }

   private static Map<String, Integer> plannedWave(SoulData souls) {
      Map<String, Integer> loadout = raisableOnly(souls.loadout());
      if (!loadout.isEmpty()) {
         return loadout;
      } else {
         String active = souls.active();
         return !active.isEmpty() && souls.count(active) > 0 && !SummonSouls.isDormant(active) ? Map.of(active, Math.min(5, souls.count(active))) : Map.of();
      }
   }

   private static Map<String, Integer> raisableOnly(Map<String, Integer> plan) {
      Map<String, Integer> raisable = new LinkedHashMap<>(plan);
      raisable.keySet().removeIf(SummonSouls::isDormant);
      return raisable;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      SoulData souls = SummonAttachments.souls(player);
      Map<String, Integer> plan = plannedWave(souls);
      if (plan.isEmpty()) {
         return this.fail(player, "awakened.summon.no_souls", new Object[0]);
      } else {
         int headroom = SummonSouls.maxMinions(player) - MinionSupport.count(player);
         if (headroom <= 0) {
            return this.fail(player, "awakened.summon.at_capacity", new Object[]{SummonSouls.maxMinions(player)});
         } else {
            ServerLevel level = player.serverLevel();
            Vec3 center = MinionSupport.groundSpot(player, 8.0);
            RandomSource rng = player.getRandom();
            double spin = rng.nextDouble() * Math.PI * 2.0;
            int raised = 0;
            Set<String> raisedTypes = new LinkedHashSet<>();
            int spotIndex = 0;

            for (Entry<String, Integer> entry : plan.entrySet()) {
               String type = entry.getKey();
               int want = entry.getValue();

               for (int i = 0; i < want && raised < headroom && souls.count(type) > 0; i++) {
                  Vec3 spot = this.ringSpot(center, spotIndex++, spin, rng);
                  if (!souls.spendOne(type)) {
                     break;
                  }

                  Mob mob = MinionSupport.spawnRising(player, type, spot);
                  if (mob == null) {
                     souls.add(type, 1, 100);
                  } else {
                     SummonFx.gather(level, mob.position());
                     raised++;
                     raisedTypes.add(type);
                  }
               }
            }

            if (raised == 0) {
               return this.fail(player, "awakened.summon.no_ground", new Object[0]);
            } else {
               raisedTypes.forEach(souls::recordUse);
               player.setData(SummonAttachments.SOULS, souls);
               player.displayClientMessage(Component.translatable("awakened.souls.raised", new Object[]{raised}), true);
               return true;
            }
         }
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "summon";
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boosts("minion_health").boosts("minion_damage").build();
   }

   private Vec3 ringSpot(Vec3 center, int index, double spin, RandomSource rng) {
      double jitterX = (rng.nextDouble() - 0.5) * 1.6 * 0.6;
      double jitterZ = (rng.nextDouble() - 0.5) * 1.6 * 0.6;
      if (index == 0) {
         return center.add(jitterX, 0.0, jitterZ);
      } else {
         int ring = (int)Math.ceil(Math.sqrt((double)(index + 1)));
         int perRing = Math.max(6, ring * 6);
         double angle = spin + (Math.PI * 2) * (double)(index % perRing) / (double)perRing + (rng.nextDouble() - 0.5) * 0.4;
         double radius = 1.6 * (double)ring * (0.85 + rng.nextDouble() * 0.3);
         return center.add(Math.cos(angle) * radius + jitterX, 0.0, Math.sin(angle) * radius + jitterZ);
      }
   }
}
