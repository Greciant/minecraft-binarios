package com.devilfruits.fruits.uo_uo_seiryu;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;

public final class SeiryuClientConfig {
   public static final ModConfigSpec SPEC;
   public static final BooleanValue HUMAN_OVERLAY;
   public static final BooleanValue HUMAN_EYE_GLOW;
   public static final EnumValue<SeiryuClientConfig.Detail> DRAGON_DETAIL;
   public static final BooleanValue FORM_SPARKS;
   public static final BooleanValue HYBRID_DIRECT_KEYS;
   public static final BooleanValue HIT_COUNTER;

   private SeiryuClientConfig() {
   }

   static {
      Builder b = new Builder();
      b.comment("Rendering of the Uo Uo no Mi, Model: Seiryu.").push("rendering");
      HUMAN_OVERLAY = b.comment("Human form: draw the horn bumps, rope belt and spiked wristbands over the player skin.").define("humanOverlay", true);
      HUMAN_EYE_GLOW = b.comment("Human form: faint glow in the eyes.").define("humanEyeGlow", true);
      DRAGON_DETAIL = b.comment("Azure Dragon body detail: AUTO follows vfxQuality (LOW 32 / MEDIUM 48 / HIGH 64 segments per 60 blocks of body).")
         .defineEnum("dragonDetail", SeiryuClientConfig.Detail.AUTO);
      FORM_SPARKS = b.comment("Blue / gold sparks drifting around the Hybrid and the Dragon.").define("formSparks", true);
      HIT_COUNTER = b.comment("Hybrid techniques: a \"HIT n\" counter with the damage dealt at the left of the screen while blows land.")
         .define("hitCounter", false);
      b.pop();
      b.comment("Controls of the Seiryu forms.").push("controls");
      HYBRID_DIRECT_KEYS = b.comment(
            new String[]{
               "Hybrid form: cast its eight techniques straight from the direct slot keys (default C X Z B T R E F, rebindable in",
               "Controls) instead of selecting them on the bar. Those keys then do nothing else in the Hybrid form (E inventory, T chat, F swap hands ...)."
            }
         )
         .define("hybridDirectKeys", false);
      b.pop();
      SPEC = b.build();
   }

   public static enum Detail {
      AUTO,
      LOW,
      MEDIUM,
      HIGH;
   }
}
