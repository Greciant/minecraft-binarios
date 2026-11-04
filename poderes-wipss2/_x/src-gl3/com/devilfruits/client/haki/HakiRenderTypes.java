package com.devilfruits.client.haki;

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

public final class HakiRenderTypes {
   public static final ResourceLocation COAT_FULL = texture("coat_full");
   public static final ResourceLocation COAT_FIST = texture("coat_fist");
   public static final ResourceLocation COAT_TILE = texture("coat_tile");
   public static final ResourceLocation OUTLINE_FULL = texture("outline_full");
   public static final ResourceLocation OUTLINE_FIST = texture("outline_fist");
   public static final ResourceLocation OUTLINE_TILE = texture("outline_tile");
   public static final ResourceLocation SHEEN = texture("sheen");
   private static RenderType sheen;
   private static RenderType itemSheen;

   private static ResourceLocation texture(String name) {
      return DevilFruits.id("textures/effect/haki/" + name + ".png");
   }

   public static RenderType shell(ResourceLocation texture) {
      return RenderType.armorCutoutNoCull(texture);
   }

   public static RenderType outline(ResourceLocation texture, boolean cull) {
      return cull ? RenderType.entityCutout(texture) : RenderType.entityCutoutNoCull(texture);
   }

   public static RenderType sheen() {
      if (sheen == null) {
         sheen = HakiRenderTypes.Shards.sheen("devilfruits_haki_sheen", true);
      }

      return sheen;
   }

   public static RenderType itemSheen() {
      if (itemSheen == null) {
         itemSheen = HakiRenderTypes.Shards.sheen("devilfruits_haki_item_sheen", false);
      }

      return itemSheen;
   }

   private HakiRenderTypes() {
   }

   private abstract static class Shards extends RenderType {
      private Shards() {
         super("", DefaultVertexFormat.POSITION, Mode.QUADS, 0, false, false, () -> {
         }, () -> {
         });
      }

      static RenderType sheen(String name, boolean layered) {
         TexturingStateShard scroll = new TexturingStateShard("devilfruits_haki_sheen", () -> {
            long step = Util.getMillis() / 90L;
            float offset = (float)(step % 64L) / 64.0F;
            Matrix4f m = new Matrix4f().translation(offset, -offset * 0.5F, 0.0F).scale(0.5F);
            RenderSystem.setTextureMatrix(m);
         }, RenderSystem::resetTextureMatrix);
         return RenderType.create(
            name,
            DefaultVertexFormat.POSITION_TEX,
            Mode.QUADS,
            4096,
            false,
            false,
            CompositeState.builder()
               .setLayeringState(layered ? VIEW_OFFSET_Z_LAYERING : NO_LAYERING)
               .setShaderState(RENDERTYPE_ENTITY_GLINT_SHADER)
               .setTextureState(new TextureStateShard(HakiRenderTypes.SHEEN, false, false))
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
