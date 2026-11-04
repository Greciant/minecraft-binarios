package com.devilfruits.mobs.pirate.client;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.model.geo.GeoAnimation;
import com.devilfruits.client.model.geo.GeoModel;
import com.devilfruits.client.model.geo.GeoOverlays;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.core.npc.AnimData;
import com.devilfruits.core.npc.Locomotion;
import com.devilfruits.mobs.pirate.PirateClips;
import com.devilfruits.mobs.pirate.Pirates;
import com.devilfruits.mobs.pirate.entity.PirateCaptain;
import com.devilfruits.mobs.pirate.entity.PirateFighter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class PirateRenderer<T extends PirateFighter> extends EntityRenderer<T> {
   private static final float DEG = (float) (Math.PI / 180.0);
   private final ResourceLocation baseId;
   private final ResourceLocation outfitId;
   private final ResourceLocation baseTexture;
   private final ResourceLocation[] outfitTextures;
   private final ResourceLocation[] actionIds = new ResourceLocation[PirateClips.NAMES.length];
   private final boolean crossesArms;
   private int generation = -1;
   @Nullable
   private GeoModel base;
   private GeoRenderer baseGeo;
   private GeoRenderer outfitGeo;
   private GeoPose pose;
   private int root;
   private int body;
   private int head;
   private int jacket;
   private int arms;
   private int rightArm;
   private int leftArm;
   private int rightLeg;
   private int leftLeg;
   private int sheathed;
   private int holstered;
   private int slung;
   private int gripR;
   private int gripL;
   private int ramrodShort;
   private int ramrodLong;
   private int[] hanging = new int[0];
   private float[] hangingSwing = new float[0];
   private int[] frontPanels = new int[0];
   private int[] backPanels = new int[0];
   private int plume = -1;

   public PirateRenderer(Context context, String mob, int variants, String vanillaTexture, boolean crossesArms) {
      super(context);
      this.crossesArms = crossesArms;
      this.shadowRadius = 0.5F;
      this.baseId = Pirates.id(mob + "_base");
      this.outfitId = Pirates.id(mob + "_outfit");
      this.baseTexture = ResourceLocation.withDefaultNamespace("textures/entity/illager/" + vanillaTexture + ".png");
      this.outfitTextures = new ResourceLocation[variants];

      for (int i = 0; i < variants; i++) {
         this.outfitTextures[i] = DevilFruits.id("textures/entity/pirate/" + mob + "_" + i + ".png");
      }

      for (int i = 0; i < this.actionIds.length; i++) {
         this.actionIds[i] = clipId(PirateClips.NAMES[i]);
      }
   }

   private static ResourceLocation clipId(String name) {
      return Pirates.id("clips/" + name);
   }

   @Nullable
   private static GeoAnimation clip(String name) {
      return GeoResources.animation(clipId(name));
   }

   private boolean ensure() {
      int gen = GeoResources.MODELS.generation();
      if (gen == this.generation) {
         return this.base != null;
      } else {
         this.generation = gen;
         this.base = GeoResources.model(this.baseId);
         GeoModel outfit = GeoResources.model(this.outfitId);
         if (this.base != null && outfit != null && outfit.boneCount() == this.base.boneCount()) {
            this.baseGeo = new GeoRenderer(this.base);
            this.outfitGeo = new GeoRenderer(outfit);
            this.pose = new GeoPose(this.base);
            this.root = this.base.bone("root");
            this.body = this.base.bone("body");
            this.head = this.base.bone("head");
            this.jacket = this.base.bone("jacket");
            this.arms = this.base.bone("arms");
            this.rightArm = this.base.bone("right_arm");
            this.leftArm = this.base.bone("left_arm");
            this.rightLeg = this.base.bone("right_leg");
            this.leftLeg = this.base.bone("left_leg");
            this.sheathed = this.base.bone("sheathed");
            this.holstered = this.base.bone("holstered");
            this.slung = this.base.bone("slung");
            this.plume = this.base.bone("feather");
            this.gripR = this.base.bone("grip_r");
            this.gripL = this.base.bone("grip_l");
            this.ramrodShort = this.base.bone("ramrod_short");
            this.ramrodLong = this.base.bone("ramrod_long");
            String[] tails = new String[]{
               "bandana_knot", "scarf_tail", "sash_tail", "coat_tail_r1", "coat_tail_l1", "coat_tail_r2", "coat_tail_l2", "coat_back_r", "coat_back_l"
            };
            float[] amount = new float[]{0.9F, 0.9F, 0.6F, 0.7F, 0.7F, 0.35F, 0.35F, 0.7F, 0.7F};
            int n = 0;
            int[] found = new int[tails.length];
            float[] swing = new float[tails.length];

            for (int i = 0; i < tails.length; i++) {
               int b = this.base.bone(tails[i]);
               if (b >= 0) {
                  found[n] = b;
                  swing[n++] = amount[i];
               }
            }

            this.hanging = Arrays.copyOf(found, n);
            this.hangingSwing = Arrays.copyOf(swing, n);
            this.frontPanels = new int[]{this.base.bone("coat_front_r"), this.base.bone("coat_front_l")};
            this.backPanels = new int[]{this.firstOf("coat_tail_r1", "coat_back_r"), this.firstOf("coat_tail_l1", "coat_back_l")};
            return true;
         } else {
            this.base = null;
            return false;
         }
      }
   }

   private int firstOf(String a, String b) {
      int i = this.base.bone(a);
      return i >= 0 ? i : this.base.bone(b);
   }

   protected float getShadowRadius(T entity) {
      return this.shadowRadius * entity.getScale();
   }

   public ResourceLocation getTextureLocation(T entity) {
      return this.outfitTextures[Math.floorMod(entity.variant(), this.outfitTextures.length)];
   }

   public void render(T e, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
      if (!this.ensure()) {
         super.render(e, entityYaw, partialTick, poseStack, buffers, light);
      } else {
         float bodyYaw = Mth.rotLerp(partialTick, e.yBodyRotO, e.yBodyRot);
         float headYaw = Mth.rotLerp(partialTick, e.yHeadRotO, e.yHeadRot);
         float netHeadYaw = Mth.clamp(Mth.wrapDegrees(headYaw - bodyYaw), -60.0F, 60.0F);
         float headPitch = Mth.clamp(Mth.lerp(partialTick, e.xRotO, e.getXRot()), -50.0F, 50.0F);
         poseStack.pushPose();
         float scale = e.getScale();
         poseStack.scale(scale, scale, scale);
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
         poseStack.scale(0.0625F, 0.0625F, 0.0625F);
         this.pose.reset();
         boolean weaponOut = this.animate(e, e.anim, e.tickCount, partialTick, netHeadYaw, headPitch);
         this.baseGeo.pose(poseStack.last(), this.pose);
         this.outfitGeo.pose(poseStack.last(), this.pose);
         Minecraft mc = Minecraft.getInstance();
         boolean visible = !e.isInvisible();
         boolean translucent = !visible && mc.player != null && !e.isInvisibleTo(mc.player);
         boolean glowing = mc.shouldEntityAppearGlowing(e);
         int overlay = LivingEntityRenderer.getOverlayCoords(e, 0.0F);
         int color = translucent ? 654311423 : -1;
         RenderType baseType = type(this.baseTexture, visible, translucent, glowing);
         RenderType outfitType = type(this.getTextureLocation(e), visible, translucent, glowing);
         if (baseType != null) {
            this.baseGeo.render(buffers.getBuffer(baseType), this.pose, light, overlay, color);
            this.outfitGeo.render(buffers.getBuffer(outfitType), this.pose, light, overlay, color);
            if (visible) {
               GeoOverlays.render(e, this.baseGeo, this.pose, buffers, light, overlay);
               GeoOverlays.render(e, this.outfitGeo, this.pose, buffers, light, overlay);
            }
         }

         if (weaponOut && visible) {
            boolean loading = inWindow(e.anim, e.tickCount, partialTick, PirateClips.LEFT_FROM, PirateClips.LEFT_TO);
            this.renderItem(e, e.getMainHandItem(), loading ? this.gripL : this.gripR, loading, poseStack, buffers, light);
            if (!loading) {
               this.renderItem(e, e.getOffhandItem(), this.gripL, true, poseStack, buffers, light);
            }
         }

         poseStack.popPose();
         super.render(e, entityYaw, partialTick, poseStack, buffers, light);
      }
   }

   @Nullable
   private static RenderType type(ResourceLocation texture, boolean visible, boolean translucent, boolean glowing) {
      if (translucent) {
         return RenderType.itemEntityTranslucentCull(texture);
      } else if (visible) {
         return RenderType.entityCutoutNoCull(texture);
      } else {
         return glowing ? RenderType.outline(texture) : null;
      }
   }

   private static boolean inWindow(AnimData a, int age, float pt, int[] from, int[] to) {
      int id = a.actionId;
      if (id >= 0 && id < from.length && from[id] >= 0) {
         float t = a.actionTime(age, pt);
         return t >= (float)from[id] && t < (float)to[id];
      } else {
         return false;
      }
   }

   private boolean animate(T e, AnimData a, int age, float pt, float netHeadYaw, float headPitch) {
      boolean captain = e instanceof PirateCaptain;
      boolean dead = e.deathTime > 0 || !e.isAlive();
      float time = (float)age + pt;
      float fade = Mth.clamp(((float)(age - a.gaitAge) + pt) / 5.0F, 0.0F, 1.0F);
      float[] gait = new float[3];
      gait[a.gait.ordinal()] += fade;
      gait[a.previousGait.ordinal()] += 1.0F - fade;
      float fall = a.airTicks > 3 ? Mth.clamp(((float)(a.airTicks - 3) + pt) / 4.0F, 0.0F, 1.0F) : 0.0F;

      for (int i = 0; i < 3; i++) {
         gait[i] *= 1.0F - fall;
      }

      float idle = gait[Locomotion.Gait.IDLE.ordinal()];
      float moving = 1.0F - idle - fall;
      float armed = armedWeight(a, age, pt);
      float turn = idle * Mth.clamp((Math.abs(a.yawRate) - 2.0F) / 6.0F, 0.0F, 1.0F);
      this.add("idle", time, (idle - turn) * (1.0F - armed));
      this.add("idle_armed", time, (idle - turn) * armed);
      this.add("turn", time * (a.yawRate < 0.0F ? -1.0F : 1.0F), turn);
      float distance = Mth.lerp(pt, a.prevDistance, a.distance) * 20.0F;
      if (moving > 0.001F) {
         float forwardW = smooth(a.forward, 0.25F, 0.7F);
         float backW = smooth(-a.forward, 0.25F, 0.7F);
         float strafeW = Math.max(0.0F, 1.0F - forwardW - backW);
         float walk = gait[Locomotion.Gait.WALK.ordinal()];
         float run = gait[Locomotion.Gait.RUN.ordinal()];
         if (captain) {
            this.add("captain_walk", distance / 2.0F, walk * forwardW);
            this.add("captain_run", distance / 4.0F, run * forwardW);
         } else {
            this.add("walk", distance / 2.2F, walk * forwardW);
            this.add("run", distance / 4.6F, run * forwardW);
         }

         this.add("backpedal", distance / 1.6F, moving * backW);
         float strafeTime = distance / 1.8F;
         this.add("strafe", a.side < 0.0F ? -strafeTime : strafeTime, moving * strafeW);
         this.add("armed_arms", 0.0F, moving * armed);
      }

      this.add("fall", time, fall);
      float land = (float)(age - a.landAge) + pt;
      if (land < 6.0F) {
         this.add("land", land, 1.0F);
      }

      float draw = a.armed ? (float)(age - a.armedAge) + pt : 100.0F;
      boolean putAway = !dead && (!a.armed || draw < 5.0F);
      boolean crossed = putAway && this.crossesArms;
      if (a.armed && draw < (float)PirateClips.LENGTH[0]) {
         this.blend("draw", draw);
      }

      float aim = Mth.lerp(pt, a.prevAim, a.aim);
      if (aim > 0.001F && !dead) {
         GeoAnimation clip = clip(e.getMainHandItem().is((Item)Pirates.MUSKET.get()) ? "aim_musket" : "aim_pistol");
         if (clip != null) {
            clip.blend(this.base, this.pose, time % clip.length(), aim);
         }
      }

      if (a.actionId >= 0 && a.actionId < this.actionIds.length && !dead) {
         GeoAnimation clip = GeoResources.animation(this.actionIds[a.actionId]);
         float t = a.actionTime(age, pt);
         if (clip != null && t < clip.length()) {
            clip.blend(this.base, this.pose, t, fadeWeight(clip, t));
         }
      }

      float sinceAction = a.actionId >= 0 && a.actionId < PirateClips.LENGTH.length ? a.actionTime(age, pt) - (float)PirateClips.LENGTH[a.actionId] : 100.0F;
      if (!dead && e.getMainHandItem().is((Item)Pirates.MUSKET.get())) {
         this.rotate(this.gripR, -50.0F * armed * (1.0F - aim) * Mth.clamp(sinceAction / 6.0F, 0.0F, 1.0F), 0.0F, 0.0F);
      }

      float aimArms = aim;
      int act = a.actionId;
      float actT = a.actionTime(age, pt);
      boolean shot = (act == 5 || act == 7 || act == 13) && actT < (float)PirateClips.LENGTH[act];
      if (shot) {
         aimArms = Math.max(aim, 1.0F - Mth.clamp((actT - (float)PirateClips.LENGTH[act] + 4.0F) / 4.0F, 0.0F, 1.0F));
      }

      if (aimArms > 0.001F && !dead) {
         float pitch = -headPitch * (float) (Math.PI / 180.0) * aimArms;
         boolean musketHeld = e.getMainHandItem().is((Item)Pirates.MUSKET.get());
         if (act != 13 || !shot) {
            this.pose.addRotation(this.rightArm, pitch, 0.0F, 0.0F);
         }

         if (musketHeld || act == 13 && shot) {
            this.pose.addRotation(this.leftArm, pitch, 0.0F, 0.0F);
         }
      }

      float hurt = (float)(age - a.hurtAge) + pt;
      if (hurt < 8.0F && !dead) {
         this.add(a.hurtFromBehind ? "hurt_back" : "hurt_front", hurt, 1.0F);
      }

      if (dead) {
         GeoAnimation death = clip("death");
         if (death != null) {
            death.blend(this.base, this.pose, Math.min((float)(e.deathTime - 1) + pt, death.length() - 0.01F), 1.0F);
         }
      } else {
         this.pose.addRotation(this.head, -headPitch * (float) (Math.PI / 180.0), -netHeadYaw * (float) (Math.PI / 180.0), 0.0F);
      }

      float swing = Mth.lerp(pt, a.prevSwing, a.swing);
      float roll = Mth.lerp(pt, a.prevRoll, a.roll);

      for (int i = 0; i < this.hanging.length; i++) {
         float flutter = Mth.sin(time * 0.17F + (float)i * 1.3F) * (1.5F + swing * 0.08F);
         this.rotate(this.hanging[i], (swing + flutter) * this.hangingSwing[i], 0.0F, roll * 0.35F);
      }

      float rightLegX = this.pose.rot[this.rightLeg * 3];
      float leftLegX = this.pose.rot[this.leftLeg * 3];
      this.bendWithLeg(this.frontPanels[0], rightLegX, true, swing * 0.3F);
      this.bendWithLeg(this.frontPanels[1], leftLegX, true, swing * 0.3F);
      this.bendWithLeg(this.backPanels[0], rightLegX, false, 0.0F);
      this.bendWithLeg(this.backPanels[1], leftLegX, false, 0.0F);
      if (this.plume >= 0) {
         this.rotate(this.plume, -swing * 0.6F + Mth.sin(time * 0.21F) * 2.0F, 0.0F, roll * 0.4F);
      }

      boolean weaponOut = !putAway && (!dead || e.deathTime <= 10);
      this.pose.hide(this.arms, !crossed);
      this.pose.hide(this.rightArm, crossed);
      this.pose.hide(this.leftArm, crossed);
      this.pose.hide(this.jacket, true);
      boolean rod = !dead && inWindow(a, age, pt, PirateClips.ROD_OUT, PirateClips.ROD_IN);
      this.pose.hide(this.ramrodShort, !rod || a.actionId != 6);
      this.pose.hide(this.ramrodLong, !rod || a.actionId != 8);
      this.pose.hide(this.sheathed, !putAway);
      boolean musket = e.getMainHandItem().is((Item)Pirates.MUSKET.get());
      boolean pistol = e.getMainHandItem().is((Item)Pirates.FLINTLOCK_PISTOL.get());
      this.pose.hide(this.holstered, !putAway || !pistol);
      this.pose.hide(this.slung, !putAway || !musket);
      return weaponOut;
   }

   private static float armedWeight(AnimData a, int age, float pt) {
      float t = Mth.clamp(((float)(age - a.armedAge) + pt) / 6.0F, 0.0F, 1.0F);
      return a.armed ? t : 1.0F - t;
   }

   private static float smooth(float x, float from, float to) {
      float t = Mth.clamp((x - from) / (to - from), 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private static float fadeWeight(GeoAnimation clip, float t) {
      float in = clip.fadeIn() > 0.0F ? Math.min(1.0F, t / clip.fadeIn()) : 1.0F;
      float out = clip.fadeOut() > 0.0F ? Math.min(1.0F, (clip.length() - t) / clip.fadeOut()) : 1.0F;
      return Mth.clamp(Math.min(in, out), 0.0F, 1.0F);
   }

   private void add(String name, float ticks, float weight) {
      if (!(weight <= 0.001F)) {
         GeoAnimation clip = clip(name);
         if (clip != null) {
            float len = clip.length();
            clip.add(this.base, this.pose, clip.loop() ? Mth.positiveModulo(ticks, len) : Math.min(ticks, len), weight);
         }
      }
   }

   private void blend(String name, float ticks) {
      GeoAnimation clip = clip(name);
      if (clip != null) {
         clip.blend(this.base, this.pose, ticks, fadeWeight(clip, ticks));
      }
   }

   private void rotate(int bone, float x, float y, float z) {
      this.pose.addRotation(bone, -x * (float) (Math.PI / 180.0), -y * (float) (Math.PI / 180.0), z * (float) (Math.PI / 180.0));
   }

   private void bendWithLeg(int panel, float legX, boolean front, float extraDegrees) {
      if (panel >= 0) {
         float push = front ? Math.max(0.0F, legX) : Math.min(0.0F, legX);
         this.pose.addRotation(panel, push * 1.15F - extraDegrees * (float) (Math.PI / 180.0), 0.0F, 0.0F);
      }
   }

   private void renderItem(T e, ItemStack stack, int bone, boolean left, PoseStack poseStack, MultiBufferSource buffers, int light) {
      if (!stack.isEmpty() && bone >= 0 && !this.pose.hidden[bone]) {
         GeoModel.Bone b = this.base.bones[bone];
         poseStack.pushPose();
         poseStack.last().pose().set(this.baseGeo.boneMatrix(bone));
         poseStack.last().normal().set(this.baseGeo.boneNormal(bone));
         poseStack.translate(b.pivotX, b.pivotY, b.pivotZ);
         poseStack.scale(16.0F, 16.0F, 16.0F);
         poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
         poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
         poseStack.translate((left ? -1.0F : 1.0F) / 16.0F, 0.125F, 0.0F);
         Minecraft.getInstance()
            .getEntityRenderDispatcher()
            .getItemInHandRenderer()
            .renderItem(
               e, stack, left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, left, poseStack, buffers, light
            );
         poseStack.popPose();
      }
   }
}
