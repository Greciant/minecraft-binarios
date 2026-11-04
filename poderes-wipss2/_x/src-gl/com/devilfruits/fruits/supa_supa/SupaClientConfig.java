package com.devilfruits.fruits.supa_supa;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;

public final class SupaClientConfig {
   public static final ModConfigSpec SPEC;
   public static final BooleanValue CINEMATIC_PAUSE;

   private SupaClientConfig() {
   }

   static {
      Builder b = new Builder();
      b.push("rendering");
      CINEMATIC_PAUSE = b.comment(
            "Super Slash through a ship: during the pause before it falls apart, viewers within 48 blocks get a slight zoom, a little desaturation, more bloom and ducked ambience (gentler with reduceFlashing)."
         )
         .define("cinematicPause", true);
      b.pop();
      SPEC = b.build();
   }
}
