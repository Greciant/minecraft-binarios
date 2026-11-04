package com.devilfruits.fruits.horo_horo.client.vfx;

import com.devilfruits.client.vfx.particle.ParticleStyle;
import com.devilfruits.client.vfx.sprite.VfxSprites;

public final class HoroLook {
   public static final int WHITE = 16775163;
   public static final int PINK = 16758741;
   public static final int HOT_PINK = 16740264;
   public static final int LAVENDER = 13219583;
   public static final int GLOOM = 5987240;
   public static final int SHUDDER = 10132184;
   public static final ParticleStyle PUFF = ParticleStyle.colour(VfxSprites.HORO_PUFF)
      .life(12, 18)
      .size(0.35F, 0.9F, 0.6F)
      .fade(0.05F, 0.35F)
      .additive(0.1F)
      .emissive(0.55F)
      .soft(0.15F)
      .drag(0.14F)
      .gravity(-0.003F)
      .spin(0.0F)
      .stepped()
      .build();
   public static final ParticleStyle PUFF_BIG = ParticleStyle.colour(VfxSprites.HORO_PUFF)
      .life(14, 22)
      .size(0.7F, 1.9F, 0.45F)
      .fade(0.03F, 0.35F)
      .additive(0.15F)
      .emissive(0.7F)
      .soft(0.25F)
      .drag(0.2F)
      .gravity(-0.004F)
      .spin(0.0F)
      .stepped()
      .build();
   public static final ParticleStyle[] SPARKLES = new ParticleStyle[VfxSprites.HORO_SPARKLES.length];
   public static final ParticleStyle MIST = ParticleStyle.colour(VfxSprites.HORO_PUFF)
      .life(10, 16)
      .size(0.14F, 0.34F, 0.6F)
      .fade(0.1F, 0.5F)
      .additive(0.3F)
      .emissive(0.6F)
      .soft(0.1F)
      .drag(0.1F)
      .gravity(-0.002F)
      .spin(0.0F)
      .stepped()
      .build();
   public static final ParticleStyle HALO = ParticleStyle.colour(VfxSprites.GLOW)
      .life(3, 3)
      .size(1.0F, 1.0F, 1.0F)
      .fade(0.3F, 0.4F)
      .additive(1.0F)
      .emissive(1.2F)
      .soft(0.4F)
      .drag(0.0F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle FLASH = ParticleStyle.colour(VfxSprites.FLARE)
      .life(5, 7)
      .size(0.5F, 1.4F, 0.5F)
      .fade(0.0F, 0.6F)
      .additive(1.0F)
      .emissive(2.4F)
      .soft(0.0F)
      .drag(0.0F)
      .spin(0.02F)
      .build();
   public static final ParticleStyle GIGGLE = ParticleStyle.colour(VfxSprites.HORO_GIGGLE)
      .life(16, 24)
      .size(0.26F, 0.2F, 1.0F)
      .fade(0.1F, 0.4F)
      .additive(0.0F)
      .emissive(0.6F)
      .soft(0.0F)
      .drag(0.08F)
      .gravity(-0.006F)
      .spin(0.0F)
      .loop(5.0F)
      .stepped()
      .build();
   public static final ParticleStyle DROP = ParticleStyle.colour(VfxSprites.HORO_DROP)
      .life(12, 18)
      .size(0.13F, 0.11F, 1.0F)
      .fade(0.1F, 0.3F)
      .additive(0.0F)
      .emissive(0.15F)
      .soft(0.0F)
      .drag(0.02F)
      .gravity(0.03F)
      .collide(0.0F)
      .spin(0.0F)
      .build();

   private HoroLook() {
   }

   static {
      for (int i = 0; i < SPARKLES.length; i++) {
         SPARKLES[i] = ParticleStyle.colour(VfxSprites.HORO_SPARKLES[i])
            .life(6, 11)
            .size(0.42F, 0.12F, 1.0F)
            .fade(0.15F, 0.5F)
            .additive(1.0F)
            .emissive(2.6F)
            .soft(0.0F)
            .drag(0.12F)
            .spin(0.0F)
            .build();
      }
   }
}
