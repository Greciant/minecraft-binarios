package com.awakenedmod.awakened.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public final class BlockAtlas {
   private BlockAtlas() {
   }

   public static RenderType layer() {
      return RenderType.cutout();
   }

   public static TextureAtlasSprite sprite(ResourceLocation id) {
      return (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(id);
   }
}
