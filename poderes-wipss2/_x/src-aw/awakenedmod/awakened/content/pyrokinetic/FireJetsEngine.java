package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.caster.Caster;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class FireJetsEngine {
   public static final String BALANCE_KEY = FireJetsAbility.class.getSimpleName();
   public static final float TAP_COST = 10.0F;
   public static final float DRAIN_PER_TICK = 0.6F;
   public static final int HOLD_START_TICKS = 6;
   private static final double BURST_SPEED = 1.15;
   private static final double GROUND_LIFT = 0.35;
   private static final double ACCEL_IGNITE = 0.06;
   private static final double ACCEL_FULL = 0.18;
   private static final double AIR_DRAG = 0.9;
   private static final double TURN_RATE = 0.45;
   private static final double STRAFE_ACCEL = 0.45;
   private static final double LIFT_ACCEL = 0.35;
   private static final double GRAVITY = 0.08;
   private static final double CRUISE_ENTER = 0.55;
   private static final double CRUISE_LEAVE = 0.3;
   private static final double CRUISE_SMOOTHING = 0.4;

   private FireJetsEngine() {
   }

   public static FireJetsEngine.Jet burst(Caster caster, Vec3 aim) {
      LivingEntity body = caster.body();
      Vec3 velocity = aim.scale(1.15);
      if (body.onGround()) {
         velocity = new Vec3(velocity.x, Math.max(velocity.y + 0.2, 0.35), velocity.z);
      }

      body.setDeltaMovement(velocity);
      body.hurtMarked = true;
      body.resetFallDistance();
      FireJetsEngine.Jet jet = new FireJetsEngine.Jet(aim, 1.15, body.position());
      ServerLevel level = caster.level();
      Vec3 at = body.position();
      FlameTone style = FlameTone.of(caster);
      PyroFlames.sound(caster, at, SoundEvents.BLAZE_SHOOT, 0.9F, 1.15F, style);
      PyroFlames.sound(caster, at, SoundEvents.FIRECHARGE_USE, 0.7F, 0.55F, style);
      PyroFlames.sound(caster, at, SoundEvents.BLAZE_SHOOT, 0.6F, 0.7F, style);
      PyroFlames.sound(caster, at, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), 0.3F, 0.8F, style);
      PyroHeatState.gainJetTap(caster);
      return jet;
   }

   public static int advance(FireJetsEngine.Jet jet) {
      return jet.tick++;
   }

   public static boolean thrust(Caster caster, FireJetsEngine.Jet jet, int t, FireJetsEngine.Steering steering) {
      LivingEntity body = caster.body();
      if (t >= 6) {
         PyroHeatState.gainJetTick(caster);
      }

      double throttle = Math.min(1.0, (double)(t + 1) / 15.0);
      double accel = Mth.lerp(throttle, 0.06, 0.18);
      Vec3 heading = jet.carve(steering.direction());
      jet.speed = Math.min(1.3, jet.speed * 0.9 + accel);
      double strafe = steering.strafe();
      double lift = steering.lift();
      jet.strafe = jet.strafe * 0.9 + 0.45 * accel * strafe;
      jet.lift = jet.lift * 0.9 + 0.35 * accel * lift;
      Vec3 velocity = heading.scale(jet.speed).add(AimUtil.horizontalRight((double)caster.yaw()).scale(-jet.strafe)).add(0.0, 0.08 + jet.lift, 0.0);
      if (body.onGround()) {
         velocity = new Vec3(velocity.x, Math.max(velocity.y, 0.35), velocity.z);
      }

      if (velocity.length() > 1.3) {
         velocity = velocity.normalize().scale(1.3);
      }

      body.setDeltaMovement(velocity);
      body.hurtMarked = true;
      body.resetFallDistance();
      Vec3 pos = body.position();
      double moved = pos.distanceTo(jet.lastPos);
      jet.lastPos = pos;
      jet.moved = moved;
      jet.commanded = velocity.length();
      jet.cruiseTravel = jet.cruiseTravel + (moved - jet.cruiseTravel) * 0.4;
      boolean cruising = t >= 6 && (jet.cruising ? jet.cruiseTravel >= 0.3 : jet.cruiseTravel >= 0.55);
      if (cruising != jet.cruising) {
         jet.cruising = cruising;
         return true;
      } else {
         return false;
      }
   }

   public static void land(Caster caster) {
      PyroFlames.sound(caster, caster.body().position(), SoundEvents.FIRE_EXTINGUISH, 0.6F, 1.1F, FlameTone.of(caster));
   }

   public static final class Jet {
      private int tick;
      private Vec3 heading;
      private double speed;
      private double strafe;
      private double lift;
      private Vec3 lastPos;
      private double moved;
      private double commanded;
      private double cruiseTravel;
      private boolean cruising;

      Jet(Vec3 heading, double speed, Vec3 pos) {
         this.heading = heading;
         this.speed = speed;
         this.lastPos = pos;
      }

      public int age() {
         return this.tick;
      }

      public boolean engaged() {
         return this.tick > 6;
      }

      public double moved() {
         return this.moved;
      }

      public double commanded() {
         return this.commanded;
      }

      public boolean cruising() {
         return this.cruising;
      }

      Vec3 carve(Vec3 look) {
         Vec3 nd = this.heading.add(look.subtract(this.heading).scale(0.45));
         if (nd.lengthSqr() < 1.0E-6) {
            nd = look;
         }

         this.heading = nd.normalize();
         return this.heading;
      }
   }

   public interface Steering {
      Vec3 direction();

      double strafe();

      double lift();
   }
}
