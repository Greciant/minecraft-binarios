package com.impactframe;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public final class ImpactCommonConfig {
   private static final Builder BUILDER = new Builder();
   private static final BooleanValue ALLOW_ON_MULTIPLAYER = BUILDER.comment(
         new String[]{
            "Let players on a dedicated server / LAN world with several players freeze time for everyone.",
            "A solo single-player world is always allowed. Off by default so players cannot grief a server."
         }
      )
      .define("allowOnMultiplayer", false);
   private static final IntValue MAX_FREEZE_TICKS = BUILDER.comment("Hard cap on how long one request may freeze time, in ticks (20 ticks = 1 second).")
      .defineInRange("maxFreezeTicks", 10, 1, 40);
   private static final IntValue PLAYER_COOLDOWN_TICKS = BUILDER.comment("Minimum ticks between two freeze requests from the same player.")
      .defineInRange("playerCooldownTicks", 20, 0, 600);
   public static final ModConfigSpec SPEC = BUILDER.build();

   private ImpactCommonConfig() {
   }

   public static boolean allowOnMultiplayer() {
      return (Boolean)ALLOW_ON_MULTIPLAYER.get();
   }

   public static int maxFreezeTicks() {
      return (Integer)MAX_FREEZE_TICKS.get();
   }

   public static int playerCooldownTicks() {
      return (Integer)PLAYER_COOLDOWN_TICKS.get();
   }

   static {
      BUILDER.comment("Server-side features that clients with the mod can opt into.").push("timeFreeze");
      BUILDER.pop();
   }
}
