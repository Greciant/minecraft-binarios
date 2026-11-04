package com.devilfruits.fruits.kage_kage.client.vfx;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.vfx.particle.ParticleStyle;
import com.devilfruits.client.vfx.sprite.VfxSprites;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class KageLook {
   public static final int RIM = 12868576;
   public static final int HOT = 14708991;
   public static final int DEEP = 8007103;
   public static final int BLACK = 459788;
   public static final int MATTER = 2495552;
   public static final ParticleStyle GLOW = ParticleStyle.colour(VfxSprites.GLOW)
      .life(5, 8)
      .size(1.0F, 1.25F, 1.0F)
      .fade(0.15F, 0.5F)
      .additive(1.0F)
      .emissive(2.2F)
      .soft(0.4F)
      .drag(0.0F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle FLASH = ParticleStyle.colour(VfxSprites.GLOW)
      .life(3, 4)
      .size(1.0F, 1.6F, 0.6F)
      .fade(0.0F, 0.7F)
      .additive(1.0F)
      .emissive(3.5F)
      .soft(0.3F)
      .drag(0.0F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle MOTE = ParticleStyle.colour(VfxSprites.GLOW)
      .life(14, 24)
      .size(0.09F, 0.02F, 1.0F)
      .fade(0.2F, 0.5F)
      .additive(1.0F)
      .emissive(3.0F)
      .soft(0.0F)
      .drag(0.06F)
      .gravity(-0.003F)
      .spin(0.0F)
      .build();
   public static final ParticleStyle FLOOR_GLOW = ParticleStyle.colour(VfxSprites.GLOW)
      .life(3, 3)
      .size(1.0F, 1.0F, 1.0F)
      .fade(0.3F, 0.4F)
      .additive(1.0F)
      .emissive(0.7F)
      .soft(0.0F)
      .flat()
      .spin(0.0F)
      .build();
   public static final ParticleStyle RIPPLE = ParticleStyle.colour(VfxSprites.RING)
      .life(9, 11)
      .size(1.0F, 5.0F, 0.5F)
      .fade(0.0F, 0.7F)
      .additive(1.0F)
      .emissive(1.2F)
      .soft(0.0F)
      .flat()
      .spin(0.0F)
      .build();
   private static ResourceLocation white;

   public static ResourceLocation white() {
      if (white == null) {
         NativeImage image = new NativeImage(4, 4, false);
         image.fillRect(0, 0, 4, 4, -1);
         white = DevilFruits.id("dynamic/kage_white");
         Minecraft.getInstance().getTextureManager().register(white, new DynamicTexture(image));
      }

      return white;
   }

   public static int scale(int rgb, float k) {
      int r = Mth.clamp((int)((float)(rgb >> 16 & 0xFF) * k), 0, 255);
      int g = Mth.clamp((int)((float)(rgb >> 8 & 0xFF) * k), 0, 255);
      int b = Mth.clamp((int)((float)(rgb & 0xFF) * k), 0, 255);
      return r << 16 | g << 8 | b;
   }

   public static int mix(int a, int b, float t) {
      t = Mth.clamp(t, 0.0F, 1.0F);
      int r = (int)Mth.lerp(t, (float)(a >> 16 & 0xFF), (float)(b >> 16 & 0xFF));
      int g = (int)Mth.lerp(t, (float)(a >> 8 & 0xFF), (float)(b >> 8 & 0xFF));
      int bl = (int)Mth.lerp(t, (float)(a & 0xFF), (float)(b & 0xFF));
      return r << 16 | g << 8 | bl;
   }

   public static int rim(float heat) {
      return heat <= 1.0F ? mix(8007103, 12868576, heat) : mix(12868576, 14708991, heat - 1.0F);
   }

   public static void sound(ClientLevel level, Vec3 at, SoundEvent event, float volume, float pitch) {
      level.playLocalSound(at.x, at.y, at.z, event, SoundSource.PLAYERS, volume, pitch, false);
   }

   private KageLook() {
   }
}
