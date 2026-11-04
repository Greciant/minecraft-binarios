package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.spider.WebSwingAbility;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.SpiderVisual;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier.PartModifier;
import com.zigythebird.playeranimcore.math.Vec3f;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

public final class SpiderSwingPose {
   private static final ResourceLocation GRIP_RIGHT = AwId.of(WebSwingAbility.gripClip(false));
   private static final ResourceLocation GRIP_LEFT = AwId.of(WebSwingAbility.gripClip(true));
   private static final ResourceLocation CATCH_RIGHT = AwId.of(WebSwingAbility.catchClip(false));
   private static final ResourceLocation CATCH_LEFT = AwId.of(WebSwingAbility.catchClip(true));
   private static final float TILT_SHARE = 0.6F;
   private static final float PITCH_MAX_DEG = 38.0F;
   private static final float ROLL_SHARE = 0.5F;
   private static final float ROLL_MAX_DEG = 24.0F;
   private static final float HEAD_COUNTER = 0.6F;
   private static final float LEG_FALL_DEG = 22.0F;
   private static final float LEG_STREAM_DEG = 12.0F;
   private static final float LEG_RISE_DEG = -40.0F;
   private static final float LEG_STAGGER_SHARE = 0.3F;
   private static final float APEX_LEG_SPREAD_DEG = 12.0F;
   private static final float APEX_ARM_SPREAD_DEG = 35.0F;
   private static final float FALL_ARM_BACK_DEG = 20.0F;
   private static final double VERTICAL_FULL = 0.8;
   private static final double SPEED_FULL = 1.6;
   private static final float EASE = 0.25F;
   private static final int STALE_TICKS = 2;
   private static final int PITCH = 0;
   private static final int ROLL = 1;
   private static final int LEGS = 2;
   private static final int LEG_SPREAD = 3;
   private static final int ARM_OUT = 4;
   private static final int ARM_BACK = 5;
   private static final int CHANNELS = 6;
   private static final Map<Integer, SpiderSwingPose.Pose> POSES = new HashMap<>();
   private static int clientTick;

   private SpiderSwingPose() {
   }

   public static void register(IEventBus gameBus) {
      gameBus.addListener(SpiderSwingPose::onClientTick);
      AwakenedAnimations.addPoseModifier(SpiderSwingPose::bone);
   }

   private static void onClientTick(Post event) {
      clientTick++;
      if (Minecraft.getInstance().level == null) {
         POSES.clear();
      } else {
         POSES.values().removeIf(p -> clientTick - p.seen > 2);
      }
   }

   private static Optional<PartModifier> bone(String bone, Entity avatar, boolean firstPerson, float partialTick) {
      if (!firstPerson && avatar instanceof Player player) {
         SpiderSwingPose.Pose p = state(player);
         if (p == null) {
            return Optional.empty();
         } else {
            float pitch = p.at(0, partialTick);
            float legs = p.at(2, partialTick);
            float legSpread = p.at(3, partialTick);
            float freeSign = p.freeLeft ? -1.0F : 1.0F;

            return switch (bone) {
               case "body" -> part(pitch, 0.0F, p.at(1, partialTick));
               case "head" -> part(-pitch * 0.6F, 0.0F, 0.0F);
               case "right_leg" -> part(legs * 1.3F, 0.0F, legSpread);
               case "left_leg" -> part(legs, 0.0F, -legSpread);
               case "right_arm" -> p.freeLeft ? Optional.empty() : part(p.at(5, partialTick), 0.0F, p.at(4, partialTick) * freeSign);
               case "left_arm" -> p.freeLeft ? part(p.at(5, partialTick), 0.0F, p.at(4, partialTick) * freeSign) : Optional.empty();
               default -> Optional.empty();
            };
         }
      } else {
         return Optional.empty();
      }
   }

   private static SpiderSwingPose.Pose state(Player player) {
      int id = player.getId();
      SpiderSwingPose.Pose p = POSES.get(id);
      if (p != null && player.tickCount == p.tick) {
         p.seen = clientTick;
         return p;
      } else {
         SpiderVisual v = (SpiderVisual)player.getData(AwakenedAttachments.SPIDER_VISUAL);
         boolean onLine = v.tether() == 1 && !v.hanging();
         if (p == null && !onLine) {
            return null;
         } else {
            boolean right = onLine && (AwakenedAnimations.isTriggered(id, GRIP_RIGHT) || AwakenedAnimations.isTriggered(id, CATCH_RIGHT));
            boolean on = onLine && (right || AwakenedAnimations.isTriggered(id, GRIP_LEFT) || AwakenedAnimations.isTriggered(id, CATCH_LEFT));
            if (p == null) {
               if (!on) {
                  return null;
               }

               p = new SpiderSwingPose.Pose();
               POSES.put(id, p);
            }

            p.seen = clientTick;
            p.tick = player.tickCount;
            System.arraycopy(p.cur, 0, p.prev, 0, 6);
            if (!on) {
               ease(p, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
               if (Math.abs(p.cur[0]) + Math.abs(p.cur[1]) + Math.abs(p.cur[2]) < 0.2F) {
                  POSES.remove(id);
                  return null;
               } else {
                  return p;
               }
            } else {
               p.freeLeft = right;
               float yaw = (float)Math.toRadians((double)player.yBodyRot);
               Vec3 forward = new Vec3((double)(-Mth.sin(yaw)), 0.0, (double)Mth.cos(yaw));
               Vec3 rightDir = new Vec3((double)(-Mth.cos(yaw)), 0.0, (double)(-Mth.sin(yaw)));
               Vec3 rope = new Vec3(v.ax(), v.ay(), v.az()).subtract(player.position().add(0.0, 1.0, 0.0));
               double up = Math.max(rope.y, 0.1);
               float pitch = (float)Math.toDegrees(Math.atan2(rope.dot(forward), up)) * 0.6F;
               float roll = (float)Math.toDegrees(Math.atan2(rope.dot(rightDir), up)) * 0.5F;
               Vec3 moved = player.position().subtract(player.xo, player.yo, player.zo);
               float fall = (float)Mth.clamp(-moved.y / 0.8, 0.0, 1.0);
               float rise = (float)Mth.clamp(moved.y / 0.8, 0.0, 1.0);
               float fast = (float)Mth.clamp(moved.length() / 1.6, 0.0, 1.0);
               float apex = (1.0F - fast) * (1.0F - fall);
               float legs = 22.0F * fall + 12.0F * fast * (1.0F - rise) + -40.0F * rise;
               ease(p, Mth.clamp(pitch, -38.0F, 38.0F), Mth.clamp(roll, -24.0F, 24.0F), legs, 12.0F * apex, 35.0F * apex, 20.0F * fall);
               return p;
            }
         }
      }
   }

   private static void ease(SpiderSwingPose.Pose p, float... targets) {
      for (int i = 0; i < 6; i++) {
         p.cur[i] = p.cur[i] + (targets[i] - p.cur[i]) * 0.25F;
      }
   }

   private static Optional<PartModifier> part(float xDeg, float yDeg, float zDeg) {
      return Optional.of(new PartModifier(new Vec3f(rad(xDeg), rad(yDeg), rad(zDeg)), Vec3f.ZERO));
   }

   private static float rad(float deg) {
      return (float)Math.toRadians((double)deg);
   }

   private static final class Pose {
      final float[] cur = new float[6];
      final float[] prev = new float[6];
      boolean freeLeft;
      int tick = Integer.MIN_VALUE;
      int seen;

      float at(int channel, float partialTick) {
         return this.prev[channel] + (this.cur[channel] - this.prev[channel]) * partialTick;
      }
   }
}
