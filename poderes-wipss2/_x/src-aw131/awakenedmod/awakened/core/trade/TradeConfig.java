package com.awakenedmod.awakened.core.trade;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;

public final class TradeConfig {
   private static final Builder BUILDER = new Builder();
   private static final boolean DEFAULT_ALLOWED = true;
   public static final BooleanValue ALLOW_POWER_TRADING = BUILDER.comment("Players can swap powers by sneaking and right-clicking each other.")
      .define("allow_power_trading", true);
   public static final ModConfigSpec SPEC = BUILDER.build();

   private TradeConfig() {
   }

   public static boolean allowedByDefault() {
      return SPEC.isLoaded() ? (Boolean)ALLOW_POWER_TRADING.get() : true;
   }
}
