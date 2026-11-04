package com.devilfruits.client.haki;

import com.devilfruits.client.logia.LogiaSkinLayout;
import com.devilfruits.client.render.SkinPixels;
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

final class HakiSkinCoat implements AutoCloseable {
   private static final int GRID = 64;
   private static final int MAX_SIZE = 512;
   private static final int FIST_FROM = 175;
   private static final int[] BAYER = new int[]{0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
   private static final Map<UUID, HakiSkinCoat> COATS = new HashMap<>();
   private static final Map<UUID, Integer> RETRY = new HashMap<>();
   private static int clock;
   private final ResourceLocation skin;
   private final boolean slim;
   private final ResourceLocation location;
   private final DynamicTexture texture;
   private final int size;
   private final int scale;
   private final int[] source;
   private final byte[] limbOf = new byte[4096];
   private final byte[] along = new byte[4096];
   private final float[] threshold = new float[4096];
   private int builtMask = -1;
   private int lastUsed;

   private HakiSkinCoat(ResourceLocation skin, boolean slim, int[] source) {
      this.skin = skin;
      this.slim = slim;
      this.source = source;
      this.size = (int)Math.round(Math.sqrt((double)source.length));
      this.scale = this.size / 64;
      LogiaSkinLayout.classify(this.limbOf, this.along, slim);

      for (int y = 0; y < 64; y++) {
         for (int x = 0; x < 64; x++) {
            int i = y * 64 + x;
            float n = 0.6F * ((float)BAYER[(y & 3) * 4 + (x & 3)] + 0.5F) / 16.0F + 0.4F * hash(x, y);
            float t = this.limbOf[i] >= 2 ? 0.65F * (1.0F - (float)(this.along[i] & 255) / 255.0F) + 0.35F * n : n;
            this.threshold[i] = 0.1F + 0.9F * Math.max(0.0F, Math.min(1.0F, t));
         }
      }

      this.texture = new DynamicTexture(this.size, this.size, true);
      this.location = Minecraft.getInstance().getTextureManager().register("devilfruits_haki_coat", this.texture);
   }

   @Nullable
   static ResourceLocation texture(AbstractClientPlayer player, int mask) {
      PlayerSkin skin = player.getSkin();
      boolean slim = skin.model() == Model.SLIM;
      UUID id = player.getUUID();
      HakiSkinCoat coat = COATS.get(id);
      if (coat != null && (!coat.skin.equals(skin.texture()) || coat.slim != slim)) {
         COATS.remove(id).close();
         coat = null;
      }

      if (coat == null) {
         Integer retry = RETRY.get(id);
         if (retry != null && clock < retry) {
            return null;
         }

         int[] pixels = SkinPixels.read(skin.texture(), 64, 512);
         if (pixels == null) {
            RETRY.put(id, clock + 40);
            return null;
         }

         RETRY.remove(id);
         coat = new HakiSkinCoat(skin.texture(), slim, pixels);
         COATS.put(id, coat);
      }

      coat.lastUsed = clock;
      coat.build(mask);
      return coat.location;
   }

   static void tick() {
      clock++;
      Iterator<HakiSkinCoat> it = COATS.values().iterator();

      while (it.hasNext()) {
         HakiSkinCoat coat = it.next();
         if (clock - coat.lastUsed > 200) {
            coat.close();
            it.remove();
         }
      }
   }

   static void clear() {
      COATS.values().forEach(HakiSkinCoat::close);
      COATS.clear();
      RETRY.clear();
   }

   private void build(int mask) {
      if (mask != this.builtMask) {
         this.builtMask = mask;
         NativeImage out = this.texture.getPixels();
         if (out != null) {
            for (int y = 0; y < this.size; y++) {
               int gy = y / this.scale;

               for (int x = 0; x < this.size; x++) {
                  int i = gy * 64 + x / this.scale;
                  int c = this.source[y * this.size + x];
                  int limb = this.limbOf[i];
                  if (limb >= 0 && c >>> 24 >= 26 && coated(mask, limb, this.along[i] & 255)) {
                     int alpha = Math.min(255, (int)Math.ceil((double)(26.0F / this.threshold[i])));
                     int lum = (c & 0xFF) * 77 + (c >> 8 & 0xFF) * 150 + (c >> 16 & 0xFF) * 29 >> 8;
                     int v = 16 + lum * 26 / 255;
                     out.setPixelRGBA(x, y, alpha << 24 | v + 7 << 16 | v + 1 << 8 | v);
                  } else {
                     out.setPixelRGBA(x, y, 0);
                  }
               }
            }

            this.texture.upload();
         }
      }
   }

   private static boolean coated(int mask, int limb, int along) {
      return switch (limb) {
         case 0 -> (mask & 128) != 0;
         case 1 -> (mask & 64) != 0;
         case 2 -> (mask & 4) != 0 || (mask & 1) != 0 && along >= 175;
         case 3 -> (mask & 8) != 0 || (mask & 2) != 0 && along >= 175;
         case 4 -> (mask & 16) != 0;
         case 5 -> (mask & 32) != 0;
         default -> false;
      };
   }

   private static float hash(int x, int y) {
      int h = x * 374761393 + y * 668265263;
      h = (h ^ h >>> 13) * 1274126177;
      return (float)((h ^ h >>> 16) & 65535) / 65536.0F;
   }

   @Override
   public void close() {
      Minecraft.getInstance().getTextureManager().release(this.location);
   }
}
