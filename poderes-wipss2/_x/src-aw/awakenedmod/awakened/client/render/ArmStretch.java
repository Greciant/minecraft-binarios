package com.awakenedmod.awakened.client.render;

import com.zigythebird.playeranim.accessors.IAnimatedPlayer;
import com.zigythebird.playeranimcore.animation.layered.AnimationStack;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import com.zigythebird.playeranimcore.bones.PlayerAnimBone;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ArmStretch {
   public static final float ARM_PX = 12.0F;
   private static final float ARM_TOP_PX = 2.0F;
   private static final float AVATAR_SCALE = 0.9375F;
   private static final float MODEL_LIFT = 1.501F;
   private static final float CROUCH_DROP_PX = 2.0F;
   private static final float BODY_PIVOT = 0.75F;
   private static final String BODY_BONE = "body";
   private static final List<ArmStretch.Source> SOURCES = new CopyOnWriteArrayList<>();

   private ArmStretch() {
   }

   public static void addSource(ArmStretch.Source source) {
      SOURCES.add(source);
   }

   public static ArmStretch.Reach resolve(LivingEntity entity, float partialTick) {
      for (ArmStretch.Source source : SOURCES) {
         ArmStretch.Reach reach = source.reach(entity, partialTick);
         if (reach != null) {
            return reach;
         }
      }

      return null;
   }

   public static Vector3f toModelPx(Vec3 world, double renderX, double renderY, double renderZ, float bodyRotDeg, float scale, @Nullable Matrix4f bodyInverse) {
      double s = scale <= 1.0E-4F ? 1.0 : (double)scale;
      double vx = (world.x - renderX) / s;
      double vy = (world.y - renderY) / s;
      double vz = (world.z - renderZ) / s;
      double theta = Math.toRadians(180.0 - (double)bodyRotDeg);
      double cos = Math.cos(theta);
      double sin = Math.sin(theta);
      Vector3f u = new Vector3f((float)(vx * cos - vz * sin), (float)vy, (float)(vx * sin + vz * cos));
      if (bodyInverse != null) {
         bodyInverse.transformPosition(u);
      }

      double mx = (double)(-u.x / 0.9375F);
      double my = (double)(-u.y / 0.9375F + 1.501F);
      double mz = (double)(u.z / 0.9375F);
      return new Vector3f((float)(mx * 16.0), (float)(my * 16.0), (float)(mz * 16.0));
   }

   @Nullable
   public static Matrix4f bodyInverse(@Nullable AnimationStack manager) {
      if (manager != null && manager.isActive()) {
         PlayerAnimBone bone = manager.get3DTransform(new PlayerAnimBone("body"));
         float px = -bone.getPosX() / 16.0F;
         float py = bone.getPosY() / 16.0F + 0.75F;
         float pz = bone.getPosZ() / 16.0F;
         Matrix4f body = new Matrix4f();
         body.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ())
            .translate(px, py, pz)
            .rotateZYX(bone.getRotZ(), -bone.getRotY(), -bone.getRotX())
            .translate(0.0F, -0.75F, 0.0F);
         return body.invert();
      } else {
         return null;
      }
   }

   public static void pose(ModelPart arm, Vector3f targetPx, float reach) {
      Vector3f dir = new Vector3f(targetPx).sub(arm.x, arm.y, arm.z);
      float full = dir.length();
      if (!(full < 0.001F)) {
         dir.div(full);
         float length = 12.0F + Math.max(0.0F, full - 12.0F) * Mth.clamp(reach, 0.0F, 1.0F);
         float k = length / 12.0F;
         Vector3f euler = new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, dir.x, dir.y, dir.z).getEulerAnglesZYX(new Vector3f());
         arm.xRot = euler.x;
         arm.yRot = euler.y;
         arm.zRot = euler.z;
         arm.yScale = k;
         float slide = 2.0F * (k - 1.0F);
         arm.x = arm.x + dir.x * slide;
         arm.y = arm.y + dir.y * slide;
         arm.z = arm.z + dir.z * slide;
      }
   }

   public static void apply(PlayerModel<?> model, LivingEntity entity, float ageInTicks) {
      ArmStretch.Reach reach = null;
      boolean fpHand = false;
      if (entity instanceof AbstractClientPlayer p) {
         Minecraft mc = Minecraft.getInstance();
         fpHand = p == mc.player && mc.options.getCameraType().isFirstPerson() && !FirstPersonMode.isFirstPersonPass();
         if (!fpHand) {
            float partialTick = ageInTicks - (float)p.tickCount;
            reach = resolve(p, partialTick);
            if (reach != null && (p.isFallFlying() || p.getSwimAmount(partialTick) > 0.0F || p.getPose() == Pose.SLEEPING || p.isAutoSpinAttack())) {
               reach = null;
            }
         }
      }

      if (reach != null && entity instanceof AbstractClientPlayer px) {
         float var18 = ageInTicks - (float)px.tickCount;
         Vec3 var19 = px.getPosition(var18);
         float scale = px.getScale();
         double renderY = var19.y - (px.isCrouching() ? 0.125 * (double)scale : 0.0);
         float bodyRot = Mth.rotLerp(var18, px.yBodyRotO, px.yBodyRot);
         AnimationStack manager = px instanceof IAnimatedPlayer anim ? anim.playerAnimLib$getAnimManager() : null;
         Vector3f target = toModelPx(reach.target(), var19.x, renderY, var19.z, bodyRot, scale, bodyInverse(manager));
         ModelPart arm = reach.leftArm() ? model.leftArm : model.rightArm;
         ModelPart other = reach.leftArm() ? model.rightArm : model.leftArm;
         other.yScale = 1.0F;
         pose(arm, target, reach.reach());
         model.rightSleeve.copyFrom(model.rightArm);
         model.leftSleeve.copyFrom(model.leftArm);
      } else {
         restore(model);
      }
   }

   private static void restore(PlayerModel<?> model) {
      if (model.rightArm.yScale != 1.0F || model.leftArm.yScale != 1.0F) {
         model.rightArm.yScale = 1.0F;
         model.leftArm.yScale = 1.0F;
         model.rightSleeve.copyFrom(model.rightArm);
         model.leftSleeve.copyFrom(model.leftArm);
      }
   }

   public static record Reach(Vec3 target, float reach, boolean leftArm) {
   }

   @FunctionalInterface
   public interface Source {
      ArmStretch.Reach reach(LivingEntity var1, float var2);
   }
}
