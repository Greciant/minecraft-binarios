package com.awakenedmod.awakened.client.ruin;

import com.awakenedmod.awakened.Awakened;
import com.awakenedmod.awakened.core.AwId;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;

public final class RuinCrackSheets {
   public static final int CELLS = 8;
   private static final int TILE = 256;
   private static final int FALLBACK_SIZE = 64;
   private static final int MAX_SOURCE = 512;
   private static final float GREY_R = 1.0F;
   private static final float GREY_G = 0.975F;
   private static final float GREY_B = 0.945F;
   private static final int LIT_CORE = 15592164;
   private static final int LIT_LIP = 12170413;
   private static final Map<ResourceLocation, RuinCrackSheets.Sheet> CACHE = new HashMap<>();
   private static NativeImage[] tiles;
   private static ResourceLocation empty;
   private static int serial;

   private RuinCrackSheets() {
   }

   public static void register(IEventBus modBus, IEventBus gameBus) {
      gameBus.addListener(e -> clear());
      ResourceManagerReloadListener onReload = manager -> clear();
      modBus.addListener(e -> e.registerReloadListener(onReload));
   }

   public static void clear() {
      TextureManager textures = Minecraft.getInstance().getTextureManager();

      for (RuinCrackSheets.Sheet sheet : CACHE.values()) {
         if (!sheet.crackAtlas().equals(empty)) {
            textures.release(sheet.crackAtlas());
         }

         if (sheet.grey() != null) {
            textures.release(sheet.grey());
         }
      }

      CACHE.clear();
      if (empty != null) {
         textures.release(empty);
         empty = null;
      }

      if (tiles != null) {
         for (NativeImage tile : tiles) {
            tile.close();
         }

         tiles = null;
      }
   }

   public static RuinCrackSheets.Sheet of(ResourceLocation texture, boolean playerSkin) {
      RuinCrackSheets.Sheet sheet = CACHE.get(texture);
      if (sheet == null) {
         sheet = bake(texture, playerSkin);
         CACHE.put(texture, sheet);
      }

      return sheet;
   }

   public static ResourceLocation emptyTexture() {
      if (empty == null) {
         NativeImage px = new NativeImage(1, 1, true);
         setArgb(px, 0, 0, 0);
         empty = register("ruin/empty", px);
         if (empty == null) {
            return AwId.of("ruin/empty");
         }
      }

      return empty;
   }

   private static RuinCrackSheets.Sheet bake(ResourceLocation texture, boolean playerSkin) {
      RuinCrackSheets.Source pixels = sourcePixels(texture);

      RuinCrackSheets.Sheet var3;
      try {
         var3 = bake(texture, playerSkin, pixels == null ? null : pixels.image());
      } finally {
         if (pixels != null && pixels.owned()) {
            pixels.image().close();
         }
      }

      return var3;
   }

   private static RuinCrackSheets.Sheet bake(ResourceLocation texture, boolean playerSkin, @Nullable NativeImage source) {
      int w = source == null ? 64 : source.getWidth();
      int h = source == null ? 64 : source.getHeight();
      if (w <= 0 || h <= 0 || w > 512 || h > 512) {
         source = null;
         w = 64;
         h = 64;
      }

      NativeImage[] crack = tiles();
      int salt = RuinLook.hash(texture.hashCode());
      int ox = salt & 0xFF;
      int oy = salt >>> 8 & 0xFF;
      NativeImage atlas = new NativeImage(w * 8, h, true);
      NativeImage grey = source == null ? null : new NativeImage(w, h, true);

      try {
         for (int y = 0; y < h; y++) {
            int ty = y + oy & 0xFF;

            for (int x = 0; x < w; x++) {
               int tx = x + ox & 0xFF;
               int src = source == null ? -1 : argb(source, x, y);
               int srcA = src >>> 24;
               if (grey != null) {
                  setArgb(grey, x, y, greyOf(src));
               }

               if (srcA != 0) {
                  for (int cell = 0; cell < 8; cell++) {
                     int stage = cell % 4;
                     int c = argb(crack[stage], tx, ty);
                     int a = (c >>> 24) * srcA / 255;
                     if (a != 0) {
                        int rgb = cell >= 4 ? lit(c) : c & 16777215;
                        setArgb(atlas, x + cell * w, y, a << 24 | rgb);
                     }
                  }
               }
            }
         }
      } catch (RuntimeException var22) {
         Awakened.LOGGER.warn("Ruin: could not bake decay textures for {}", texture, var22);
         atlas.close();
         if (grey != null) {
            grey.close();
         }

         return source == null ? new RuinCrackSheets.Sheet(emptyTexture(), null, 64, 64, playerSkin) : bake(texture, playerSkin, null);
      }

      String name = "ruin/" + serial++ + "_" + texture.getPath().replaceAll("[^a-z0-9_]", "_");
      ResourceLocation atlasId = register(name + "/cracks", atlas);
      if (atlasId == null) {
         if (grey != null) {
            grey.close();
         }

         return new RuinCrackSheets.Sheet(emptyTexture(), null, 64, 64, playerSkin);
      } else {
         ResourceLocation greyId = grey == null ? null : register(name + "/grey", grey);
         return new RuinCrackSheets.Sheet(atlasId, greyId, w, h, source == null && playerSkin);
      }
   }

