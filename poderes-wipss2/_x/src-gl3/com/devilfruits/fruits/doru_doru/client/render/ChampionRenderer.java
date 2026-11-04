package com.devilfruits.fruits.doru_doru.client.render;

import com.devilfruits.DevilFruits;
import com.devilfruits.client.animation.AnimationClip;
import com.devilfruits.client.animation.AnimationController;
import com.devilfruits.client.model.geo.GeoAnimation;
import com.devilfruits.client.model.geo.GeoModel;
import com.devilfruits.client.model.geo.GeoOverlays;
import com.devilfruits.client.model.geo.GeoPose;
import com.devilfruits.client.model.geo.GeoRenderer;
import com.devilfruits.client.model.geo.GeoResources;
import com.devilfruits.client.render.FormRenderers;
import com.devilfruits.core.form.FruitForm;
import com.devilfruits.fruits.doru_doru.DoruDoru;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;

public final class ChampionRenderer implements FormRenderers.FormRenderer {
   public static final ResourceLocation MODEL = DoruDoru.id("champion");
   private static final ResourceLocation TEXTURE = DevilFruits.id("textures/entity/doru_doru/champion.png");
   private static final String CLIP_PREFIX = "doru_doru/champion/";
   private static final float MODEL_SCALE = 0.034615383F;
   private static final float WALK_STRIDE = 13.0F;
   private static final float FIRST_PERSON_SCALE = 0.7F;
   private static final float DEG = (float) (Math.PI / 180.0);
   private final Int2ObjectMap<ChampionRenderer.State> states = new Int2ObjectOpenHashMap();
   private final AnimationController.ClipState clipState = new AnimationController.ClipState();
   private final Map<ResourceLocation, GeoAnimation> mapped = new HashMap<>();
   private int generation = -1;
   @Nullable
   private GeoModel model;
   private GeoRenderer geo;
   private GeoPose pose;
   private GeoPose fpPose;
   private GeoAnimation idle;
   private GeoAnimation walk;
   private GeoAnimation fall;
   private GeoAnimation punchRight;
   private GeoAnimation punchLeft;
   private GeoAnimation death;
   private int body;
   private int head;
   private int playerHead;
   private int rightArm;
   private int leftArm;
   private int rightFist;
   private int leftFist;
   @Nullable
   private SkullModel skull;

