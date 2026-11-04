package com.devilfruits.core.rules;

import com.devilfruits.core.config.CoreConfig;
import com.devilfruits.core.data.PlayerFruitData;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.registry.DFTriggers;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

public final class Mastery {
   public static int maxEnergy(@Nullable DevilFruit fruit, int mastery) {
      return fruit == null ? 100 + 10 * (mastery - 1) : fruit.config().baseEnergy() + fruit.config().energyPerLevel() * (mastery - 1);
   }

   public static int maxEnergy(PlayerFruitData data) {
      return maxEnergy(data.fruit(), data.mastery());
   }

   public static float damageMultiplier(int mastery) {
      return (float)(1.0 + (Double)CoreConfig.DAMAGE_PER_LEVEL.get() * (double)(mastery - 1))
         * ((Double)CoreConfig.GLOBAL_DAMAGE_MULTIPLIER.get()).floatValue();
   }

   public static float rangeMultiplier(int mastery) {
      return (float)(1.0 + (Double)CoreConfig.RANGE_PER_LEVEL.get() * (double)(mastery - 1));
   }

   public static float cooldownMultiplier(int mastery) {
      return (float)Math.max(0.1, 1.0 - (Double)CoreConfig.COOLDOWN_REDUCTION_PER_LEVEL.get() * (double)(mastery - 1));
   }

   public static void addXp(ServerPlayer player, PlayerFruitData data, float rawAmount) {
      DevilFruit fruit = data.fruit();
      if (fruit != null && !(rawAmount <= 0.0F) && data.mastery() < 10) {
         float xp = data.masteryXp() + rawAmount * ((Double)CoreConfig.XP_MULTIPLIER.get()).floatValue() * (float)fruit.config().xpMultiplier();
         int level = data.mastery();

         boolean leveled;
         for (leveled = false; level < 10 && xp >= (float)CoreConfig.xpForLevel(level); leveled = true) {
            xp -= (float)CoreConfig.xpForLevel(level);
            level++;
         }

         if (level >= 10) {
            xp = 0.0F;
         }

         data.setMasteryXp(xp);
         if (leveled) {
            setLevel(player, data, level, true);
         }
      }
   }

   public static void setLevel(ServerPlayer player, PlayerFruitData data, int level, boolean announce) {
      int old = data.mastery();
      DevilFruit fruit = data.fruit();
      data.setMastery(level);
      if (level > old) {
         data.setEnergy(Math.min((float)maxEnergy(fruit, level), data.energy() + (float)(maxEnergy(fruit, level) - maxEnergy(fruit, old))));
      } else {
         data.setEnergy(Math.min((float)maxEnergy(fruit, level), data.energy()));
      }

      if (fruit != null) {
         if (announce && level > old) {
            player.displayClientMessage(fruit.message("mastery_up", level).withStyle(ChatFormatting.AQUA), true);
            ServerFx.simple(player, fruit.fx().levelUp());
         }

         if (level >= 10) {
            DFTriggers.MAX_MASTERY.get().trigger(player, fruit.id());
         }
      }
   }

   private Mastery() {
   }
}
