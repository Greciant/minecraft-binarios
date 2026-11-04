package com.devilfruits.client.animation;

import com.devilfruits.DevilFruits;
import com.devilfruits.core.config.CoreClientConfig;
import com.devilfruits.core.fruit.DevilFruitItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class AnimationController {
   private static final ResourceLocation EAT_FRUIT = DevilFruits.id("eat_fruit");
   private static final Set<ResourceLocation> WARNED = new HashSet<>();
   private static final List<AnimationController.PoseLayer> LAYERS = new CopyOnWriteArrayList<>();
   private static final Int2ObjectMap<AnimationController.Active> ACTIVE = new Int2ObjectOpenHashMap();
   private static final AnimationController.Active EATING = new AnimationController.Active();
   private static final AnimationController.Active USING = new AnimationController.Active();
   private static final List<AnimationController.UseAnimation> USE_ANIMATIONS = new ArrayList<>();
   private static final float[] ROT = new float[3];
   private static final float[] POS = new float[3];
   private static final List<AnimationController.ClipSource> SOURCES = new ArrayList<>();
   private static HumanoidModel<?> layerModel;
   private static float layerAge;
   private static boolean layered;
   private static final AnimationController.PoseSink MODEL_SINK = (id, time, weight) -> {
      PlayerAnimation clip = weight > 0.001F ? AnimationLoader.INSTANCE.get(id) : null;
      if (clip != null) {
         applyAdditive(layerModel, clip, layerTime(clip, time), Math.min(1.0F, weight), layerAge);
         layered = true;
      }
   };
   private static final float[] LAYER_ROOT = new float[6];
   private static final float[] LEAN = new float[2];
   private static final AnimationController.PoseSink ROOT_SINK = (id, time, weight) -> {
      PlayerAnimation clip = weight > 0.001F ? AnimationLoader.INSTANCE.get(id) : null;
      if (clip != null && clip.root != null) {
         clip.root.sample(layerTime(clip, time), ROT, POS);
         float w = Math.min(1.0F, weight);

         for (int i = 0; i < 3; i++) {
            LAYER_ROOT[i] = LAYER_ROOT[i] + ROT[i] * w;
            LAYER_ROOT[3 + i] = LAYER_ROOT[3 + i] + POS[i] * w;
         }
      }
   };

   public static void addPoseLayer(AnimationController.PoseLayer layer) {
      LAYERS.add(layer);
   }

   public static void registerUseAnimation(Predicate<ItemStack> item, ResourceLocation mainHand, ResourceLocation offHand) {
      USE_ANIMATIONS.add(new AnimationController.UseAnimation(item, mainHand, offHand));
   }

   public static void addClipSource(AnimationController.ClipSource source) {
      SOURCES.add(source);
   }

   @Nullable
   private static AnimationClip find(ResourceLocation id) {
      AnimationClip clip = AnimationLoader.INSTANCE.get(id);

      for (int i = 0; clip == null && i < SOURCES.size(); i++) {
         clip = SOURCES.get(i).get(id);
      }

      return clip;
   }

   public static boolean current(Entity entity, float partialTick, AnimationController.ClipState out) {
      AnimationController.Active active = (AnimationController.Active)ACTIVE.get(entity.getId());
      if (active == null) {
         out.id = null;
         out.clip = null;
         out.previous = null;
         out.previousId = null;
         return false;
      } else {
         float age = (float)entity.tickCount + partialTick;
         out.id = active.id;
         out.clip = active.anim;
         out.time = active.time(age);
         out.weight = active.weight(age);
         if (active.previous != null) {
            out.previous = active.previous.anim;
            out.previousId = active.previous.id;
            out.previousTime = active.previousTime;
            out.previousWeight = active.previousWeight * (1.0F - active.fadeInProgress(age));
         } else {
            out.previous = null;
            out.previousId = null;
         }

         return true;
      }
   }

   public static void play(Entity entity, ResourceLocation id) {
      AnimationClip anim = find(id);
      if (anim == null) {
         if (WARNED.add(id)) {
            DevilFruits.LOGGER.warn("Unknown player animation {}", id);
         }
      } else {
         float age = (float)entity.tickCount;
         AnimationController.Active next = new AnimationController.Active();
         next.id = id;
         next.anim = anim;
         next.startAge = age;
         AnimationController.Active old = (AnimationController.Active)ACTIVE.get(entity.getId());
         if (old != null) {
            next.previous = old;
            next.previousTime = old.time(age);
            next.previousWeight = old.weight(age);
            old.previous = null;
         }

         ACTIVE.put(entity.getId(), next);
      }
   }

   public static void stop(Entity entity) {
      AnimationController.Active active = (AnimationController.Active)ACTIVE.get(entity.getId());
      if (active != null && active.stopAge < 0.0F) {
         active.stopAge = (float)entity.tickCount;
      }
   }

   public static boolean isAnimating(Entity entity) {
      return ACTIVE.containsKey(entity.getId());
   }

   public static void tick() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         ACTIVE.clear();
      } else {
         ACTIVE.int2ObjectEntrySet().removeIf(e -> {
            Entity entity = level.getEntity(e.getIntKey());
            return entity == null || ((AnimationController.Active)e.getValue()).finished((float)entity.tickCount);
         });
      }
   }

   public static void clear() {
      ACTIVE.clear();
   }

   @Nullable
   private static AnimationController.Active activeFor(Player player, float age) {
      AnimationController.Active active = (AnimationController.Active)ACTIVE.get(player.getId());
      if (active != null) {
         return active;
      } else {
         if (player.isUsingItem() && player.getUseItem().getItem() instanceof DevilFruitItem) {
            PlayerAnimation eat = AnimationLoader.INSTANCE.get(EAT_FRUIT);
            if (eat != null) {
               EATING.id = EAT_FRUIT;
               EATING.anim = eat;
               EATING.startAge = age - (float)player.getTicksUsingItem() - (age - (float)((int)age));
               EATING.stopAge = -1.0F;
               EATING.previous = null;
               return EATING;
            }
         }

         if (player.isUsingItem()) {
            for (AnimationController.UseAnimation use : USE_ANIMATIONS) {
               if (use.item().test(player.getUseItem())) {
                  ResourceLocation id = player.getUsedItemHand() == InteractionHand.MAIN_HAND ? use.mainHand() : use.offHand();
                  PlayerAnimation clip = AnimationLoader.INSTANCE.get(id);
                  if (clip != null) {
                     USING.id = id;
                     USING.anim = clip;
                     USING.startAge = age - (float)player.getTicksUsingItem() - (age - (float)((int)age));
                     USING.stopAge = -1.0F;
                     USING.previous = null;
                     return USING;
                  }
               }
            }
         }

         return null;
      }
   }

   private static float layerTime(PlayerAnimation clip, float time) {
      float len = clip.length();
      if (clip.loop() && !(len <= 0.0F) && !(time <= len)) {
         float from = Mth.clamp(clip.loopFrom(), 0.0F, len - 0.001F);
         return from + (time - from) % (len - from);
      } else {
         return time;
      }
   }

   private static void applyAdditive(HumanoidModel<?> model, PlayerAnimation anim, float t, float w, float age) {
      for (Entry<PlayerAnimation.Part, PlayerAnimation.Channel> entry : anim.parts.entrySet()) {
         PlayerAnimation.Part p = entry.getKey();
         entry.getValue().sample(t, ROT, POS);
         addTremble(anim, p, age);
         ModelPart part = partOf(model, p);
         boolean limb = p == PlayerAnimation.Part.RIGHT_ARM
            || p == PlayerAnimation.Part.LEFT_ARM
            || p == PlayerAnimation.Part.RIGHT_LEG
            || p == PlayerAnimation.Part.LEFT_LEG;
         boolean leg = p == PlayerAnimation.Part.RIGHT_LEG || p == PlayerAnimation.Part.LEFT_LEG;
         part.xRot = part.xRot + ROT[0] * w;
         part.yRot = part.yRot + ROT[1] * w;
         if (limb) {
            part.zRot = part.zRot + ROT[2] * w;
         }

         part.y = part.y + POS[1] * w;
         if (leg) {
            part.z = part.z + POS[2] * w;
         }
      }
   }

   public static void applyToModel(HumanoidModel<?> model, AbstractClientPlayer player, float ageInTicks) {
      restoreUnposed(model);
      layered = false;
      if (!LAYERS.isEmpty()) {
         layerModel = model;
         layerAge = ageInTicks;

         for (AnimationController.PoseLayer layer : LAYERS) {
            layer.poses(player, ageInTicks - (float)player.tickCount, MODEL_SINK);
         }

         layerModel = null;
      }

      AnimationController.Active active = activeFor(player, ageInTicks);
      if (active == null) {
         if (layered) {
            model.hat.copyFrom(model.head);
         }
      } else {
         float w = active.weight(ageInTicks);
         if (active.previous != null && active.previous.humanoid() != null) {
            float pw = active.previousWeight * (1.0F - active.fadeInProgress(ageInTicks));
            if (pw > 0.001F) {
               applyAnimation(model, active.previous.humanoid(), active.previousTime, pw, ageInTicks);
            }
         }

         if (w > 0.001F && active.humanoid() != null) {
            applyAnimation(model, active.humanoid(), active.time(ageInTicks), w, ageInTicks);
         }

         model.hat.copyFrom(model.head);
      }
   }

   private static void restoreUnposed(HumanoidModel<?> model) {
      model.rightLeg.x = model.rightLeg.getInitialPose().x;
      model.leftLeg.x = model.leftLeg.getInitialPose().x;
      model.head.x = model.head.getInitialPose().x;
      model.body.x = model.body.getInitialPose().x;
      model.head.z = model.head.getInitialPose().z;
      model.body.z = model.body.getInitialPose().z;
   }

   private static void applyAnimation(HumanoidModel<?> model, PlayerAnimation anim, float t, float w, float age) {
      float bodyYawBefore = model.body.yRot;
      PlayerAnimation.Channel body = anim.parts.get(PlayerAnimation.Part.BODY);
      if (body != null) {
         body.sample(t, ROT, POS);
         addTremble(anim, PlayerAnimation.Part.BODY, age);
         blend(model.body, w);
         float by = model.body.yRot;
         model.rightArm.z = Mth.sin(by) * 5.0F;
         model.rightArm.x = -Mth.cos(by) * 5.0F;
         model.leftArm.z = -Mth.sin(by) * 5.0F;
         model.leftArm.x = Mth.cos(by) * 5.0F;
      }

      float bodyYawDelta = model.body.yRot - bodyYawBefore;

      for (Entry<PlayerAnimation.Part, PlayerAnimation.Channel> entry : anim.parts.entrySet()) {
         PlayerAnimation.Part part = entry.getKey();
         if (part != PlayerAnimation.Part.BODY) {
            entry.getValue().sample(t, ROT, POS);
            addTremble(anim, part, age);
            if (part == PlayerAnimation.Part.RIGHT_ARM || part == PlayerAnimation.Part.LEFT_ARM) {
               ROT[1] = ROT[1] + model.body.yRot;
            }

            blend(partOf(model, part), w);
         }
      }

      if (bodyYawDelta != 0.0F) {
         if (!anim.parts.containsKey(PlayerAnimation.Part.RIGHT_ARM)) {
            model.rightArm.yRot += bodyYawDelta;
         }

         if (!anim.parts.containsKey(PlayerAnimation.Part.LEFT_ARM)) {
            model.leftArm.yRot += bodyYawDelta;
         }
      }
   }

   private static void addTremble(PlayerAnimation anim, PlayerAnimation.Part part, float age) {
      Float amplitude = anim.tremble.get(part);
      if (amplitude != null) {
         float seed = (float)part.ordinal() * 1.7F;
         ROT[0] = ROT[0] + amplitude * (Mth.sin(age * 4.1F + seed) * 0.6F + Mth.sin(age * 9.7F + seed * 2.0F) * 0.4F);
         ROT[1] = ROT[1] + amplitude * 0.5F * Mth.sin(age * 6.3F + seed * 3.0F);
         ROT[2] = ROT[2] + amplitude * (Mth.sin(age * 7.9F + seed) * 0.5F + Mth.sin(age * 13.1F) * 0.3F);
      }
   }

   private static void blend(ModelPart part, float w) {
      part.xRot = Mth.lerp(w, part.xRot, ROT[0]);
      part.yRot = Mth.lerp(w, part.yRot, ROT[1]);
      part.zRot = Mth.lerp(w, part.zRot, ROT[2]);
      part.x = part.x + POS[0] * w;
      part.y = part.y + POS[1] * w;
      part.z = part.z + POS[2] * w;
   }

   private static ModelPart partOf(HumanoidModel<?> model, PlayerAnimation.Part part) {
      return switch (part) {
         case HEAD -> model.head;
         case BODY -> model.body;
         case RIGHT_ARM -> model.rightArm;
         case LEFT_ARM -> model.leftArm;
         case RIGHT_LEG -> model.rightLeg;
         case LEFT_LEG -> model.leftLeg;
      };
   }

   public static boolean applyRoot(PoseStack poseStack, AbstractClientPlayer player, float partialTick) {
      float age = (float)player.tickCount + partialTick;
      AnimationController.Active active = activeFor(player, age);
      PlayerAnimation humanoid = active == null ? null : active.humanoid();
      boolean clip = humanoid != null && humanoid.root != null;
      Arrays.fill(LAYER_ROOT, 0.0F);
      LEAN[0] = 0.0F;
      LEAN[1] = 0.0F;

      for (AnimationController.PoseLayer layer : LAYERS) {
         layer.poses(player, partialTick, ROOT_SINK);
         layer.lean(player, partialTick, LEAN);
      }

      LAYER_ROOT[0] = LAYER_ROOT[0] + LEAN[0];
      LAYER_ROOT[2] = LAYER_ROOT[2] + LEAN[1];
      boolean layerRoot = false;

      for (float v : LAYER_ROOT) {
         layerRoot |= Math.abs(v) > 1.0E-5F;
      }

      if (!clip && !layerRoot) {
         return false;
      } else {
         float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
         poseStack.pushPose();
         poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
         if (layerRoot) {
            poseStack.translate(LAYER_ROOT[3], LAYER_ROOT[4], -LAYER_ROOT[5]);
            poseStack.translate(0.0, 0.9, 0.0);
            poseStack.mulPose(Axis.YP.rotation(LAYER_ROOT[1]));
            poseStack.mulPose(Axis.XP.rotation(-LAYER_ROOT[0]));
            poseStack.mulPose(Axis.ZP.rotation(LAYER_ROOT[2]));
            poseStack.translate(0.0, -0.9, 0.0);
         }

         if (!clip) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-(180.0F - bodyYaw)));
            return true;
         } else {
            float w = active.weight(age);
            humanoid.root.sample(active.time(age), ROT, POS);
            poseStack.translate(POS[0] * w, POS[1] * w, -POS[2] * w);
            if (humanoid.rootScale != null) {
               humanoid.rootScale.sample(active.time(age), ROT, POS);
               poseStack.scale(Mth.lerp(w, 1.0F, POS[0]), Mth.lerp(w, 1.0F, POS[1]), Mth.lerp(w, 1.0F, POS[2]));
               humanoid.root.sample(active.time(age), ROT, POS);
            }

            poseStack.translate(0.0, 0.9, 0.0);
            poseStack.mulPose(Axis.YP.rotation(ROT[1] * w));
            poseStack.mulPose(Axis.XP.rotation(-ROT[0] * w));
            poseStack.mulPose(Axis.ZP.rotation(ROT[2] * w));
            poseStack.translate(0.0, -0.9, 0.0);
            poseStack.mulPose(Axis.YP.rotationDegrees(-(180.0F - bodyYaw)));
            return true;
         }
      }
   }

   public static void applyFirstPerson(PoseStack poseStack, Player player, HumanoidArm arm, float partialTick) {
      if ((Boolean)CoreClientConfig.FIRST_PERSON_ANIMATIONS.get()) {
         float age = (float)player.tickCount + partialTick;
         AnimationController.Active active = activeFor(player, age);
         PlayerAnimation humanoid = active == null ? null : active.humanoid();
         if (humanoid != null) {
            PlayerAnimation.Channel channel = arm == HumanoidArm.RIGHT ? humanoid.fpRight : humanoid.fpLeft;
            if (channel != null) {
               float w = active.weight(age);
               channel.sample(active.time(age), ROT, POS);
               float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
               poseStack.translate(side * POS[0] * w, POS[1] * w, -POS[2] * w);
               poseStack.mulPose(Axis.XP.rotation(ROT[0] * w));
               poseStack.mulPose(Axis.YP.rotation(side * ROT[1] * w));
               poseStack.mulPose(Axis.ZP.rotation(side * ROT[2] * w));
            }
         }
      }
   }

   public static boolean wantsBothArmsFirstPerson(Player player, float partialTick) {
      if (!(Boolean)CoreClientConfig.FIRST_PERSON_ANIMATIONS.get()) {
         return false;
      } else {
         float age = (float)player.tickCount + partialTick;
         AnimationController.Active active = (AnimationController.Active)ACTIVE.get(player.getId());
         return active != null && active.humanoid() != null && active.humanoid().bothArmsFirstPerson && active.weight(age) > 0.05F;
      }
   }

   private AnimationController() {
   }

   private static final class Active {
      ResourceLocation id;
      AnimationClip anim;
      float startAge;
      float stopAge = -1.0F;
      @Nullable
      AnimationController.Active previous;
      float previousTime;
      float previousWeight;

      float time(float age) {
         float t = age - this.startAge;
         if (this.anim.loop()) {
            float from = this.anim.loopFrom();
            if (from > 0.0F && from < this.anim.length()) {
               return t < this.anim.length() ? t : from + (t - this.anim.length()) % (this.anim.length() - from);
            } else {
               return this.anim.length() <= 0.0F ? 0.0F : t % this.anim.length();
            }
         } else {
            return Math.min(t, this.anim.length());
         }
      }

      float weight(float age) {
         float t = age - this.startAge;
         float w = this.anim.fadeIn() > 0.0F ? Mth.clamp(t / this.anim.fadeIn(), 0.0F, 1.0F) : 1.0F;
         if (!this.anim.loop() && t > this.anim.length()) {
            w *= 1.0F - Mth.clamp((t - this.anim.length()) / Math.max(0.01F, this.anim.fadeOut()), 0.0F, 1.0F);
         }

         if (this.stopAge >= 0.0F) {
            w *= 1.0F - Mth.clamp((age - this.stopAge) / Math.max(0.01F, this.anim.fadeOut()), 0.0F, 1.0F);
         }

         return w;
      }

      float fadeInProgress(float age) {
         return this.anim.fadeIn() > 0.0F ? Mth.clamp((age - this.startAge) / this.anim.fadeIn(), 0.0F, 1.0F) : 1.0F;
      }

      boolean finished(float age) {
         float t = age - this.startAge;
         return this.stopAge >= 0.0F ? age - this.stopAge > this.anim.fadeOut() : !this.anim.loop() && t > this.anim.length() + this.anim.fadeOut();
      }

      @Nullable
      PlayerAnimation humanoid() {
         return this.anim instanceof PlayerAnimation p ? p : null;
      }
   }

   public interface ClipSource {
      @Nullable
      AnimationClip get(ResourceLocation var1);
   }

   public static final class ClipState {
      @Nullable
      public ResourceLocation id;
      @Nullable
      public AnimationClip clip;
      public float time;
      public float weight;
      @Nullable
      public AnimationClip previous;
      @Nullable
      public ResourceLocation previousId;
      public float previousTime;
      public float previousWeight;
   }

   public interface PoseLayer {
      void poses(Player var1, float var2, AnimationController.PoseSink var3);

      default void lean(Player player, float partialTick, float[] pitchRoll) {
      }
   }

   @FunctionalInterface
   public interface PoseSink {
      void add(ResourceLocation var1, float var2, float var3);
   }

   private static record UseAnimation(Predicate<ItemStack> item, ResourceLocation mainHand, ResourceLocation offHand) {
   }
}