   @Nullable
   private static RuinCrackSheets.Source sourcePixels(ResourceLocation texture) {
      Minecraft mc = Minecraft.getInstance();

      try {
         if (mc.getTextureManager().getTexture(texture) instanceof DynamicTexture dynamic && dynamic.getPixels() != null) {
            NativeImage px = dynamic.getPixels();
            if (px.format() == Format.RGBA) {
               return new RuinCrackSheets.Source(px, false);
            }
         }
      } catch (RuntimeException var7) {
      }

      Optional<Resource> resource = mc.getResourceManager().getResource(texture);
      if (resource.isEmpty()) {
         return null;
      } else {
         try {
            RuinCrackSheets.Source var5;
            try (InputStream in = resource.get().open()) {
               NativeImage image = NativeImage.read(in);
               if (image.format() != Format.RGBA) {
                  image.close();
                  return null;
               }

               var5 = new RuinCrackSheets.Source(image, true);
            }

            return var5;
         } catch (RuntimeException | IOException var9) {
            return null;
         }
      }
   }

   private static NativeImage[] tiles() {
      if (tiles == null) {
         tiles = new NativeImage[4];

         for (int i = 0; i < 4; i++) {
            tiles[i] = readResource(AwId.of("textures/entity/ruin/crack_" + (i + 1) + ".png"));
         }
      }

      return tiles;
   }

   private static NativeImage readResource(ResourceLocation id) {
      Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
      if (resource.isPresent()) {
         try {
            label60: {
               NativeImage var4;
               try (InputStream in = resource.get().open()) {
                  NativeImage image = NativeImage.read(in);
                  if (image.getWidth() != 256 || image.getHeight() != 256) {
                     image.close();
                     break label60;
                  }

                  var4 = image;
               }

               return var4;
            }
         } catch (IOException var7) {
         }
      }

      Awakened.LOGGER.warn("Ruin: crack tile {} missing or not {}x{}; using a blank one", new Object[]{id, 256, 256});
      return new NativeImage(256, 256, true);
   }

   private static int lit(int crackArgb) {
      int r = crackArgb >> 16 & 0xFF;
      int g = crackArgb >> 8 & 0xFF;
      int b = crackArgb & 0xFF;
      int lum = r * 77 + g * 151 + b * 28 >> 8;
      return lum > 90 ? 12170413 : 15592164;
   }

   private static int greyOf(int argb) {
      int a = argb >>> 24;
      int r = argb >> 16 & 0xFF;
      int g = argb >> 8 & 0xFF;
      int b = argb & 0xFF;
      int lum = r * 77 + g * 151 + b * 28 >> 8;
      int gr = Math.min(255, Math.round((float)lum * 1.0F));
      int gg = Math.min(255, Math.round((float)lum * 0.975F));
      int gb = Math.min(255, Math.round((float)lum * 0.945F));
      return a << 24 | gr << 16 | gg << 8 | gb;
   }

   @Nullable
   private static ResourceLocation register(String path, NativeImage pixels) {
      ResourceLocation id = AwId.of(path);
      TextureManager textures = Minecraft.getInstance().getTextureManager();
      DynamicTexture texture = null;

      try {
         texture = new DynamicTexture(pixels);
         textures.register(id, texture);
         return id;
      } catch (RuntimeException var6) {
         Awakened.LOGGER.warn("Ruin: could not upload decay texture {}", id, var6);
         if (texture != null) {
            texture.close();
         } else {
            pixels.close();
         }

         return null;
      }
   }

   static int argb(NativeImage image, int x, int y) {
      return abgrToArgb(image.getPixelRGBA(x, y));
   }

   static void setArgb(NativeImage image, int x, int y, int argb) {
      image.setPixelRGBA(x, y, abgrToArgb(argb));
   }

   private static int abgrToArgb(int c) {
      return c & -16711936 | c >> 16 & 0xFF | (c & 0xFF) << 16;
   }

   public static record Sheet(ResourceLocation crackAtlas, @Nullable ResourceLocation grey, int width, int height, boolean playerLayout) {
   }

   private static record Source(NativeImage image, boolean owned) {
   }
}
