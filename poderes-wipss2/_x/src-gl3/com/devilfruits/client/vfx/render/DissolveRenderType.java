package com.devilfruits.client.vfx.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;

public final class DissolveRenderType {
   private static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();

   @Nullable
   public static RenderType of(ResourceLocation texture) {
      return VfxShaders.dissolve == null ? null : TYPES.computeIfAbsent(texture, DissolveRenderType.Shards::create);
   }

   public static int colour(float progress) {
      return Math.max(1, Math.min(255, (int)(progress * 255.0F))) << 24 | 16777215;
   }

   private DissolveRenderType() {
   }

   private abstract static class Shards extends RenderType {
      private Shards() {
         super("", DefaultVertexFormat.POSITION, Mode.QUADS, 0, false, false, () -> {
         }, () -> {
         });
      }

      static RenderType create(ResourceLocation texture) {
         return RenderType.create(
            "devilfruits_dissolve",
            DefaultVertexFormat.NEW_ENTITY,
            Mode.QUADS,
            1536,
            true,
            false,
            CompositeState.builder()
               .setShaderState(new ShaderStateShard(() -> VfxShaders.dissolve))
               .setTextureState(new TextureStateShard(texture, false, false))
               .setTransparencyState(NO_TRANSPARENCY)
               .setCullState(NO_CULL)
               .setLightmapState(LIGHTMAP)
               .setOverlayState(OVERLAY)
               .createCompositeState(true)
         );
      }
   }
}
