package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.caster.Caster;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class FlameBeamEngine {
   public static final String BALANCE_KEY = FlameBeamAbility.class.getSimpleName();
   public static final float UPFRONT_COST = 6.0F;
   public static final float DRAIN_PER_TICK = 0.45F;
   private static final float PULSE_DAMAGE_START = 0.75F;
   private static final float PULSE_DAMAGE_MAX = 2.0F;
   private static final int IGNITE_SECONDS = 2;
   private static final double FIRE_SPEED = 1.15;
   private static final int DWELL_MAX_TICKS = 4;
   private static final int FLARE_INTERVAL = 5;
   private static final int IMPACT_SOUND_INTERVAL = 10;

   private FlameBeamEngine() {
   }

   public static void ignite(Caster caster) {
      PyroBeamSounds.play(caster, caster.body().position(), 0.0F, 1.0F, FlameTone.of(caster), PyroBeamSounds.IGNITION);
   }

   @Nullable
   public static LivingEntity tick(Caster caster, FlameBeamEngine.Channel channel) {
      ServerLevel level = caster.level();
      int t = channel.tick++;
      PyroHeatState.gainBeamTick(caster);
      FlameTone style = FlameTone.of(caster);
      Vec3 eye = caster.eyePos();
      Vec3 end = FlameBeamAbility.beamEnd(level, caster.body(), eye, caster.aimDir());
      FlameBeamAbility.LineHit hit = FlameBeamAbility.firstOnLine(level, caster.body(), eye, end, caster::canHarm);
      LivingEntity victim = hit == null ? null : hit.entity();
      boolean fireArrived = channel.dwellSatisfied(victim, hit == null ? 0.0 : hit.dist(), t);
      LivingEntity fullBurn = null;
      if (t >= channel.nextPulse && victim != null && fireArrived) {
         channel.nextPulse = t + 5;
         float ramp = Mth.clamp((float)t / 60.0F, 0.0F, 1.0F);
         float base = Mth.lerp(ramp, 0.75F, 2.0F);
         float damage = caster.scaledDamage(base, BALANCE_KEY, PyroHeatState.heatDamageBonus(caster));
         victim.igniteForSeconds(2.0F);
         if (Dmg.hurtRapid(victim, level, caster.damageSource(DamageTypes.ON_FIRE), damage)) {
            victim.hurtMarked = true;
            caster.onHit(victim);
            if (ramp >= 1.0F) {
               fullBurn = victim;
            }
         }
      }

      if (t % 5 == 0) {
         PyroFlames.impactFlare(level, end, style);
      }

      if (t % 10 == 0) {
         impactSound(caster, eye, end, victim != null && fireArrived ? victim : null, Mth.clamp((float)t / 60.0F, 0.0F, 1.0F), style);
      }

      return fullBurn;
   }

   private static void impactSound(Caster caster, Vec3 eye, Vec3 end, LivingEntity victim, float ramp, FlameTone style) {
      float body = 0.7F + 0.3F * ramp;
      if (victim != null) {
         Vec3 at = victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0);
         PyroBeamSounds.play(caster, at, ramp, body, style, PyroBeamSounds.BODY_HIT);
      } else if (end.distanceToSqr(eye) < 399.999) {
         PyroBeamSounds.play(caster, end, ramp, body, style, PyroBeamSounds.WALL_HIT);
      }
   }

   public static void shutdown(Caster caster, FlameBeamEngine.Channel channel, boolean dry) {
      float ramp = Mth.clamp((float)channel.tick / 60.0F, 0.0F, 1.0F);
      PyroBeamSounds.play(caster, caster.body().position(), ramp, 1.0F, FlameTone.of(caster), dry ? PyroBeamSounds.SHUTDOWN_DRY : PyroBeamSounds.SHUTDOWN);
   }

   public static final class Channel {
      private int tick;
      private int nextPulse;
      private boolean fullBurnReported;
      private LivingEntity onLine;
      private int onLineSince;

      public int age() {
         return this.tick;
      }

      public boolean fullBurnReported() {
         return this.fullBurnReported;
      }

      public void markFullBurnReported() {
         this.fullBurnReported = true;
      }

      boolean dwellSatisfied(LivingEntity victim, double dist, int t) {
         if (victim != this.onLine) {
            this.onLine = victim;
            this.onLineSince = t;
         }

         int dwell = Math.min(Mth.ceil(dist / 1.15), 4);
         return victim != null && t - this.onLineSince >= dwell;
      }
   }
}
