package com.devilfruits.fruits.supa_supa.client.blade;

import com.devilfruits.client.logia.LogiaSkinLayout;
import com.devilfruits.client.render.SkinPixels;
import com.devilfruits.fruits.supa_supa.BladeLimbs;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;

final class BladeSkinMask implements AutoCloseable {
   private static final int GRID = 64;
   private static final int STEPS = 16;
   private static final Map<UUID, BladeSkinMask> MASKS = new HashMap<>();
   private static final Map<UUID, Integer> RETRY = new HashMap<>();
   private static int clock;
   private final ResourceLocation skin;
   private final boolean slim;
   private final int[] source;
   private final int size;
   private final int scale;
   private final DynamicTexture texture;
   private final ResourceLocation location;
   private final byte[] limbOf = new byte[4096];
   private final byte[] along = new byte[4096];
   private int builtKey = -1;
   private int lastUsed;

   private BladeSkinMask(ResourceLocation skin, boolean slim, int[] source) {
      this.skin = skin;
      this.slim = slim;
      this.source = source;
      this.size = (int)Math.round(Math.sqrt((double)source.length));
      this.scale = Math.max(1, this.size / 64);
      LogiaSkinLayout.classify(this.limbOf, this.along, slim);
      this.texture = new DynamicTexture(this.size, this.size, true);
      this.location = Minecraft.getInstance().getTextureManager().register("devilfruits_supa_skin", this.texture);
   }

   static float cut(BladeLimbsClient.Limb limb, float partial) {
      BladeLimbs.ArmMode mode = limb.shown();
      if (mode != BladeLimbs.ArmMode.FOREARM && mode != BladeLimbs.ArmMode.X_BLADES) {
         return 0.0F;
      } else {
         float g = limb.growth(partial);
         return BladeShapes.smooth((g - 0.22F) / 0.58F);
      }
   }

   @Nullable
   static ResourceLocation texture(AbstractClientPlayer player) {
      BladeLimbsClient.Limbs limbs = BladeLimbsClient.of(player);
      if (limbs == null) {
         return null;
      } else {
         float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
         int r = Math.round(cut(limbs.right, partial) * 16.0F);
         int l = Math.round(cut(limbs.left, partial) * 16.0F);
         int st = Math.round(BladeShapes.smooth(limbs.steel.growth(partial)) * 16.0F);
         if (r == 0 && l == 0 && st == 0) {
            return null;
         } else {
            boolean x = limbs.right.shown() == BladeLimbs.ArmMode.X_BLADES || limbs.left.shown() == BladeLimbs.ArmMode.X_BLADES;
            PlayerSkin s = player.getSkin();
            boolean slim = s.model() == Model.SLIM;
            UUID id = player.getUUID();
            BladeSkinMask mask = MASKS.get(id);
            if (mask != null && (!mask.skin.equals(s.texture()) || mask.slim != slim)) {
               MASKS.remove(id).close();
               mask = null;
            }

            if (mask == null) {
               Integer retry = RETRY.get(id);
               if (retry != null && clock < retry) {
                  return null;
               }

               int[] pixels = SkinPixels.read(s.texture(), 64, 512);
               if (pixels == null) {
                  RETRY.put(id, clock + 40);
                  return null;
               }

               RETRY.remove(id);
               mask = new BladeSkinMask(s.texture(), slim, pixels);
               MASKS.put(id, mask);
            }

            mask.lastUsed = clock;
            mask.build(r, l, x, st);
            return mask.location;
         }
      }
   }

   private void build(int right, int left, boolean x, int steel) {
      int key = right | left << 5 | (x ? 1024 : 0) | steel << 11;
      if (key != this.builtKey) {
         this.builtKey = key;
         NativeImage out = this.texture.getPixels();
         if (out != null) {
            for (int y = 0; y < this.size; y++) {
               for (int px = 0; px < this.size; px++) {
                  out.setPixelRGBA(px, y, this.source[y * this.size + px]);
               }
            }

            float from = x ? 3.0F : 3.5F;
            int dx = this.slim ? 3 : 4;
            if (steel > 0) {
               this.steel(out, (float)steel / 16.0F);
            }

            this.arm(out, 40, 16, dx, (float)right / 16.0F, from);
            this.arm(out, 32, 48, dx, (float)left / 16.0F, from);
            this.texture.upload();
         }
      }
   }

