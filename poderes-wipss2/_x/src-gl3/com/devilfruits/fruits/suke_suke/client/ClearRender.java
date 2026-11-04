package com.devilfruits.fruits.suke_suke.client;

import com.devilfruits.client.render.BodyModels;
import com.devilfruits.client.render.EntityShadows;
import com.devilfruits.client.render.FormRenderers;
import com.devilfruits.client.vfx.VfxSettings;
import com.devilfruits.fruits.suke_suke.SukeSuke;
import com.devilfruits.fruits.suke_suke.clear.Clear;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ClearRender {
   public static final int TINT = 12578784;
   public static final float SELF_ALPHA = 0.32F;
   public static final float ALLY_ALPHA = 0.24F;
   public static final float ITEM_ALPHA = 0.3F;
   private static boolean rerendering;
   private static boolean itemRerendering;
   private static final float LEGS = 0.32F;
   private static final float TORSO = 0.42F;
   private static final float ARMS = 0.48F;
   private static final float HEAD = 0.56F;
   private static final float GLASS_PEAK = 0.55F;

   static void init() {
      BodyModels.register(ClearRender::body);
      EntityShadows.hideWhen(e -> ClearTransitions.phase(e, 0.0F) > 0.5F && ClearTransitions.relation(e) != Visibility.Mode.SPECTATOR);
      Clear.hiddenHere = Visibility::hidden;
      Clear.itemsHiddenHere = Visibility::itemsHidden;
   }

   public static float alpha(Visibility.Mode mode) {
      return mode == Visibility.Mode.SELF ? 0.32F : 0.24F;
   }

   public static int tint(float phase) {
      float k = ClearTransitions.smooth(phase * 3.0F);
      int r = Math.round(Mth.lerp(k, 255.0F, 191.0F));
      int g = Math.round(Mth.lerp(k, 255.0F, 239.0F));
      int b = Math.round(Mth.lerp(k, 255.0F, 224.0F));
      return r << 16 | g << 8 | b;
   }

   public static float layerAlpha(float phase, Visibility.Mode relation) {
      float floor = relation.glass() ? alpha(relation) : 0.0F;
      return Mth.lerp(ClearTransitions.smooth((phase - 0.15F) / 0.7F), 1.0F, floor);
   }

   public static boolean dispatch(
      EntityRenderDispatcher dispatcher,
      Entity target,
      double x,
      double y,
      double z,
      float yaw,
      float partial,
      PoseStack pose,
      MultiBufferSource buffers,
      int light
   ) {
      if (rerendering) {
         return false;
      } else if (ShipLook.entityHidden(target, partial)) {
         return true;
      } else {
         float phase = ClearTransitions.phase(target, partial);
         if (phase < 0.0F) {
            return false;
         } else {
            Visibility.Mode relation = ClearTransitions.relation(target);
            if (relation == Visibility.Mode.SPECTATOR) {
               return false;
            } else if (phase >= 1.0F && relation == Visibility.Mode.HIDDEN) {
               if (!SukeSuke.flag("hideNameTag") && target instanceof AbstractClientPlayer player) {
                  pose.pushPose();
                  pose.translate(x, y, z);
                  FormRenderers.renderNameTag(player, partial, pose, buffers, light);
                  pose.popPose();
               }

               return true;
            } else {
               rerendering = true;
               Clear.drawnSolid = target;

               try {
                  dispatcher.render(target, x, y, z, yaw, partial, pose, GlassBuffers.of(buffers, tint(phase), layerAlpha(phase, relation)), light);
               } finally {
                  Clear.drawnSolid = null;
                  rerendering = false;
               }

               return true;
            }
         }
      }
   }

   private static boolean body(AbstractClientPlayer player, PlayerModel<?> posed, PoseStack pose, MultiBufferSource buffers, int light, int overlay, int color) {
      if (!(buffers instanceof GlassBuffers)) {
         return false;
      } else {
         float phase = Math.max(0.0F, ClearTransitions.phase(player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)));
         Visibility.Mode relation = ClearTransitions.relation(player);
         float floor = relation.glass() ? alpha(relation) : 0.0F;
         float skin = 1.0F - ClearTransitions.smooth(phase / 0.3F);
         float glass = ClearTransitions.smooth(phase / 0.25F);
         float legs = limb(phase, 0.32F, floor);
         float torso = limb(phase, 0.42F, floor);
         float arms = limb(phase, 0.48F, floor);
         float head = limb(phase, 0.56F, floor);
         ResourceLocation texture = player.getSkin().texture();
         MultiBufferSource out = GlassBuffers.unwrap(buffers);
         ModelPart[][] parts = new ModelPart[][]{
            {posed.rightLeg, posed.leftLeg, posed.rightPants, posed.leftPants},
            {posed.body, posed.jacket},
            {posed.rightArm, posed.leftArm, posed.rightSleeve, posed.leftSleeve},
            {posed.head, posed.hat}
         };
         float[] away = new float[]{legs, torso, arms, head};
         boolean legacy = VfxSettings.legacy();
         if (!legacy && out instanceof BufferSource source) {
            RenderType depth = ClearRender.Types.depth(texture);
            VertexConsumer dc = source.getBuffer(depth);

            for (int i = 0; i < parts.length; i++) {
               if (Math.max(skin, glass * away[i]) > 0.02F) {
                  for (ModelPart part : parts[i]) {
                     part.render(pose, dc, light, overlay);
                  }
               }
            }

            source.endBatch(depth);
         }

         RenderType colour = legacy ? RenderType.entityTranslucent(texture) : ClearRender.Types.glass(texture);
         int tint = tint(phase);

         for (int ix = 0; ix < parts.length; ix++) {
            float a = Math.max(skin, glass * away[ix]);
            if (!(a <= 0.004F)) {
               int argb = Math.round(Mth.clamp(a, 0.0F, 1.0F) * 255.0F) << 24 | tint;

               for (ModelPart part : parts[ix]) {
                  part.render(pose, out.getBuffer(colour), light, overlay, argb);
               }
            }
         }

         return true;
      }
   }

   private static float limb(float phase, float start, float floor) {
      return Mth.lerp(ClearTransitions.smooth((phase - start) / 0.3F), 0.55F, floor);
   }

   public static boolean arm(PlayerRenderer renderer, AbstractClientPlayer player, HumanoidArm side, PoseStack pose, MultiBufferSource buffers, int light) {
      if (Clear.invisible(player) && Visibility.of(player) == Visibility.Mode.SELF) {
         float phase = Math.max(0.0F, ClearTransitions.phase(player, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)));
         PlayerModel<AbstractClientPlayer> model = (PlayerModel<AbstractClientPlayer>)renderer.getModel();
         model.attackTime = 0.0F;
         model.crouching = false;
         model.swimAmount = 0.0F;
         model.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
         ModelPart arm = side == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
         ModelPart sleeve = side == HumanoidArm.RIGHT ? model.rightSleeve : model.leftSleeve;
         arm.xRot = 0.0F;
         sleeve.xRot = 0.0F;
         sleeve.visible = player.isModelPartShown(side == HumanoidArm.RIGHT ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE);
         int argb = Math.round(layerAlpha(phase, Visibility.Mode.SELF) * 255.0F) << 24 | tint(phase);
         RenderType type = RenderType.entityTranslucent(player.getSkin().texture());
         arm.render(pose, buffers.getBuffer(type), light, OverlayTexture.NO_OVERLAY, argb);
         sleeve.render(pose, buffers.getBuffer(type), light, OverlayTexture.NO_OVERLAY, argb);
         return true;
      } else {
         return false;
      }
   }

   public static boolean armHidden(AbstractClientPlayer player, boolean vanilla) {
      return vanilla && (!Clear.invisible(player) || Visibility.of(player) != Visibility.Mode.SELF);
   }

   public static boolean item(
      ItemInHandRenderer renderer,
      LivingEntity entity,
      ItemStack stack,
      ItemDisplayContext context,
      boolean leftHand,
      PoseStack pose,
      MultiBufferSource buffers,
      int light
   ) {
      if (itemRerendering) {
         return false;
      } else if (Visibility.itemsHidden(entity)) {
         return true;
      } else if (context.firstPerson() && Clear.invisible(entity) && Visibility.of(entity) == Visibility.Mode.SELF) {
         itemRerendering = true;

         try {
            float phase = Math.max(0.0F, ClearTransitions.phase(entity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)));
            renderer.renderItem(
               entity, stack, context, leftHand, pose, GlassBuffers.of(buffers, tint(phase), Mth.lerp(ClearTransitions.smooth(phase), 1.0F, 0.3F)), light
            );
         } finally {
            itemRerendering = false;
         }

         return true;
      } else {
         return false;
      }
   }

   private ClearRender() {
   }

   static final class Types extends RenderType {
      private static final Map<ResourceLocation, RenderType> DEPTH = new HashMap<>();
      private static final Map<ResourceLocation, RenderType> GLASS = new HashMap<>();

      private Types() {
         super("", DefaultVertexFormat.POSITION, Mode.QUADS, 0, false, false, () -> {
         }, () -> {
         });
      }

      static RenderType depth(ResourceLocation texture) {
         return DEPTH.computeIfAbsent(
            texture,
            t -> RenderType.create(
                  "devilfruits_suke_glass_depth",
                  DefaultVertexFormat.NEW_ENTITY,
                  Mode.QUADS,
                  16384,
                  false,
                  false,
                  CompositeState.builder()
                     .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                     .setTextureState(new TextureStateShard(t, false, false))
                     .setTransparencyState(NO_TRANSPARENCY)
                     .setCullState(CULL)
                     .setLightmapState(LIGHTMAP)
                     .setOverlayState(OVERLAY)
                     .setWriteMaskState(DEPTH_WRITE)
                     .createCompositeState(false)
               )
         );
      }

      static RenderType glass(ResourceLocation texture) {
         return GLASS.computeIfAbsent(
            texture,
            t -> RenderType.create(
                  "devilfruits_suke_glass",
                  DefaultVertexFormat.NEW_ENTITY,
                  Mode.QUADS,
                  16384,
                  false,
                  true,
                  CompositeState.builder()
                     .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                     .setTextureState(new TextureStateShard(t, false, false))
                     .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                     .setCullState(CULL)
                     .setLightmapState(LIGHTMAP)
                     .setOverlayState(OVERLAY)
                     .setWriteMaskState(COLOR_WRITE)
                     .createCompositeState(true)
               )
         );
      }
   }
}
