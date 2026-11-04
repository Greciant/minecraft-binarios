package com.awakenedmod.awakened.client.ruin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

public final class RuinDecayLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
   private static final float MODEL_FEET_Y = 1.501F;
   private static final DecayVertexSink SINK = new DecayVertexSink();
   private static final int KIND_CUTOUT = 0;
   private static final int KIND_TRANSLUCENT = 1;
   private static final int KIND_EMISSIVE = 2;
   private static final int GREY_FALLBACK_TINT = -6646385;
   private final LivingEntityRenderer<T, M> renderer;
   private static LivingEntity swallowedEntity;
   private static RenderType swallowedType;

   public RuinDecayLayer(LivingEntityRenderer<T, M> renderer) {
      super(renderer);
      this.renderer = renderer;
   }

   public static void attach(LivingEntityRenderer<?, ?> renderer) {
      renderer.addLayer(new RuinDecayLayer((LivingEntityRenderer<T, M>)renderer));
   }

   public static boolean hidesBody(LivingEntity entity) {
      if (!entity.isInvisible() && !firstPersonPass(entity)) {
         RuinLook.Look look = RuinDecayBodies.of(entity);
         return look != null && look.crumbling();
      } else {
         return false;
      }
   }

   public static RenderType swallowBody(LivingEntity entity, RenderType vanilla) {
      if (!hidesBody(entity)) {
         return vanilla;
      } else {
         swallowedEntity = entity;
         swallowedType = vanilla;
         return RenderType.entityTranslucent(RuinCrackSheets.emptyTexture());
      }
   }

   public static boolean crumbling(LivingEntity entity) {
      RuinLook.Look look = RuinDecayBodies.of(entity);
      return look != null && look.crumbling();
   }

   private static boolean firstPersonPass(LivingEntity entity) {
      return entity == Minecraft.getInstance().player && FirstPersonMode.isFirstPersonPass();
   }

   public void render(
      PoseStack poseStack,
      MultiBufferSource buffers,
      int packedLight,
      T entity,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float ageInTicks,
      float netHeadYaw,
      float headPitch
   ) {
      if (!entity.isInvisible() && !firstPersonPass(entity)) {
         RuinLook.Look look = RuinDecayBodies.of(entity);
         if (look != null) {
            ResourceLocation texture = this.renderer.getTextureLocation(entity);
            RuinCrackSheets.Sheet sheet = RuinCrackSheets.of(texture, entity instanceof AbstractClientPlayer);
            float feetY = poseStack.last().pose().transformPosition(0.0F, 1.501F, 0.0F, new Vector3f()).y;
            RuinDecayLayer.BodyDraw body = swallowedEntity == entity ? new RuinDecayLayer.BodyDraw(swallowedType, -1) : null;
            swallowedEntity = null;
            swallowedType = null;
            List<RuinDecayLayer.Draw> draws = plan(look, sheet, texture, feetY, entity.getBbHeight(), body);
            M model = (M)this.getParentModel();

            for (RuinDecayLayer.Draw draw : draws) {
               VertexConsumer vc = buffers.getBuffer(renderType(draw));
               SINK.bind(vc, draw.pass());
               model.renderToBuffer(poseStack, SINK, packedLight, OverlayTexture.NO_OVERLAY, draw.tint());
            }
         }
      }
   }

   private static RenderType renderType(RuinDecayLayer.Draw draw) {
      if (draw.type() != null) {
         return draw.type();
      } else {
         return switch (draw.kind()) {
            case 0 -> RenderType.entityCutoutNoCull(draw.texture());
            case 2 -> RenderType.entityTranslucentEmissive(draw.texture());
            default -> RenderType.entityTranslucent(draw.texture());
         };
      }
   }

   private static List<RuinDecayLayer.Draw> plan(
      RuinLook.Look look, RuinCrackSheets.Sheet sheet, ResourceLocation bodyTexture, float feetY, float height, @Nullable RuinDecayLayer.BodyDraw vanillaBody
   ) {
      List<RuinDecayLayer.Draw> draws = new ArrayList<>(5);
      boolean crumbling = look.crumbling();
      float crumbleY = crumbling ? feetY + height * (1.05F - 1.1F * look.crumble()) : Float.NaN;
      float darken = 1.0F - RuinLook.GREY_DARKEN.getF() * Mth.clamp((look.depth() - 2.5F) / 2.0F, 0.0F, 1.0F);
      if (crumbling) {
         darken = Mth.lerp(look.crumble(), darken, darken * 0.75F);
         DecayVertexSink.Pass body = pass(look, 0, 0, crumbleY, sheet);
         draws.add(
            new RuinDecayLayer.Draw(0, bodyTexture, body, vanillaBody != null ? vanillaBody.tint() : -1, vanillaBody != null ? vanillaBody.type() : null)
         );
      }

      if (look.grey() > 0.004F) {
         DecayVertexSink.Pass grey = pass(look, 1, 0, crumbleY, sheet);
         grey.alpha = look.grey();
         grey.darken = darken;
         boolean baked = sheet.grey() != null;
         draws.add(new RuinDecayLayer.Draw(1, baked ? sheet.grey() : bodyTexture, grey, baked ? -1 : -6646385, null));
      }

      if (look.depth() > 0.004F || crumbling) {
         for (int crackPass = 0; crackPass <= 1; crackPass++) {
            DecayVertexSink.Pass cracks = pass(look, 2, crackPass, crumbleY, sheet);
            cracks.darken = darken;
            draws.add(new RuinDecayLayer.Draw(1, sheet.crackAtlas(), cracks, -1, null));
         }

         if (look.flash() > 0.01F) {
            DecayVertexSink.Pass flash = pass(look, 2, 2, crumbleY, sheet);
            flash.alpha = RuinLook.smooth(look.flash());
            draws.add(new RuinDecayLayer.Draw(2, sheet.crackAtlas(), flash, -1, null));
         }
      }

      return draws;
   }

   private static DecayVertexSink.Pass pass(RuinLook.Look look, int mode, int crackPass, float crumbleY, RuinCrackSheets.Sheet sheet) {
      DecayVertexSink.Pass p = new DecayVertexSink.Pass();
      p.mode = mode;
      p.crackPass = crackPass;
      p.depth = look.depth();
      p.alpha1 = RuinLook.crackAlpha(1);
      p.alpha2 = RuinLook.crackAlpha(2);
      p.alpha3 = RuinLook.crackAlpha(3);
      p.alpha4 = RuinLook.crackAlpha(4);
      p.alpha = 1.0F;
      p.darken = 1.0F;
      p.inside = RuinLook.CRUMBLE_INSIDE.getF();
      p.crumbleY = crumbleY;
      p.playerLayout = sheet.playerLayout();
      p.cells = mode == 2 ? 8 : 0;
      return p;
   }

   private static record BodyDraw(RenderType type, int tint) {
   }

   private static record Draw(int kind, ResourceLocation texture, DecayVertexSink.Pass pass, int tint, @Nullable RenderType type) {
   }
}
