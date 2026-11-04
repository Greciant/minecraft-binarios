package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.core.AwId;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class PyroHeatPose implements IAnimation {
   public static final ResourceLocation LAYER = AwId.of("pyro_heat_pose");
   private static final int PRIORITY = 900;
   public static final float EASE = 0.1F;
   private static final float OFF = 0.005F;
   public static final float ARM_LIFT = -12.0F;
   public static final float ARM_SPLAY = 8.0F;
   public static final float TORSO_LEAN = 3.0F;
   public static final float TREMBLE = 1.2F;
   public static final float TREMBLE_RATE = 1.9F;
   private final AbstractClientPlayer avatar;
   private final float phase;
   private float weight;
   private int tick = Integer.MIN_VALUE;

   private PyroHeatPose(AbstractClientPlayer avatar) {
      this.avatar = avatar;
      this.phase = (float)(avatar.getId() % 16) * 0.4F;
   }

   public static void register() {
      PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 900, PyroHeatPose::new);
   }

   private float weight() {
      if (this.avatar.tickCount != this.tick) {
         this.tick = this.avatar.tickCount;
         float target = 0.0F;
         if (!this.avatar.isUsingItem() && !this.avatar.isInvisible()) {
            target = postureWeight(PyroClientHeat.heat(this.avatar));
         }

         this.weight = this.weight + (target - this.weight) * 0.1F;
         if (target == 0.0F && this.weight < 0.005F) {
            this.weight = 0.0F;
         }
      }

      return this.weight;
   }

   public static float postureWeight(float heat) {
      return Mth.clamp((heat - 0.55F) / 0.14999998F, 0.0F, 1.0F);
   }

   public boolean isActive() {
      return this.weight() > 0.005F;
   }

   public PlayerAnimBone get3DTransform(PlayerAnimBone bone) {
      float w = this.weight();
      if (w <= 0.005F) {
         return bone;
      } else {
         float shake = 1.2F * Mth.sin((float)this.avatar.tickCount * 1.9F + this.phase) * w;
         String var4 = bone.getName();
         switch (var4) {
            case "right_arm":
               bone.addRot(rad(-12.0F * w), 0.0F, rad(8.0F * w + shake));
               break;
            case "left_arm":
               bone.addRot(rad(-12.0F * w), 0.0F, rad(-8.0F * w - shake));
               break;
            case "torso":
               bone.addRot(rad(3.0F * w), 0.0F, 0.0F);
         }

         return bone;
      }
   }

   public FirstPersonMode getFirstPersonMode() {
      return FirstPersonMode.NONE;
   }

   private static float rad(float deg) {
      return (float)Math.toRadians((double)deg);
   }
}