   private void arm(NativeImage out, int u0, int v0, int dx, float cut, float from) {
      if (!(cut <= 0.0F)) {
         int dz = 4;

         for (int row = 0; row < 12; row++) {
            float y = (float)(row - 2) + 0.5F;
            if (reached(y, cut, from)) {
               int v = v0 + dz + row;

               for (int c = 0; c < dz; c++) {
                  float zw = 1.5F - (float)c;
                  float ze = (float)c - 1.5F;
                  if (behind(zw, y, from)) {
                     this.clear(out, u0 + c, v);
                  }

                  if (behind(ze, y, from)) {
                     this.clear(out, u0 + dz + dx + c, v);
                  }
               }

               if (behind(1.95F, y, from)) {
                  for (int c = 0; c < dx; c++) {
                     this.clear(out, u0 + 2 * dz + dx + c, v);
                  }
               }
            }
         }

         if (reached(9.5F, cut, from)) {
            for (int j = 0; j < 2; j++) {
               for (int c = 0; c < dx; c++) {
                  this.clear(out, u0 + dz + dx + c, v0 + j);
               }
            }
         }
      }
   }

   private float reach(int i) {
      int limb = this.limbOf[i];
      float a = (float)(this.along[i] & 255) / 255.0F;

      return switch (limb) {
         case 0 -> 0.3F + 0.6F * (1.0F - a);
         case 1 -> 0.12F * Math.abs(a - 0.3F) / 0.7F;
         case 2, 3 -> 0.15F + 0.85F * a;
         case 4, 5 -> 0.25F + 0.75F * a;
         default -> 1.0F;
      };
   }

   private void steel(NativeImage out, float progress) {
      float front = progress * 1.12F;

      for (int gy = 0; gy < 64; gy++) {
         for (int gx = 0; gx < 64; gx++) {
            int i = gy * 64 + gx;
            if (this.limbOf[i] >= 0) {
               float d = this.reach(i);
               if (!(d > front)) {
                  boolean rim = front < 1.1F && d > front - 0.08F;
                  boolean band = (gx + gy) % 9 < 2;

                  for (int y = gy * this.scale; y < (gy + 1) * this.scale; y++) {
                     for (int x = gx * this.scale; x < (gx + 1) * this.scale; x++) {
                        int c = this.source[y * this.size + x];
                        int a = c >>> 24;
                        if (a >= 26) {
                           float lum = ((float)(c & 0xFF) * 0.3F + (float)(c >> 8 & 0xFF) * 0.59F + (float)(c >> 16 & 0xFF) * 0.11F) / 255.0F;
                           float k = (float)Math.pow((double)Math.min(1.0F, lum * 1.15F), 1.4);
                           float r = 58.0F + 138.0F * k;
                           float g = 66.0F + 142.0F * k;
                           float b = 86.0F + 144.0F * k;
                           int grain = (gx * 3 + gy * 7) % 17;
                           if (grain == 0) {
                              r = 250.0F;
                              g = 252.0F;
                              b = 255.0F;
                           } else if (grain == 8) {
                              r *= 0.8F;
                              g *= 0.8F;
                              b *= 0.85F;
                           }

                           if (band) {
                              r = r * 0.5F + 29.0F;
                              g = g * 0.5F + 42.5F;
                              b = b * 0.5F + 100.0F;
                           }

                           if (rim) {
                              r = 255.0F;
                              g = 255.0F;
                              b = 255.0F;
                           }

                           out.setPixelRGBA(x, y, a << 24 | (int)b << 16 | (int)g << 8 | (int)r);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static boolean reached(float y, float cut, float from) {
      return y >= from && (y - from) / (10.0F - from) <= cut * 1.05F;
   }

   private static boolean behind(float z, float y, float from) {
      return z > 0.0F && z <= y - from + 0.25F;
   }

   private void clear(NativeImage out, int gx, int gy) {
      for (int y = gy * this.scale; y < (gy + 1) * this.scale; y++) {
         for (int x = gx * this.scale; x < (gx + 1) * this.scale; x++) {
            out.setPixelRGBA(x, y, 0);
         }
      }
   }

   static void tick() {
      clock++;
      Iterator<BladeSkinMask> it = MASKS.values().iterator();

      while (it.hasNext()) {
         BladeSkinMask m = it.next();
         if (clock - m.lastUsed > 200) {
            m.close();
            it.remove();
         }
      }
   }

   static void clear() {
      MASKS.values().forEach(BladeSkinMask::close);
      MASKS.clear();
      RETRY.clear();
   }

   @Override
   public void close() {
      Minecraft.getInstance().getTextureManager().release(this.location);
   }
}
