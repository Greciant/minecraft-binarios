package com.devilfruits.client.vfx.sprite;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.core.config.CoreClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class VfxSprites {
   public static final ResourceLocation ATLAS = DevilFruits.id("textures/vfx/atlas.png");
   public static final ResourceLocation LUT = DevilFruits.id("textures/vfx/lut.png");
   public static final ResourceLocation FLAME_RIBBON = DevilFruits.id("textures/vfx/flame_ribbon.png");
   public static final ResourceLocation LANCE = DevilFruits.id("textures/vfx/lance.png");
   public static final ResourceLocation NOISE = DevilFruits.id("textures/vfx/noise.png");
   public static final ResourceLocation HEAT_NORMAL = DevilFruits.id("textures/vfx/heat_normal.png");
   public static final ResourceLocation LIGHTNING = DevilFruits.id("textures/vfx/lightning_branches.png");
   public static final ResourceLocation CRACKLE = DevilFruits.id("textures/vfx/crackle.png");
   public static final ResourceLocation VEIL_SHEEN = DevilFruits.id("textures/vfx/veil_sheen.png");
   public static final ResourceLocation SAND_STREAM = DevilFruits.id("textures/vfx/sand_stream.png");
   public static final ResourceLocation SAND_SHEET = DevilFruits.id("textures/vfx/sand_sheet.png");
   public static final ResourceLocation PACK_HI = DevilFruits.id("textures/vfx/explosion_hi.png");
   public static final ResourceLocation PACK_MED = DevilFruits.id("textures/vfx/explosion_med.png");
   public static final int FIRE = 0;
   public static final int THUNDER = 1;
   public static final int SMOKE = 2;
   public static final int SOUL_FIRE = 3;
   public static final int BLACKBODY = 4;
   public static final int STEEL = 5;
   public static final int SOUL = 6;
   public static final int FROST = 7;
   public static final VfxSprites.Sprite FIRE_PUFF = px(0, 0, 1024, 1024, 8, 8, 64);
   public static final VfxSprites.Sprite FIREBALL = px(1024, 0, 1024, 1024, 8, 8, 64);
   public static final VfxSprites.Sprite FLARE = px(1024, 1024, 256, 256, 1, 1, 1);
   public static final VfxSprites.Sprite EMBER = px(1920, 1024, 32, 32, 1, 1, 1);
   public static final VfxSprites.Sprite SPARK = px(1952, 1024, 64, 16, 1, 1, 1);
   public static final VfxSprites.Sprite SCORCH_GLOW = px(1536, 1280, 512, 512, 1, 1, 1);
   public static final VfxSprites.Sprite CLAW_GLOW = px(1280, 1792, 256, 256, 1, 1, 1);
   public static final VfxSprites.Sprite SMOKE_PUFF = px(0, 1024, 1024, 1024, 8, 8, 64);
   public static final VfxSprites.Sprite RING = px(1280, 1024, 256, 256, 1, 1, 1);
   public static final VfxSprites.Sprite GLOW = px(1792, 1024, 128, 128, 1, 1, 1);
   public static final VfxSprites.Sprite SCORCH = px(1024, 1280, 512, 512, 1, 1, 1);
   public static final VfxSprites.Sprite CLAW = px(1024, 1792, 256, 256, 1, 1, 1);
   public static final VfxSprites.Sprite SAND_PLUME = px(0, 2048, 1024, 1024, 8, 8, 64);
   public static final VfxSprites.Sprite SAND_RING = px(1024, 2048, 768, 768, 4, 4, 16);
   public static final VfxSprites.Sprite SAND_DECAL = px(1792, 2048, 256, 256, 1, 1, 1);
   public static final VfxSprites.Sprite[] SAND_GRAINS = new VfxSprites.Sprite[]{
      px(1536, 1792, 128, 128, 1, 1, 1), px(1664, 1792, 128, 128, 1, 1, 1), px(1792, 1792, 128, 128, 1, 1, 1), px(1920, 1792, 128, 128, 1, 1, 1)
   };
   public static final VfxSprites.Sprite SAND_CLUMP = px(0, 3072, 1024, 1024, 4, 4, 16);
   public static final VfxSprites.Sprite SAND_STREAK = px(1536, 1920, 128, 32, 1, 1, 1);
   public static final VfxSprites.Sprite RING_NORMAL = px(1536, 1024, 256, 256, 1, 1, 1);
   public static final VfxSprites.Sprite PACK_FIREBALL = pack(0, 0, 2048, 2048, 8, 8, 64);
   public static final VfxSprites.Sprite PACK_SMOKE = pack(2048, 0, 2048, 2048, 8, 8, 64);
   public static final VfxSprites.Sprite PACK_MUSHROOM = pack(0, 2048, 2048, 2048, 8, 8, 64);
   public static final VfxSprites.Sprite PACK_DUST_RING = pack(2048, 2048, 1024, 1024, 4, 4, 16);
   public static final VfxSprites.Sprite PACK_COLUMN = pack(3072, 2048, 1024, 1024, 4, 4, 16);
   public static final VfxSprites.Sprite PACK_SCORCH = pack(2048, 3072, 512, 512, 1, 1, 1);
   public static final VfxSprites.Sprite PACK_SOOT = pack(2560, 3072, 512, 512, 1, 1, 1);
   public static final VfxSprites.Sprite PACK_STREAK = pack(3072, 3072, 512, 128, 1, 1, 1);
   public static final VfxSprites.Sprite PACK_SHOCK_RING = pack(3072, 3200, 256, 256, 1, 1, 1);
   public static final VfxSprites.Sprite PACK_SPARK = pack(3328, 3200, 128, 32, 1, 1, 1);
   public static final VfxSprites.Sprite PACK_EMBER = pack(3456, 3200, 64, 64, 1, 1, 1);
   public static final VfxSprites.Sprite[] PACK_DEBRIS = new VfxSprites.Sprite[]{
      pack(3520, 3200, 64, 64, 1, 1, 1), pack(3584, 3200, 64, 64, 1, 1, 1), pack(3648, 3200, 64, 64, 1, 1, 1), pack(3712, 3200, 64, 64, 1, 1, 1)
   };
   public static final VfxSprites.Sprite WAX_SPLASH = px(1024, 2816, 512, 512, 4, 4, 16);
   public static final VfxSprites.Sprite CANDLE_FLAME = px(1536, 2816, 512, 512, 4, 4, 16);
   public static final VfxSprites.Sprite WAX_BEAD = px(1024, 3328, 64, 64, 1, 1, 1);
   public static final VfxSprites.Sprite[] WAX_FLAKES = new VfxSprites.Sprite[]{
      px(1088, 3328, 64, 64, 1, 1, 1), px(1152, 3328, 64, 64, 1, 1, 1), px(1216, 3328, 64, 64, 1, 1, 1), px(1280, 3328, 64, 64, 1, 1, 1)
   };
   public static final VfxSprites.Sprite WAX_GLOSS = px(1344, 3328, 128, 32, 1, 1, 1);
   public static final VfxSprites.Sprite WAX_PUDDLE = px(1024, 3392, 256, 256, 1, 1, 1);
   public static final VfxSprites.Sprite WAX_CRACK = px(1280, 3392, 256, 256, 1, 1, 1);
   public static final VfxSprites.Sprite YOMI_FLAME = px(1024, 3712, 768, 384, 4, 2, 8);
   public static final VfxSprites.Sprite YOMI_WISP = px(1536, 3328, 384, 96, 4, 1, 4);
   public static final VfxSprites.Sprite[] YOMI_CHIPS = new VfxSprites.Sprite[]{
      px(1536, 3424, 64, 64, 1, 1, 1), px(1600, 3424, 64, 64, 1, 1, 1), px(1664, 3424, 64, 64, 1, 1, 1), px(1728, 3424, 64, 64, 1, 1, 1)
   };
   public static final VfxSprites.Sprite YOMI_SNOW = px(1792, 3424, 64, 64, 1, 1, 1);
   public static final VfxSprites.Sprite[] YOMI_FROST = new VfxSprites.Sprite[]{px(1536, 3488, 128, 128, 1, 1, 1), px(1664, 3488, 128, 128, 1, 1, 1)};
   public static final ResourceLocation YOMI_TENDRIL = DevilFruits.id("textures/vfx/yomi_yomi/soul_tendril.png");
   public static final ResourceLocation YOMI_CHILL_PIP = DevilFruits.id("textures/vfx/yomi_yomi/chill_pip.png");
   public static final VfxSprites.Sprite[] HORO_SPARKLES = new VfxSprites.Sprite[]{
      px(1792, 2304, 64, 64, 1, 1, 1), px(1856, 2304, 64, 64, 1, 1, 1), px(1920, 2304, 64, 64, 1, 1, 1)
   };
   public static final VfxSprites.Sprite HORO_DROP = px(1984, 2304, 64, 64, 1, 1, 1);
   public static final VfxSprites.Sprite HORO_PUFF = px(1792, 2368, 256, 64, 4, 1, 4);
   public static final VfxSprites.Sprite HORO_GLOOM = px(1792, 2432, 256, 128, 2, 2, 4);
   public static final VfxSprites.Sprite HORO_LINES = px(1792, 2560, 128, 128, 1, 1, 1);
   public static final VfxSprites.Sprite HORO_GIGGLE = px(1920, 2560, 128, 64, 2, 1, 2);
   public static final ResourceLocation HORO_MIST = DevilFruits.id("textures/vfx/horo_horo/mist_band.png");
   public static final ResourceLocation HORO_VIGNETTE = DevilFruits.id("textures/vfx/horo_horo/gloom_vignette.png");
   public static final ResourceLocation WAX_STREAM = DevilFruits.id("textures/vfx/wax_stream.png");
   public static final ResourceLocation CUT_RIBBON = DevilFruits.id("textures/vfx/cut_ribbon.png");
   private static boolean packWanted;

   private static VfxSprites.Sprite px(int x, int y, int w, int h, int cols, int rows, int frames) {
      float su = 4.8828125E-4F;
      float sv = 2.4414062E-4F;
      return new VfxSprites.Sprite((float)x * su, (float)y * sv, (float)(x + w) * su, (float)(y + h) * sv, cols, rows, frames, 0);
   }

   private static VfxSprites.Sprite pack(int x, int y, int w, int h, int cols, int rows, int frames) {
      float s = 2.4414062E-4F;
      return new VfxSprites.Sprite((float)x * s, (float)y * s, (float)(x + w) * s, (float)(y + h) * s, cols, rows, frames, 1);
   }

   public static ResourceLocation packTexture() {
      return VfxSettings.tier() == CoreClientConfig.CinematicQuality.MEDIUM ? PACK_MED : PACK_HI;
   }

   public static void wantPack() {
      packWanted = true;
   }

   public static int packId(int atlasId) {
      return packWanted ? id(packTexture()) : atlasId;
   }

   public static int id(ResourceLocation texture) {
      return Minecraft.getInstance().getTextureManager().getTexture(texture).getId();
   }

   private VfxSprites() {
   }

   public static record Sprite(float u0, float v0, float u1, float v1, int cols, int rows, int frames, int atlas) {
      public float frameU(int frame) {
         return this.u0 + (float)(frame % this.cols) * (this.u1 - this.u0) / (float)this.cols;
      }

      public float frameV(int frame) {
         return this.v0 + (float)(frame / this.cols) * (this.v1 - this.v0) / (float)this.rows;
      }

      public float cellU() {
         return (this.u1 - this.u0) / (float)this.cols;
      }

      public float cellV() {
         return (this.v1 - this.v0) / (float)this.rows;
      }
   }
}
