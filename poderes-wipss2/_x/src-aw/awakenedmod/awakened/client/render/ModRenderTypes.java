package com.awakenedmod.awakened.client.render;

import com.awakenedmod.awakened.core.AwId;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.CullStateShard;
import net.minecraft.client.renderer.RenderStateShard.DepthTestStateShard;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard;
import net.minecraft.client.renderer.RenderStateShard.WriteMaskStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class ModRenderTypes {
   private static final DepthTestStateShard NO_DEPTH_TEST = new ModRenderTypes.NoDepthTest();
   static final RenderType DARK_TRIANGLES = RenderType.create(
      "awakened:dark_triangles",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      1536,
      CompositeState.builder()
         .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
         .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
         .setCullState(RenderStateShard.NO_CULL)
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
         .createCompositeState(false)
   );
   static final RenderType ADDITIVE_NO_DEPTH = RenderType.create(
      "awakened:additive_no_depth",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      1536,
      false,
      false,
      CompositeState.builder()
         .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
         .setDepthTestState(NO_DEPTH_TEST)
         .createCompositeState(false)
   );
   static final RenderType DARK_TRIANGLES_DEPTH = RenderType.create(
      "awakened:dark_triangles_depth",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      1536,
      CompositeState.builder()
         .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
         .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
         .setCullState(RenderStateShard.NO_CULL)
         .setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
         .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
         .createCompositeState(false)
   );
   static final RenderType DARK_TRIANGLES_NO_DEPTH = RenderType.create(
      "awakened:dark_triangles_no_depth",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      1536,
      CompositeState.builder()
         .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
         .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
         .setCullState(RenderStateShard.NO_CULL)
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .setDepthTestState(NO_DEPTH_TEST)
         .createCompositeState(false)
   );
   public static final RenderType WATER_TRANSLUCENT = RenderType.create(
      "awakened:water_translucent",
      DefaultVertexFormat.NEW_ENTITY,
      Mode.QUADS,
      1536,
      true,
      true,
      CompositeState.builder()
         .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
         .setTextureState(new TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
         .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
         .setCullState(RenderStateShard.NO_CULL)
         .setLightmapState(RenderStateShard.LIGHTMAP)
         .setOverlayState(RenderStateShard.OVERLAY)
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .createCompositeState(false)
   );
   public static final RenderType WATER_FOAM = RenderType.create(
      "awakened:water_foam",
      DefaultVertexFormat.NEW_ENTITY,
      Mode.QUADS,
      1536,
      true,
      false,
      CompositeState.builder()
         .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
         .setTextureState(new TextureStateShard(TextureAtlas.LOCATION_BLOCKS, false, false))
         .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
         .setCullState(RenderStateShard.NO_CULL)
         .setLightmapState(RenderStateShard.LIGHTMAP)
         .setOverlayState(RenderStateShard.OVERLAY)
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .createCompositeState(false)
   );
   private static final TransparencyStateShard INVERT_TRANSPARENCY = new TransparencyStateShard("awakened_invert_transparency", () -> {
      RenderSystem.enableBlend();
      RenderSystem.blendFuncSeparate(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR, SourceFactor.ONE, DestFactor.ZERO);
   }, () -> {
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
   });
   static final RenderType INVERT_TRIANGLES = invertType("awakened:invert_triangles", Mode.TRIANGLES, RenderStateShard.CULL, RenderStateShard.LEQUAL_DEPTH_TEST);
   static final RenderType INVERT_TRIANGLES_NO_DEPTH = invertType(
      "awakened:invert_triangles_no_depth", Mode.TRIANGLES, RenderStateShard.CULL, RenderStateShard.NO_DEPTH_TEST
   );
   static final RenderType INVERT_MODEL = invertType("awakened:invert_model", Mode.QUADS, RenderStateShard.NO_CULL, RenderStateShard.EQUAL_DEPTH_TEST);
   private static final ResourceLocation WHITE = AwId.of("textures/misc/white.png");
   static final RenderType PACK_LIGHT = particles("awakened:pack_light", RenderStateShard.LIGHTNING_TRANSPARENCY, RenderStateShard.CULL);
   static final RenderType PACK_PAINT = particles("awakened:pack_paint", RenderStateShard.TRANSLUCENT_TRANSPARENCY, RenderStateShard.NO_CULL);
   static final RenderType PACK_SOLID = particles(
      "awakened:pack_solid",
      RenderStateShard.TRANSLUCENT_TRANSPARENCY,
      RenderStateShard.NO_CULL,
      RenderStateShard.COLOR_DEPTH_WRITE,
      RenderStateShard.LEQUAL_DEPTH_TEST
   );
   static final RenderType PACK_LIGHT_NO_DEPTH = particles(
      "awakened:pack_light_no_depth", RenderStateShard.LIGHTNING_TRANSPARENCY, RenderStateShard.CULL, RenderStateShard.COLOR_WRITE, NO_DEPTH_TEST
   );
   static final RenderType PACK_PAINT_NO_DEPTH = particles(
      "awakened:pack_paint_no_depth", RenderStateShard.TRANSLUCENT_TRANSPARENCY, RenderStateShard.NO_CULL, RenderStateShard.COLOR_WRITE, NO_DEPTH_TEST
   );
   static final RenderType PACK_DEPTH_STAMP = RenderType.create(
      "awakened:pack_depth_stamp",
      DefaultVertexFormat.POSITION_COLOR,
      Mode.TRIANGLES,
      1536,
      false,
      false,
      CompositeState.builder()
         .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
         .setWriteMaskState(RenderStateShard.DEPTH_WRITE)
         .setCullState(RenderStateShard.NO_CULL)
         .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
         .createCompositeState(false)
   );

   private ModRenderTypes() {
   }

   private static RenderType invertType(String name, Mode mode, CullStateShard cull, DepthTestStateShard depth) {
      return RenderType.create(
         name,
         DefaultVertexFormat.POSITION_COLOR,
         mode,
         1536,
         CompositeState.builder()
            .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
            .setTransparencyState(INVERT_TRANSPARENCY)
            .setCullState(cull)
            .setWriteMaskState(RenderStateShard.COLOR_WRITE)
            .setDepthTestState(depth)
            .createCompositeState(false)
      );
   }

   private static RenderType particles(String name, TransparencyStateShard blend, CullStateShard cull) {
      return particles(name, blend, cull, RenderStateShard.COLOR_WRITE, RenderStateShard.LEQUAL_DEPTH_TEST);
   }

   private static RenderType particles(String name, TransparencyStateShard blend, CullStateShard cull, WriteMaskStateShard write, DepthTestStateShard depth) {
      return RenderType.create(
         name,
         DefaultVertexFormat.PARTICLE,
         Mode.QUADS,
         1536,
         false,
         false,
         CompositeState.builder()
            .setShaderState(new ShaderStateShard(GameRenderer::getParticleShader))
            .setTextureState(new TextureStateShard(WHITE, false, false))
            .setTransparencyState(blend)
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setWriteMaskState(write)
            .setDepthTestState(depth)
            .setCullState(cull)
            .createCompositeState(false)
      );
   }

   private static final class NoDepthTest extends DepthTestStateShard {
      private boolean wasOn;

      NoDepthTest() {
         super("always", 519);
      }

      public void setupRenderState() {
         this.wasOn = GL11.glIsEnabled(2929);
         RenderSystem.disableDepthTest();
      }

      public void clearRenderState() {
         if (this.wasOn) {
            RenderSystem.enableDepthTest();
         }
      }
   }
}
