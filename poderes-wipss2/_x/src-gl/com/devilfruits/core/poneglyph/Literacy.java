package com.devilfruits.core.poneglyph;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

public final class Literacy {
   public static final int MAX_LEVEL = 5;

   public static PoneglyphPlayerData.PoneglyphLiteracy get(Player player) {
      return (PoneglyphPlayerData.PoneglyphLiteracy)player.getData(Poneglyphs.LITERACY);
   }

   public static int level(Player player) {
      return get(player).level();
   }

   public static int xpFor(int level) {
      double total = 0.0;

      for (int n = 0; n < level; n++) {
         total += (double)((Integer)PoneglyphConfig.XP_BASE.get()).intValue() * Math.pow((Double)PoneglyphConfig.XP_GROWTH.get(), (double)n);
      }

      return (int)Math.round(total);
   }

   public static int levelFor(int xp) {
      int level = 0;

      while (level < 5 && xp >= xpFor(level + 1)) {
         level++;
      }

      return level;
   }

   public static void addXp(ServerPlayer player, int amount) {
      if (amount > 0) {
         PoneglyphPlayerData.PoneglyphLiteracy old = get(player);
         int xp = old.xp() + amount;
         set(player, Math.max(old.level(), levelFor(xp)), xp);
      }
   }

   public static void set(ServerPlayer player, int level, int xp) {
      PoneglyphPlayerData.PoneglyphLiteracy old = get(player);
      level = Math.max(0, Math.min(5, level));
      player.setData(Poneglyphs.LITERACY, new PoneglyphPlayerData.PoneglyphLiteracy(level, Math.max(xp, xpFor(level))));
      if (level > old.level()) {
         player.displayClientMessage(
            Component.translatable(
                  "poneglyph.devilfruits.message.literacy_up", new Object[]{level, Component.translatable("poneglyph.devilfruits.literacy.perk." + level)}
               )
               .withStyle(ChatFormatting.AQUA),
            true
         );
         player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
         if (level >= 5) {
            Poneglyphs.LITERACY_MAX.get().trigger(player);
         }
      }
   }

   public static int decoyReduction(int level) {
      return level >= 1 ? (Integer)PoneglyphConfig.L1_FEWER_DECOYS.get() : 0;
   }

   public static boolean hint(int level) {
      return level >= 2;
   }

   public static double lockoutFactor(int level) {
      return level >= 3 ? (Double)PoneglyphConfig.L3_LOCKOUT_FACTOR.get() : 1.0;
   }

   public static double rubFactor(int level) {
      return level >= 3 ? (Double)PoneglyphConfig.L3_RUB_FACTOR.get() : 1.0;
   }

   public static boolean forgive(int level) {
      return level >= 4;
   }

   public static boolean reveal(int level) {
      return level >= 5;
   }

   private Literacy() {
   }
}
