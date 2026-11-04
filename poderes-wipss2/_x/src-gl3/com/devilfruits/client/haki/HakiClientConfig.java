package com.devilfruits.client.haki;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;

public final class HakiClientConfig {
   public static final ModConfigSpec SPEC;
   public static final EnumValue<HakiClientConfig.Quality> QUALITY;
   public static final DoubleValue SHAKE;

   private HakiClientConfig() {
   }

   static {
      Builder b = new Builder();
      QUALITY = b.comment("LOW: the black coat with a thin outline, no sheen. MEDIUM: + the moving sheen. HIGH: + embers, full outlines everywhere.")
         .defineEnum("quality", HakiClientConfig.Quality.HIGH);
      SHAKE = b.comment("Camera shake of Haki blows, clashes and Ryuo (0 turns it off).").defineInRange("shake", 1.0, 0.0, 2.0);
      SPEC = b.build();
   }

   public static enum Quality {
      LOW,
      MEDIUM,
      HIGH;
   }
}
