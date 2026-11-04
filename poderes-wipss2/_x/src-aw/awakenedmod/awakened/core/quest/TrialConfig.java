package com.awakenedmod.awakened.core.quest;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;

public final class TrialConfig {
   private static final Builder BUILDER = new Builder();
   private static final boolean DEFAULT_ENABLED = true;
   public static final BooleanValue ENABLED = BUILDER.comment("Players can do Trials.").define("trials.enabled", true);
   public static final ModConfigSpec SPEC = BUILDER.build();

   private TrialConfig() {
   }

   public static boolean enabled() {
      return SPEC.isLoaded() ? (Boolean)ENABLED.get() : true;
   }
}
