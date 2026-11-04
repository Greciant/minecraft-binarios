package com.devilfruits.core.gacha;

import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fruit.FruitRegistry;
import com.devilfruits.core.rules.WorldFruitData;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.ToIntFunction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

public final class GachaRoll {
   public static <T> Optional<GachaRoll.Candidate<T>> roll(List<GachaRoll.Candidate<T>> candidates, ToIntFunction<GachaTier> tierWeight, RandomSource random) {
      Map<GachaTier, List<GachaRoll.Candidate<T>>> byTier = new EnumMap<>(GachaTier.class);

      for (GachaRoll.Candidate<T> candidate : candidates) {
         if (candidate.weight() > 0) {
            byTier.computeIfAbsent(candidate.tier(), t -> new ArrayList<>()).add(candidate);
         }
      }

      int total = 0;

      for (GachaTier tier : byTier.keySet()) {
         total += Math.max(0, tierWeight.applyAsInt(tier));
      }

      if (total <= 0) {
         return Optional.empty();
      } else {
         int pick = random.nextInt(total);

         for (Entry<GachaTier, List<GachaRoll.Candidate<T>>> entry : byTier.entrySet()) {
            pick -= Math.max(0, tierWeight.applyAsInt(entry.getKey()));
            if (pick < 0) {
               return Optional.of(within(entry.getValue(), random));
            }
         }

         throw new IllegalStateException("tier roll ran past its total");
      }
   }

   public static <T> boolean rollable(List<GachaRoll.Candidate<T>> candidates, ToIntFunction<GachaTier> tierWeight) {
      for (GachaRoll.Candidate<T> candidate : candidates) {
         if (candidate.weight() > 0 && tierWeight.applyAsInt(candidate.tier()) > 0) {
            return true;
         }
      }

      return false;
   }

   private static <T> GachaRoll.Candidate<T> within(List<GachaRoll.Candidate<T>> tier, RandomSource random) {
      int total = 0;

      for (GachaRoll.Candidate<T> candidate : tier) {
         total += candidate.weight();
      }

      int pick = random.nextInt(total);

      for (GachaRoll.Candidate<T> candidate : tier) {
         pick -= candidate.weight();
         if (pick < 0) {
            return candidate;
         }
      }

      throw new IllegalStateException("fruit roll ran past its total");
   }

   public static List<GachaRoll.Candidate<DevilFruit>> candidates(MinecraftServer server) {
      List<GachaRoll.Candidate<DevilFruit>> out = new ArrayList<>();
      WorldFruitData world = CoreConfig.ONE_PER_WORLD.get() ? WorldFruitData.get(server) : null;

      for (DevilFruit fruit : FruitRegistry.playable()) {
         if (fruit.hasItem()
            && fruit.config().enabled()
            && fruit.config().lootWeight() > 0
            && (world == null || world.state(fruit) == WorldFruitData.State.FREE)) {
            out.add(new GachaRoll.Candidate<>(fruit, fruit.config().gachaTier(), fruit.config().lootWeight()));
         }
      }

      return out;
   }

   public static Optional<GachaRoll.Candidate<DevilFruit>> roll(MinecraftServer server, RandomSource random) {
      return roll(candidates(server), CoreConfig::gachaTierWeight, random);
   }

   private GachaRoll() {
   }

   public static record Candidate<T>(T value, GachaTier tier, int weight) {
   }
}