   private boolean ensureModel() {
      int gen = GeoResources.MODELS.generation();
      if (gen == this.generation) {
         return this.model != null;
      } else {
         this.generation = gen;
         this.mapped.clear();
         this.states.clear();
         this.model = GeoResources.model(MODEL);
         if (this.model == null) {
            DevilFruits.LOGGER.error("Candle Champion model {} is missing, drawing the vanilla player", MODEL);
            return false;
         } else {
            this.geo = new GeoRenderer(this.model);
            this.pose = new GeoPose(this.model);
            this.fpPose = new GeoPose(this.model);
            this.idle = clip("idle");
            this.walk = clip("walk");
            this.fall = clip("fall");
            this.punchRight = clip("punch_right");
            this.punchLeft = clip("punch_left");
            this.death = clip("death");
            this.body = this.model.bone("body");
            this.head = this.model.bone("head");
            this.playerHead = this.model.bone("player_head");
            this.skull = new SkullModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_HEAD));
            this.rightArm = this.model.bone("right_arm");
            this.leftArm = this.model.bone("left_arm");
            this.rightFist = this.model.bone("right_fist");
            this.leftFist = this.model.bone("left_fist");
            return true;
         }
      }
   }

   @Nullable
   private static GeoAnimation clip(String name) {
      return GeoResources.animation(DoruDoru.id("champion/" + name));
   }

   @Nullable
   private GeoAnimation resolve(@Nullable ResourceLocation id, @Nullable AnimationClip clip) {
      if (clip instanceof GeoAnimation g && g.id.getPath().startsWith("doru_doru/champion/")) {
         return g;
      }

      if (id == null) {
         return null;
      } else {
         GeoAnimation g = this.mapped.get(id);
         if (g == null && !this.mapped.containsKey(id)) {
            String path = id.getPath();
            g = clip(path.substring(path.lastIndexOf(47) + 1));
            this.mapped.put(id, g);
         }

         return g;
      }
   }

   @Override
   public boolean render(AbstractClientPlayer player, FruitForm form, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      if (!player.isSpectator() && this.ensureModel()) {
         float age = (float)player.tickCount + partialTick;
         float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
         float headYaw = Mth.rotLerp(partialTick, player.yHeadRotO, player.yHeadRot);
         float netHeadYaw = Mth.wrapDegrees(headYaw - bodyYaw);
         float headPitch = Mth.lerp(partialTick, player.xRotO, player.getXRot());
         ChampionRenderer.State st = (ChampionRenderer.State)this.states.computeIfAbsent(player.getId(), kx -> new ChampionRenderer.State());
         update(st, player, age);
         poseStack.pushPose();
         float scale = player.getScale();
         poseStack.scale(scale, scale, scale);
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
         if (player.deathTime > 0 && this.death == null) {
            float f = Math.min(1.0F, Mth.sqrt(((float)player.deathTime + partialTick - 1.0F) / 20.0F * 1.6F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(f * 90.0F));
         }

         poseStack.scale(0.034615383F, 0.034615383F, 0.034615383F);
         this.pose.reset();
         this.animate(player, st, age, partialTick, netHeadYaw, headPitch);
         this.geo.pose(poseStack.last(), this.pose);
         Minecraft mc = Minecraft.getInstance();
         boolean visible = !player.isInvisible();
         boolean translucent = !visible && !player.isInvisibleTo(mc.player);
         if (visible || translucent) {
            int overlay = LivingEntityRenderer.getOverlayCoords(player, 0.0F);
            RenderType type = translucent ? RenderType.itemEntityTranslucentCull(TEXTURE) : RenderType.entityCutoutNoCull(TEXTURE);
            this.geo.render(buffers.getBuffer(type), this.pose, packedLight, overlay, translucent ? 654311423 : -1);
            if (visible) {
               GeoOverlays.render(player, this.geo, this.pose, buffers, packedLight, overlay);
            }
         }

         if ((visible || translucent) && this.playerHead >= 0 && this.skull != null && !this.pose.hidden[this.playerHead]) {
            GeoModel.Bone at = this.model.bones[this.playerHead];
            poseStack.pushPose();
            poseStack.last().pose().set(this.geo.boneMatrix(this.playerHead));
            poseStack.last().normal().set(this.geo.boneNormal(this.playerHead));
            poseStack.translate(at.pivotX, at.pivotY, at.pivotZ);
            float k = 1.0F / (0.034615383F * player.getScale());
            poseStack.scale(-k, -k, k);
            this.skull.setupAnim(0.0F, 0.0F, 0.0F);
            ResourceLocation skin = player.getSkin().texture();
            RenderType type = translucent ? RenderType.itemEntityTranslucentCull(skin) : RenderType.entityTranslucent(skin);
            this.skull
               .renderToBuffer(
                  poseStack, buffers.getBuffer(type), packedLight, LivingEntityRenderer.getOverlayCoords(player, 0.0F), translucent ? 654311423 : -1
               );
            poseStack.popPose();
         }

         poseStack.popPose();
         FormRenderers.renderNameTag(player, partialTick, poseStack, buffers, packedLight);
         return true;
      } else {
         return false;
      }
   }

   private static void update(ChampionRenderer.State st, AbstractClientPlayer player, float age) {
      st.lastSeen = player.tickCount;
      float dt = age - st.lastAge;
      if (!Float.isNaN(st.lastAge) && !(dt < 0.0F) && !(dt > 10.0F)) {
         st.lastAge = age;
         boolean airborne = !player.onGround() && !player.isInWater() && !player.isPassenger() && !player.getAbilities().flying;
         st.air = st.air + ((airborne ? 1.0F : 0.0F) - st.air) * Math.min(1.0F, 0.25F * dt);
      } else {
         st.lastAge = age;
      }
   }

   private void animate(AbstractClientPlayer player, ChampionRenderer.State st, float age, float partialTick, float netHeadYaw, float headPitch) {
      GeoModel m = this.model;
      float speed = !player.isPassenger() && player.isAlive() ? Math.min(1.0F, player.walkAnimation.speed(partialTick)) : 0.0F;
      float position = player.walkAnimation.position(partialTick);
      float moving = Mth.clamp(speed * 1.6F, 0.0F, 1.0F) * (1.0F - st.air);
      if (this.idle != null) {
         this.idle.add(m, this.pose, this.idle.length() <= 0.0F ? 0.0F : age % this.idle.length(), 1.0F - moving);
      }

      if (this.walk != null) {
         float phase = position / 13.0F;
         this.walk.add(m, this.pose, this.walk.length() * (phase - (float)Mth.floor(phase)), moving);
      }

      if (this.fall != null && st.air > 0.01F) {
         this.fall.blend(m, this.pose, 0.0F, st.air);
      }

      if (AnimationController.current(player, partialTick, this.clipState)) {
         GeoAnimation previous = this.resolve(this.clipState.previousId, this.clipState.previous);
         if (previous != null && this.clipState.previousWeight > 0.001F) {
            previous.blend(m, this.pose, this.clipState.previousTime, this.clipState.previousWeight);
         }

         GeoAnimation current = this.resolve(this.clipState.id, this.clipState.clip);
         if (current != null) {
            current.blend(m, this.pose, this.clipState.time, this.clipState.weight);
         }
      }

      float attack = player.getAttackAnim(partialTick);
      if (attack > 0.0F) {
         boolean right = player.getMainArm() == HumanoidArm.RIGHT;
         GeoAnimation punch = right ? this.punchRight : this.punchLeft;
         if (punch != null) {
            punch.blend(m, this.pose, attack * punch.length(), Mth.sin(Math.min(1.0F, attack * 1.2F) * (float) Math.PI) * 0.6F + 0.4F);
         }
      }

      float yaw = Mth.clamp(netHeadYaw, -45.0F, 45.0F) * (float) (Math.PI / 180.0);
      float pitch = Mth.clamp(headPitch, -30.0F, 30.0F) * (float) (Math.PI / 180.0);
      this.pose.addRotation(this.head, -pitch * 0.4F, -yaw * 0.5F, 0.0F);
      this.pose.addRotation(this.body, 0.0F, -yaw * 0.2F, 0.0F);
      if (player.hurtTime > 0 && player.deathTime == 0) {
         float h = ((float)player.hurtTime - partialTick) / 10.0F;
         this.pose.addRotation(this.body, Mth.sin(h * (float) Math.PI) * 0.08F, 0.0F, 0.0F);
      }

      if (this.death != null && player.deathTime > 0) {
         this.death.blend(m, this.pose, (float)player.deathTime + partialTick, 1.0F);
      }
   }

   @Override
   public boolean renderArm(AbstractClientPlayer player, FruitForm form, HumanoidArm arm, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
      if (!this.ensureModel()) {
         return false;
      } else {
         boolean right = arm == HumanoidArm.RIGHT;
         int upper = right ? this.rightArm : this.leftArm;
         int fist = right ? this.rightFist : this.leftFist;
         if (upper >= 0 && fist >= 0) {
            GeoModel.Bone shoulder = this.model.bones[upper];
            poseStack.pushPose();
            poseStack.translate((right ? -5.0F : 5.0F) / 16.0F, 0.25F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            float k = 0.04375F;
            poseStack.scale(k, k, k);
            poseStack.translate(-shoulder.pivotX, -shoulder.pivotY, -shoulder.pivotZ);
            this.fpPose.reset();
            this.fpPose.addRotation(upper, -shoulder.rotX, -shoulder.rotY, -shoulder.rotZ);
            this.geo.pose(poseStack.last(), this.fpPose);
            VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
            this.geo.renderBone(vc, shoulder, packedLight, OverlayTexture.NO_OVERLAY, -1);
            this.geo.renderBone(vc, this.model.bones[fist], packedLight, OverlayTexture.NO_OVERLAY, -1);
            poseStack.popPose();
            return true;
         } else {
            return false;
         }
      }
   }

   public void prune(ClientLevel level) {
      this.states.int2ObjectEntrySet().removeIf(e -> {
         Entity entity = level.getEntity(e.getIntKey());
         return entity == null || entity.tickCount - ((ChampionRenderer.State)e.getValue()).lastSeen > 200;
      });
   }

   private static final class State {
      float lastAge = Float.NaN;
      int lastSeen;
      float air;
   }
}
