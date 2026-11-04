package com.devilfruits.client.haki;

import com.devilfruits.client.model.geo.GeoModel;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.core.haki.HakiFormParts;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class HakiCoatRenderer {
   public static final float SHELL = 0.0F;
   public static final float OUTLINE = 0.025F;
   private static final float ITEM_RIM = 0.5F;
   private static final int WHITE = 15921906;
   private static final CoatGeometry GEOMETRY = new CoatGeometry();
   private static final Map<GeoModel, Int2ObjectOpenHashMap<int[]>> BONES = new IdentityHashMap<>();
   private static final VertexConsumer NONE = new VertexConsumer() {
      public VertexConsumer addVertex(float x, float y, float z) {
         return this;
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         return this;
      }
   };

   public static CoatGeometry begin() {
      GEOMETRY.reset();
      return GEOMETRY;
   }

   public static HakiClientConfig.Quality quality() {
      return (HakiClientConfig.Quality)HakiClientConfig.QUALITY.get();
   }

   private static int alpha(float progress) {
      return (int)(Mth.clamp(progress, 0.1F, 1.0F) * 255.0F) << 24;
   }

   public static void passes(
      MultiBufferSource buffers,
      CoatGeometry geometry,
      ResourceLocation shell,
      @Nullable ResourceLocation outline,
      float progress,
      int light,
      float shellInflate,
      float outlineInflate
   ) {
      if (!geometry.isEmpty() && !(progress <= 0.0F)) {
         int alpha = alpha(progress);
         geometry.emit(buffers.getBuffer(HakiRenderTypes.shell(shell)), shellInflate, false, alpha | 16777215, light, OverlayTexture.NO_OVERLAY, false);
         if (quality() != HakiClientConfig.Quality.LOW && progress >= 0.99F) {
            geometry.emit(buffers.getBuffer(HakiRenderTypes.sheen()), shellInflate, false, -1, light, OverlayTexture.NO_OVERLAY, false);
         }

         if (outline != null) {
            rim(buffers, geometry, outline, alpha, outlineInflate);
         }
      }
   }

   private static void rim(MultiBufferSource buffers, CoatGeometry geometry, ResourceLocation outline, int alpha, float inflate) {
      if (!geometry.isEmpty()) {
         geometry.emit(buffers.getBuffer(HakiRenderTypes.outline(outline, true)), inflate, true, alpha | 15921906, 15728880, OverlayTexture.NO_OVERLAY, true);
      }
   }

   public static void humanoid(
      AbstractClientPlayer player,
      HumanoidModel<?> model,
      int mask,
      int contact,
      float progress,
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      int overlay
   ) {
      if (mask != 0 && progress > 0.0F) {
         ResourceLocation coat = HakiSkinCoat.texture(player, mask);
         int alpha = alpha(progress);
         if (coat != null) {
            model.renderToBuffer(pose, buffers.getBuffer(HakiRenderTypes.shell(coat)), light, overlay, alpha | 16777215);
            if (quality() != HakiClientConfig.Quality.LOW && progress >= 0.99F) {
               model.renderToBuffer(pose, buffers.getBuffer(HakiRenderTypes.sheen()), light, overlay, -1);
            }
         }

         CoatGeometry g = begin();
         part(g, model.head, mask, 128, pose, light);
         part(g, model.body, mask, 64, pose, light);
         part(g, model.rightArm, mask, 4, pose, light);
         part(g, model.leftArm, mask, 8, pose, light);
         part(g, model.rightLeg, mask, 16, pose, light);
         part(g, model.leftLeg, mask, 32, pose, light);
         if (coat == null) {
            passes(buffers, g, HakiRenderTypes.COAT_FULL, HakiRenderTypes.OUTLINE_FULL, progress, light, 0.0F, 0.025F);
         } else {
            rim(buffers, g, HakiRenderTypes.OUTLINE_FULL, alpha, 0.025F);
         }

         g = begin();
         fist(g, model.rightArm, mask, 4, 1, pose, light);
         fist(g, model.leftArm, mask, 8, 2, pose, light);
         if (coat == null) {
            passes(buffers, g, HakiRenderTypes.COAT_FIST, HakiRenderTypes.OUTLINE_FIST, progress, light, 0.0F, 0.025F);
         } else {
            rim(buffers, g, HakiRenderTypes.OUTLINE_FIST, alpha, 0.025F);
         }
      }

      if (contact != 0) {
         CoatGeometry gx = begin();
         part(gx, model.head, contact, 128, pose, light);
         part(gx, model.body, contact, 64, pose, light);
         part(gx, model.rightArm, contact, 4, pose, light);
         part(gx, model.leftArm, contact, 8, pose, light);
         part(gx, model.rightLeg, contact, 16, pose, light);
         part(gx, model.leftLeg, contact, 32, pose, light);
         rim(buffers, gx, HakiRenderTypes.OUTLINE_FULL, -16777216, 0.025F);
      }
   }

   private static void part(CoatGeometry g, ModelPart part, int mask, int bit, PoseStack pose, int light) {
      if ((mask & bit) != 0 && part.visible) {
         part.render(pose, g, light, OverlayTexture.NO_OVERLAY);
      }
   }

   private static void fist(CoatGeometry g, ModelPart arm, int mask, int armBit, int fistBit, PoseStack pose, int light) {
      if ((mask & armBit) == 0 && (mask & fistBit) != 0 && arm.visible) {
         arm.render(pose, g, light, OverlayTexture.NO_OVERLAY);
      }
   }

   public static void firstPersonArm(
      AbstractClientPlayer player, ModelPart arm, ModelPart sleeve, boolean right, PoseStack pose, MultiBufferSource buffers, int light
   ) {
      int mask = HakiClientState.shown(player);
      if (mask != 0 && !HakiClientState.blinkedOut(player)) {
         float progress = HakiClientState.progress(player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
         boolean whole = (mask & (right ? 4 : 8)) != 0;
         boolean hand = (mask & (right ? 1 : 2)) != 0;
         if ((whole || hand) && !(progress <= 0.0F)) {
            ResourceLocation outline = whole ? HakiRenderTypes.OUTLINE_FULL : HakiRenderTypes.OUTLINE_FIST;
            CoatGeometry g = begin();
            arm.render(pose, g, light, OverlayTexture.NO_OVERLAY);
            ResourceLocation coat = HakiSkinCoat.texture(player, mask);
            if (coat == null) {
               passes(buffers, g, whole ? HakiRenderTypes.COAT_FULL : HakiRenderTypes.COAT_FIST, outline, progress, light, 0.0F, 0.02125F);
            } else {
               int alpha = alpha(progress);
               VertexConsumer shell = buffers.getBuffer(HakiRenderTypes.shell(coat));
               arm.render(pose, shell, light, OverlayTexture.NO_OVERLAY, alpha | 16777215);
               sleeve.render(pose, shell, light, OverlayTexture.NO_OVERLAY, alpha | 16777215);
               if (quality() != HakiClientConfig.Quality.LOW && progress >= 0.99F) {
                  VertexConsumer sheen = buffers.getBuffer(HakiRenderTypes.sheen());
                  arm.render(pose, sheen, light, OverlayTexture.NO_OVERLAY);
                  sleeve.render(pose, sheen, light, OverlayTexture.NO_OVERLAY);
               }

               rim(buffers, g, outline, alpha, 0.02125F);
            }
         }
      }
   }

   public static boolean item(
      LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean left, PoseStack pose, MultiBufferSource buffers, int light
   ) {
      if (!(entity instanceof Player player) || stack.isEmpty() || player.getMainHandItem() != stack) {
         return false;
      }

      if ((HakiClientState.shown(player) & 256) != 0 && !HakiClientState.blinkedOut(player)) {
         float progress = HakiClientState.progress(player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
         if (progress < 0.5F) {
            return false;
         } else {
            ItemRenderer items = Minecraft.getInstance().getItemRenderer();
            int seed = entity.getId() + context.ordinal();
            items.renderStatic(
               entity,
               stack,
               context,
               left,
               pose,
               type -> (VertexConsumer)(glint(type) ? NONE : new HakiCoatRenderer.Tinted(buffers.getBuffer(type))),
               entity.level(),
               light,
               OverlayTexture.NO_OVERLAY,
               seed
            );
            if (quality() != HakiClientConfig.Quality.LOW) {
               VertexConsumer sheen = buffers.getBuffer(HakiRenderTypes.itemSheen());
               items.renderStatic(
                  entity,
                  stack,
                  context,
                  left,
                  pose,
                  type -> (VertexConsumer)(glint(type) ? NONE : new HakiCoatRenderer.Tinted(sheen, false)),
                  entity.level(),
                  light,
                  OverlayTexture.NO_OVERLAY,
                  seed
               );
            }

            BakedModel model = items.getModel(stack, entity.level(), entity, seed);
            if (!model.isGui3d() && !model.isCustomRenderer()) {
               CoatGeometry g = begin();
               items.renderStatic(
                  entity, stack, context, left, pose, type -> (VertexConsumer)(glint(type) ? NONE : g), entity.level(), light, OverlayTexture.NO_OVERLAY, seed
               );
               TextureAtlasSprite sprite = model.getParticleIcon();
               g.emitSpriteRim(
                  buffers.getBuffer(HakiRenderTypes.outline(HakiRenderTypes.OUTLINE_TILE, false)), sprite, 0.5F, -855310, 15728880, OverlayTexture.NO_OVERLAY
               );
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private static boolean glint(RenderType type) {
      return type == RenderType.glint()
         || type == RenderType.glintTranslucent()
         || type == RenderType.entityGlint()
         || type == RenderType.entityGlintDirect()
         || type == RenderType.armorEntityGlint();
   }

   public static void geo(CoatGeometry g, GeoRenderer renderer, GeoPose pose, ResourceLocation fruit, String form, int mask, int light) {
      GeoModel model = renderer.model();

      for (int bone : bones(model, fruit, form, mask)) {
         if (!pose.hidden[bone]) {
            renderer.renderBone(g, model.bones[bone], light, OverlayTexture.NO_OVERLAY, -1);
         }
      }
   }

   static int[] bones(GeoModel model, ResourceLocation fruit, String form, int mask) {
      return (int[])BONES.computeIfAbsent(model, k -> new Int2ObjectOpenHashMap()).computeIfAbsent(mask, k -> {
         List<String> names = HakiFormParts.parts(fruit, form, mask);
         return names.stream().mapToInt(model::bone).filter(i -> i >= 0).toArray();
      });
   }

   private HakiCoatRenderer() {
   }

   private static record Tinted(VertexConsumer inner, boolean dark) implements VertexConsumer {
      Tinted(VertexConsumer inner) {
         this(inner, true);
      }

      public VertexConsumer addVertex(float x, float y, float z) {
         this.inner.addVertex(x, y, z);
         return this;
      }

      public VertexConsumer setColor(int r, int g, int b, int a) {
         if (this.dark) {
            this.inner.setColor(r * 44 / 255, g * 44 / 255, b * 56 / 255, a);
         } else {
            this.inner.setColor(r, g, b, a);
         }

         return this;
      }

      public VertexConsumer setUv(float u, float v) {
         this.inner.setUv(u, v);
         return this;
      }

      public VertexConsumer setUv1(int u, int v) {
         this.inner.setUv1(u, v);
         return this;
      }

      public VertexConsumer setUv2(int u, int v) {
         this.inner.setUv2(u, v);
         return this;
      }

      public VertexConsumer setNormal(float x, float y, float z) {
         this.inner.setNormal(x, y, z);
         return this;
      }
   }
}
