package com.awakenedmod.awakened.core.relic;

import com.awakenedmod.awakened.content.relic.RelicItem;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;

public final class RelicDrops {
   private static final int[] LOW_WEIGHTS = new int[]{8, 5, 3, 1};

   private RelicDrops() {
   }

   private static int weight(Rarity rarity, boolean favorHigh) {
      int idx = rarity.ordinal();
      return favorHigh ? LOW_WEIGHTS[LOW_WEIGHTS.length - 1 - idx] : LOW_WEIGHTS[idx];
   }

   public static String roll(RandomSource random, Rarity floor, Rarity ceil, boolean favorHigh) {
      List<Relic> pool = new ArrayList<>();
      int total = 0;

      for (Relic relic : RelicRegistry.all()) {
         if (!relic.isSignature()) {
            int ord = relic.rarity().ordinal();
            if (ord >= floor.ordinal() && ord <= ceil.ordinal()) {
               pool.add(relic);
               total += weight(relic.rarity(), favorHigh);
            }
         }
      }

      if (pool.isEmpty()) {
         return null;
      } else {
         int pick = random.nextInt(total);

         for (Relic relicx : pool) {
            pick -= weight(relicx.rarity(), favorHigh);
            if (pick < 0) {
               return relicx.id();
            }
         }

         return pool.get(pool.size() - 1).id();
      }
   }

   @Nullable
   public static ItemEntity dropAt(ServerLevel level, Vec3 pos, RandomSource random, Rarity floor, Rarity ceil, boolean favorHigh) {
      String id = roll(random, floor, ceil, favorHigh);
      return id == null ? null : spawn(level, pos, id);
   }

   @Nullable
   public static ItemEntity dropSignature(ServerLevel level, Vec3 pos, String relicId) {
      return RelicRegistry.get(relicId) == null ? null : spawn(level, pos, relicId);
   }

   private static ItemEntity spawn(ServerLevel level, Vec3 pos, String relicId) {
      ItemEntity drop = new ItemEntity(level, pos.x, pos.y, pos.z, RelicItem.of(relicId));
      drop.setDeltaMovement(0.0, 0.2, 0.0);
      drop.setExtendedLifetime();
      level.addFreshEntity(drop);
      return drop;
   }
}
