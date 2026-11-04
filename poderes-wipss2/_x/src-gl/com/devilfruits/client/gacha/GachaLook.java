package com.devilfruits.client.gacha;

import com.devilfruits.client.vfx.particle.ParticleStyle;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.devilfruits.core.fruit.DevilFruit;
import com.devilfruits.core.gacha.GachaTier;
import net.minecraft.util.Mth;

public final class GachaLook {
   public static final int GOLD = 16763210;
   public static final int GOLD_LIGHT = 16770976;
   public static final int GOLD_WHITE = 16774876;
   public static final ParticleStyle MOTE = ParticleStyle.colour(VfxSprites.GLOW)
      .life(28, 46)
      .size(0.12F, 0.05F, 1.0F)
      .fade(0.15F, 0.55F)
      .additive(1.0F)
      .emissive(2.2F)
      .soft(0.0F)
      .drag(0.08F)
      .gravity(-0.0015F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle RAY = ParticleStyle.colour(VfxSprites.SPARK)
      .life(9, 15)
      .size(0.34F, 0.2F, 1.0F)
      .fade(0.1F, 0.5F)
      .additive(1.0F)
      .emissive(2.4F)
      .soft(0.0F)
      .drag(0.02F)
      .stretch(1.3F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle GLINT = ParticleStyle.colour(VfxSprites.SPARK)
      .life(12, 22)
      .size(0.18F, 0.06F, 1.0F)
      .fade(0.0F, 0.4F)
      .additive(1.0F)
      .emissive(3.0F)
      .soft(0.0F)
      .drag(0.05F)
      .gravity(0.012F)
      .stretch(0.5F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle HALO = ParticleStyle.colour(VfxSprites.GLOW)
      .life(3, 3)
      .size(1.0F, 1.0F, 1.0F)
      .fade(0.25F, 0.4F)
      .additive(1.0F)
      .emissive(1.6F)
      .soft(0.4F)
      .drag(0.0F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle RING = ParticleStyle.colour(VfxSprites.RING)
      .life(14, 16)
      .size(0.6F, 5.0F, 0.5F)
      .fade(0.0F, 0.65F)
      .additive(1.0F)
      .emissive(1.4F)
      .soft(0.0F)
      .flat()
      .spin(0.0F)
      .build();

   public static float tierPower(GachaTier tier) {
      return switch (tier) {
         case COMMON -> 0.0F;
         case UNCOMMON -> 1.0F;
         case RARE -> 2.0F;
         case LEGENDARY -> 3.0F;
      };
   }

   public static int fruitColour(DevilFruit fruit) {
      return fruit == null ? 16763210 : fruit.theme().masteryColor() & 16777215;
   }

   public static int mix(int a, int b, float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      int r = (int)Mth.lerp(t, (float)(a >> 16 & 0xFF), (float)(b >> 16 & 0xFF));
      int g = (int)Mth.lerp(t, (float)(a >> 8 & 0xFF), (float)(b >> 8 & 0xFF));
      int bl = (int)Mth.lerp(t, (float)(a & 0xFF), (float)(b & 0xFF));
      return r << 16 | g << 8 | bl;
   }

   private GachaLook() {
   }
}
