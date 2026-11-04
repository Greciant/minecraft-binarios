package com.awakenedmod.awakened.core;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class CatalystConfig {
   private static final Builder BUILDER = new Builder();
   private static final int DEFAULT_XP_COST = 0;
   private static final int DEFAULT_COOLDOWN_SECONDS = 0;
   private static final boolean DEFAULT_BANK_PROGRESS = true;
   private static final boolean DEFAULT_ENABLED = true;
   private static final double DEFAULT_DROP_MULTIPLIER = 1.0;
   public static final BooleanValue ENABLED = BUILDER.comment("Players can use the Catalyst.").define("catalyst.enabled", true);
   public static final BooleanValue FOCUSED_ENABLED = BUILDER.comment("Players can use the Focused, Honed, Refined, Precise and Perfect Catalysts.")
      .define("catalyst.focusedEnabled", true);
   public static final DoubleValue DROP_MULTIPLIER = BUILDER.comment("Multiplier on Catalysts from chests, boss drops and powered NPCs. 0 = none.")
      .defineInRange("catalyst.dropMultiplier", 1.0, 0.0, 100.0);
   public static final IntValue XP_COST = BUILDER.comment("Experience levels the Catalyst costs. 0 = free.").defineInRange("catalyst.xpCost", 0, 0, 1000);
   public static final IntValue COOLDOWN_SECONDS = BUILDER.comment("Seconds a player waits between Catalyst uses. 0 = no wait.")
      .defineInRange("catalyst.cooldownSeconds", 0, 0, 86400);
   public static final BooleanValue BANK_PROGRESS = BUILDER.comment("Players keep each power's mastery and Trial progress when they switch powers.")
      .define("catalyst.bankProgress", true);
   public static final CatalystConfig.Tier FOCUSED = new CatalystConfig.Tier("focused", 12, 0);
   public static final CatalystConfig.Tier HONED = new CatalystConfig.Tier("honed", 8, 0);
   public static final CatalystConfig.Tier REFINED = new CatalystConfig.Tier("refined", 5, 0);
   public static final CatalystConfig.Tier PRECISE = new CatalystConfig.Tier("precise", 3, 0);
   public static final CatalystConfig.Tier PERFECT = new CatalystConfig.Tier("perfect", 1, 0);
   public static final ModConfigSpec SPEC = BUILDER.build();

   private CatalystConfig() {
   }

   public static int xpCost() {
      return orDefault(XP_COST, 0);
   }

   public static int cooldownSeconds() {
      return orDefault(COOLDOWN_SECONDS, 0);
   }

   public static boolean enabled() {
      return orDefault(ENABLED, true);
   }

   public static boolean focusedEnabled() {
      return orDefault(FOCUSED_ENABLED, true);
   }

   public static double dropMultiplier() {
      return orDefault(DROP_MULTIPLIER, 1.0);
   }

   public static boolean bankProgress() {
      return orDefault(BANK_PROGRESS, true);
   }

   public static AwakenedConfigs.Saved setBankProgress(MinecraftServer server, boolean value) {
      return AwakenedConfigs.write(BANK_PROGRESS, value, server);
   }

   private static <T> T orDefault(ConfigValue<T> value, T fallback) {
      return (T)(SPEC.isLoaded() ? value.get() : fallback);
   }

   public static final class Tier {
      private final IntValue picks;
      private final IntValue xpCost;
      private final int defaultPicks;
      private final int defaultXpCost;

      private Tier(String id, int picks, int xpCost) {
         this.defaultPicks = picks;
         this.defaultXpCost = xpCost;
         String name = Character.toUpperCase(id.charAt(0)) + id.substring(1) + " Catalyst";
         this.picks = CatalystConfig.BUILDER
            .comment("Powers a player picks for the " + name + " before it rolls one of them.")
            .defineInRange("catalyst." + id + ".picks", picks, 1, 256);
         this.xpCost = CatalystConfig.BUILDER
            .comment("Experience levels the " + name + " costs. 0 = free.")
            .defineInRange("catalyst." + id + ".xpCost", xpCost, 0, 1000);
      }

      public int picks() {
         return CatalystConfig.orDefault(this.picks, this.defaultPicks);
      }

      public int xpCost() {
         return CatalystConfig.orDefault(this.xpCost, this.defaultXpCost);
      }
   }
}
