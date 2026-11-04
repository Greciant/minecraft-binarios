package com.awakenedmod.awakened.client;

import com.awakenedmod.awakened.content.velocity.VelocityVisualState;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.data.VelocityVisual;
import com.zigythebird.playeranimcore.animation.layered.modifier.AdjustmentModifier.PartModifier;
import com.zigythebird.playeranimcore.math.Vec3f;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class VelocityRunPose {
   private static final float LEAN_PER_GEAR_DEG = 4.0F;
   private static final float LEAN_MAX_DEG = 20.0F;
   private static final float EASE = 0.2F;
   private static final float ARM_SWING_GAIN_PER_GEAR = 0.25F;
   private static final float ARM_EXTRA_DEG_AT_MAX = 10.0F;
   private static final float VANILLA_SWING_RATE = 0.6662F;
   private static final float VANILLA_SWING_DEG = 57.2958F;
   private static final int STALE_TICKS = 2;
   private static final ResourceLocation RUN_CLIP = AwId.of("velocity_run");
   private static final Map<Integer, VelocityRunPose.Lean> LEAN = new HashMap<>();
   private static int clientTick;

   private VelocityRunPose() {
   }

   static void tick(Minecraft mc) {
      clientTick++;
      if (mc.level == null) {
         LEAN.clear();
      } else {
         LEAN.values().removeIf(lean -> clientTick - lean.seen > 2);
      }
   }

   public static Optional<PartModifier> bone(String bone, Entity avatar, boolean firstPerson, float partialTick) {
      if (avatar instanceof LivingEntity living) {
         VelocityRunPose.Lean s = state(living);
         if (s != null && !firstPerson) {
            return switch (bone) {
               case "body" -> Optional.of(new PartModifier(new Vec3f(rad(s.pitch), 0.0F, 0.0F), Vec3f.ZERO));
               case "head" -> Optional.of(new PartModifier(new Vec3f(-rad(s.pitch), 0.0F, 0.0F), Vec3f.ZERO));
               case "right_arm" -> Optional.of(new PartModifier(new Vec3f(-armSwing(living, s, partialTick), 0.0F, 0.0F), Vec3f.ZERO));
               case "left_arm" -> Optional.of(new PartModifier(new Vec3f(armSwing(living, s, partialTick), 0.0F, 0.0F), Vec3f.ZERO));
               default -> Optional.empty();
            };
         } else {
            return Optional.empty();
         }
      } else {
         return Optional.empty();
      }
   }

   private static VelocityRunPose.Lean state(LivingEntity avatar) {
      VelocityVisual v = VelocityVisualState.of(avatar);
      int id = avatar.getId();
      boolean on = v.running() && AwakenedAnimations.isTriggered(id, RUN_CLIP);
      VelocityRunPose.Lean s = LEAN.get(id);
      if (s == null) {
         if (!on) {
            return null;
         }

         s = new VelocityRunPose.Lean();
         s.walkPos = avatar.walkAnimation.position();
         LEAN.put(id, s);
      }

      s.seen = clientTick;
      if (avatar.tickCount != s.tick) {
         s.tick = avatar.tickCount;
         int gear = Mth.clamp(v.gear(), 0, 5);
         float pitchT = on ? Math.min(20.0F, 4.0F * (float)gear) : 0.0F;
         s.pitch = s.pitch + (pitchT - s.pitch) * 0.2F;
         float pos = avatar.walkAnimation.position();
         s.phase = s.phase + (pos - s.walkPos) * 0.6662F * swingGain(gear);
         s.walkPos = pos;
         if (!on && Math.abs(s.pitch) < 0.05F) {
            LEAN.remove(id);
            return null;
         }
      }

      return s;
   }

   private static float swingGain(int gear) {
      return 1.0F + 0.25F * (float)gear;
   }

   private static float armSwing(LivingEntity avatar, VelocityRunPose.Lean s, float partialTick) {
      int gear = Mth.clamp(VelocityVisualState.of(avatar).gear(), 0, 5);
      float sub = (avatar.walkAnimation.position(partialTick) - s.walkPos) * 0.6662F * swingGain(gear);
      float amount = Math.min(1.0F, avatar.walkAnimation.speed(partialTick));
      float amplitudeDeg = 57.2958F + 10.0F * (float)gear / 5.0F;
      return Mth.cos(s.phase + sub) * amount * rad(amplitudeDeg);
   }

   private static float rad(float deg) {
      return (float)Math.toRadians((double)deg);
   }

   private static final class Lean {
      float pitch;
      float phase;
      float walkPos;
      int tick = Integer.MIN_VALUE;
      int seen;
   }
}
