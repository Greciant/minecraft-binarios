package com.devilfruits.fruits.yomi_yomi.client;

import com.devilfruits.client.render.SkinPixels;
import com.devilfruits.fruits.yomi_yomi.Vessel;
import com.devilfruits.fruits.yomi_yomi.YomiYomi;
import com.devilfruits.registry.DFAttachments;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.resources.ResourceLocation;

public final class VesselSkins {
   private static final ResourceLocation SKELETON = ResourceLocation.withDefaultNamespace("textures/entity/skeleton/skeleton.png");
   private static final int GRID = 64;
   public static boolean thinLimbs = true;
   @Nullable
   public static Vessel preview;
   private static final Map<UUID, VesselSkins.Corpse> CORPSES = new HashMap<>();
   private static int clock;

   @Nullable
   public static ResourceLocation skin(AbstractClientPlayer player) {
      Vessel v = preview != null && player == Minecraft.getInstance().player ? preview : shown(player);
      return v != Vessel.CORPSE ? null : corpse(player, player.getSkin().model() == Model.SLIM);
   }

   @Nullable
   public static ResourceLocation corpseSkin(AbstractClientPlayer player) {
      return corpse(player, player.getSkin().model() == Model.SLIM);
   }

   @Nullable
   public static Vessel shown(AbstractClientPlayer player) {
      if (player.hasData(DFAttachments.PLAYER_DATA) && YomiYomi.ID.equals(DFAttachments.data(player).fruitId())) {
         String form = DFAttachments.data(player).formName();
         return "soul_king".equals(form) ? Vessel.SKELETON : Vessel.byName(form);
      } else {
         return null;
      }
   }

   @Nullable
   private static ResourceLocation corpse(AbstractClientPlayer player, boolean slim) {
      ResourceLocation skin = player.getSkin().texture();
      VesselSkins.Corpse c = CORPSES.get(player.getUUID());
      if (c != null && (!c.skin.equals(skin) || c.slim != slim)) {
         CORPSES.remove(player.getUUID()).close();
         c = null;
      }

      if (c == null) {
         int[] pixels = SkinPixels.read(skin, 64, 512);
         if (pixels == null) {
            return null;
         }

         c = new VesselSkins.Corpse(skin, slim, pixels);
         CORPSES.put(player.getUUID(), c);
      }

      c.lastUsed = clock;
      return c.location;
   }

   private static int tint(int c, int gx, int gy) {
      int a = c >>> 24;
      if (a < 8) {
         return c;
      } else {
         float r = (float)(c & 0xFF);
         float g = (float)(c >> 8 & 0xFF);
         float b = (float)(c >> 16 & 0xFF);
         float lum = r * 0.3F + g * 0.59F + b * 0.11F;
         r += (lum - r) * 0.5F;
         g += (lum - g) * 0.5F;
         b += (lum - b) * 0.5F;
         r = r * 0.7F + 31.2F;
         g = g * 0.7F + 41.4F;
         b = b * 0.7F + 26.400002F;
         float bruise = blotch(gx, gy);
         if (bruise > 0.0F) {
            r += (92.0F - r) * bruise;
            g += (66.0F - g) * bruise;
            b += (108.0F - b) * bruise;
         }

         float dark = 0.9F;
         return a << 24 | clamp(b * dark) << 16 | clamp(g * dark) << 8 | clamp(r * dark);
      }
   }

   private static float blotch(int gx, int gy) {
      int cx = gx / 3;
      int cy = gy / 3;
      int h = cx * 73856093 ^ cy * 19349663;
      h = (h ^ h >>> 13) * 1274126177;
      float n = (float)(h >>> 8 & 65535) / 65535.0F;
      if (n < 0.84F) {
         return 0.0F;
      } else {
         float fx = (float)(gx % 3) - 1.0F;
         float fy = (float)(gy % 3) - 1.0F;
         return (n - 0.84F) / 0.16F * 0.55F * (1.0F - 0.25F * (fx * fx + fy * fy));
      }
   }

   private static int clamp(float v) {
      return Math.max(0, Math.min(255, Math.round(v)));
   }

   private static void stitches(NativeImage out, int scale, boolean slim) {
      int armEnd = slim ? 54 : 56;
      line(out, scale, 16, 20, 40, 20);
      line(out, scale, 9, 9, 15, 9);

      for (int i = 0; i <= 6; i++) {
         stitch(out, scale, 21 + i, 23 + i, i % 2 == 0);
      }

      line(out, scale, 40, 24, armEnd, 24);
      line(out, scale, 16, 55, 32, 55);
   }

   private static void line(NativeImage out, int scale, int x0, int y, int x1, int y1) {
      for (int x = x0; x < x1; x++) {
         stitch(out, scale, x, y, (x - x0) % 2 == 0);
      }
   }

   private static void stitch(NativeImage out, int scale, int gx, int gy, boolean tick) {
      put(out, scale, gx, gy, -14673374);
      if (tick) {
         put(out, scale, gx, gy - 1, -14015952);
         put(out, scale, gx, gy + 1, -14015952);
      }
   }

   private static void put(NativeImage out, int scale, int gx, int gy, int abgr) {
      if (gx >= 0 && gy >= 0 && gx < 64 && gy < 64) {
         for (int y = gy * scale; y < (gy + 1) * scale; y++) {
            for (int x = gx * scale; x < (gx + 1) * scale; x++) {
               if (out.getPixelRGBA(x, y) >>> 24 > 8) {
                  out.setPixelRGBA(x, y, abgr);
               }
            }
         }
      }
   }

   static void tick() {
      clock++;
      Iterator<VesselSkins.Corpse> it = CORPSES.values().iterator();

      while (it.hasNext()) {
         VesselSkins.Corpse c = it.next();
         if (clock - c.lastUsed > 200) {
            c.close();
            it.remove();
         }
      }
   }

   static void clear() {
      CORPSES.values().forEach(VesselSkins.Corpse::close);
      CORPSES.clear();
   }

   private VesselSkins() {
   }

   private static final class Corpse implements AutoCloseable {
      final ResourceLocation skin;
      final boolean slim;
      final ResourceLocation location;
      int lastUsed;

      Corpse(ResourceLocation skin, boolean slim, int[] source) {
         this.skin = skin;
         this.slim = slim;
         int size = (int)Math.round(Math.sqrt((double)source.length));
         int scale = Math.max(1, size / 64);
         NativeImage out = new NativeImage(size, size, true);

         for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
               out.setPixelRGBA(x, y, VesselSkins.tint(source[y * size + x], x / scale, y / scale));
            }
         }

         VesselSkins.stitches(out, scale, slim);
         this.location = Minecraft.getInstance().getTextureManager().register("devilfruits_yomi_corpse", new DynamicTexture(out));
      }

      @Override
      public void close() {
         Minecraft.getInstance().getTextureManager().release(this.location);
      }
   }
}
