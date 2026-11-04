package com.devilfruits.fruits.kage_kage.client.vfx;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.OutputStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.WriteMaskStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class SilhouetteRim {
   private static final Map<ResourceLocation, RenderType> MASKS = new HashMap<>();
   private static RenderType outline;

   public static void enableStencil() {
      RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
      if (!main.isStencilEnabled()) {
         main.enableStencil();
      }
   }

   static boolean ready() {
      return Minecraft.getInstance().getMainRenderTarget().isStencilEnabled();
   }

   static RenderType mask(ResourceLocation texture) {
      return MASKS.computeIfAbsent(texture, SilhouetteRim.Shards::stencilMask);
   }

   static RenderType outline() {
      if (outline == null) {
         outline = SilhouetteRim.Shards.hull(KageLook.white());
      }

      return outline;
   }

   private SilhouetteRim() {
   }

   private abstract static class Shards extends RenderType {
      private Shards() {
         super("", DefaultVertexFormat.POSITION, Mode.QUADS, 0, false, false, () -> {
         }, () -> {
         });
      }

      static RenderType stencilMask(ResourceLocation texture) {
         OutputStateShard mark = new OutputStateShard("devilfruits_kage_stencil_mark", () -> {
            GL11.glEnable(2960);
            RenderSystem.stencilMask(255);
            RenderSystem.clearStencil(0);
            RenderSystem.clear(1024, Minecraft.ON_OSX);
            RenderSystem.stencilFunc(519, 1, 255);
            RenderSystem.stencilOp(7680, 7680, 7681);
         }, () -> GL11.glDisable(2960));
         return RenderType.create(
            "devilfruits_kage_silhouette_mask",
            DefaultVertexFormat.NEW_ENTITY,
            Mode.QUADS,
            16384,
            false,
            false,
            CompositeState.builder()
               .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
               .setTextureState(new TextureStateShard(texture, false, false))
               .setTransparencyState(NO_TRANSPARENCY)
               .setCullState(NO_CULL)
               .setLightmapState(LIGHTMAP)
               .setOverlayState(OVERLAY)
               .setWriteMaskState(new WriteMaskStateShard(false, false))
               .setOutputState(mark)
               .createCompositeState(false)
         );
      }

      static RenderType hull(ResourceLocation white) {
         OutputStateShard outside = new OutputStateShard("devilfruits_kage_stencil_outside", () -> {
            GL11.glEnable(2960);
            RenderSystem.stencilMask(0);
            RenderSystem.stencilFunc(517, 1, 255);
            RenderSystem.stencilOp(7680, 7680, 7680);
         }, () -> {
            RenderSystem.stencilMask(255);
            GL11.glDisable(2960);
         });
         return RenderType.create(
            "devilfruits_kage_silhouette_rim",
            DefaultVertexFormat.NEW_ENTITY,
            Mode.QUADS,
            16384,
            false,
            false,
            CompositeState.builder()
               .setShaderState(RENDERTYPE_EYES_SHADER)
               .setTextureState(new TextureStateShard(white, false, false))
               .setTransparencyState(ADDITIVE_TRANSPARENCY)
               .setWriteMaskState(COLOR_WRITE)
               .setOutputState(outside)
               .createCompositeState(false)
         );
      }
   }
}
