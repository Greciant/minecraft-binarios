package com.awakenedmod.awakened.content.graviton;

import com.awakenedmod.awakened.core.Fx;
import net.minecraft.core.particles.DustParticleOptions;

public final class GravitonPalette {
   public static final int WARM = 16747069;
   public static final int PALE = 16769732;
   public static final int DARK = 1971474;
   public static final int WHITE_HOT = 16774888;
   public static final int EMBER = 12731932;
   public static final DustParticleOptions WARM_DUST = Fx.dust(16747069, 1.0F);
   public static final DustParticleOptions PALE_DUST = Fx.dust(16769732, 1.0F);
   public static final DustParticleOptions DARK_DUST = Fx.dust(1971474, 1.0F);
   public static final DustParticleOptions WHITE_HOT_DUST = Fx.dust(16774888, 1.0F);
   public static final DustParticleOptions EMBER_DUST = Fx.dust(12731932, 1.0F);

   private GravitonPalette() {
   }
}
