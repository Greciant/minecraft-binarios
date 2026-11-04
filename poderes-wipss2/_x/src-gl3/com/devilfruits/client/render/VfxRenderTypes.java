package com.devilfruits.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;

public final class VfxRenderTypes {
   public static final RenderType GLOW = RenderType.create(
      "devilfruits_glow",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.QUADS,
      65536,
      false,
      false,
      CompositeState.builder()
         .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
         .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
         .setCullState(RenderStateShard.NO_CULL)
         .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .createCompositeState(false)
   );
   public static final RenderType SHELL = RenderType.create(
      "devilfruits_shell",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.QUADS,
      65536,
      false,
      false,
      CompositeState.builder()
         .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
         .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
         .setCullState(RenderStateShard.NO_CULL)
         .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .createCompositeState(false)
   );
   public static final RenderType WATER = RenderType.create(
      "devilfruits_water",
      DefaultVertexFormat.BLOCK,
      Mode.QUADS,
      65536,
      false,
      true,
      CompositeState.builder()
         .setShaderState(RenderStateShard.RENDERTYPE_TRANSLUCENT_SHADER)
         .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
         .setLightmapState(RenderStateShard.LIGHTMAP)
         .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
         .setCullState(RenderStateShard.NO_CULL)
         .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .createCompositeState(false)
   );
   private static final Map<ResourceLocation, RenderType> GLOW_TEXTURED = new HashMap<>();
   private static final Map<ResourceLocation, RenderType> SHADE_TEXTURED = new HashMap<>();

   public static RenderType shadeTextured(ResourceLocation texture) {
      return SHADE_TEXTURED.computeIfAbsent(texture, VfxRenderTypes.Shards::shadeTextured);
   }

   public static RenderType glowTextured(ResourceLocation texture) {
      return GLOW_TEXTURED.computeIfAbsent(texture, VfxRenderTypes.Shards::glowTextured);
   }

   private VfxRenderTypes() {
   }

   private abstract static class Shards extends RenderType {
      private static final ShaderStateShard POSITION_TEX_COLOR = new ShaderStateShard(GameRenderer::getPositionTexColorShader);

      private Shards() {
         super("", DefaultVertexFormat.POSITION, Mode.QUADS, 0, false, false, () -> {
         }, () -> {
         });
      }

      static RenderType glowTextured(ResourceLocation texture) {
         return RenderType.create(
            "devilfruits_glow_textured",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            Mode.QUADS,
            16384,
            false,
            false,
            CompositeState.builder()
               .setShaderState(POSITION_TEX_COLOR)
               .setTextureState(new TextureStateShard(texture, false, false))
               .setTransparencyState(LIGHTNING_TRANSPARENCY)
               .setCullState(NO_CULL)
               .setDepthTestState(LEQUAL_DEPTH_TEST)
               .setWriteMaskState(COLOR_WRITE)
               .createCompositeState(false)
         );
      }

      static RenderType shadeTextured(ResourceLocation texture) {
         return RenderType.create(
            "devilfruits_shade_textured",
            DefaultVertexFormat.POSITION_TEX_COLOR,
            Mode.QUADS,
            16384,
            false,
            true,
            CompositeState.builder()
               .setShaderState(POSITION_TEX_COLOR)
               .setTextureState(new TextureStateShard(texture, false, false))
               .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
               .setCullState(NO_CULL)
               .setDepthTestState(LEQUAL_DEPTH_TEST)
               .setWriteMaskState(COLOR_WRITE)
               .createCompositeState(false)
         );
      }
   }
}
