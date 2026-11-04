package com.devilfruits.fruits.supa_supa.client.blade;

import com.devilfruits.DevilFruits;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderStateShard.TexturingStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class BladeRenderTypes {
   public static final ResourceLocation SHEEN = DevilFruits.id("textures/entity/supa_supa/blade_sheen.png");
   private static RenderType sheen;

   public static RenderType sheen() {
      if (sheen == null) {
         sheen = BladeRenderTypes.Shards.sheen();
      }

      return sheen;
   }

   private BladeRenderTypes() {
   }

   private abstract static class Shards extends RenderType {
      private Shards() {
         super("", DefaultVertexFormat.POSITION, Mode.QUADS, 0, false, false, () -> {
         }, () -> {
         });
      }

      static RenderType sheen() {
         TexturingStateShard scroll = new TexturingStateShard("devilfruits_supa_sheen", () -> {
            float t = (float)(Util.getMillis() % 2600L) / 2600.0F;
            Matrix4f m = new Matrix4f().translation(t * 1.6F - 0.3F, t * 0.4F, 0.0F).scale(0.6F);
            RenderSystem.setTextureMatrix(m);
         }, RenderSystem::resetTextureMatrix);
         return RenderType.create(
            "devilfruits_supa_sheen",
            DefaultVertexFormat.POSITION_TEX,
            Mode.QUADS,
            4096,
            false,
            false,
            CompositeState.builder()
               .setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
               .setTextureState(new TextureStateShard(BladeRenderTypes.SHEEN, true, false))
               .setWriteMaskState(COLOR_WRITE)
               .setCullState(NO_CULL)
               .setDepthTestState(EQUAL_DEPTH_TEST)
               .setTransparencyState(GLINT_TRANSPARENCY)
               .setTexturingState(scroll)
               .createCompositeState(false)
         );
      }
   }
}
