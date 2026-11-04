package com.awakenedmod.awakened.content.pyrokinetic;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.caster.Caster;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class BackdraftEngine {
   public static final String BALANCE_KEY = BackdraftAbility.class.getSimpleName();
   public static final float COST = 35.0F;
   private static final double RADIUS = 12.0;
   public static final int TAIL_TICKS = 10;
   private static final int COLUMN_INTERVAL = 2;
   private static final double COLUMN_HEIGHT = 6.0;
   private static final double COLUMN_WIDTH = 2.4;
   private static final double PULL_BASE = 0.02;
   private static final double PULL_GAIN = 0.06;
   private static final double PULL_DEAD_ZONE = 1.0;
   private static final int SECOND_SWEEP_TICK = 20;
   private static final int GATHER_CAP = 24;
   private static final int SLOW_REFRESH = 10;
   private static final int SLOW_TICKS = 14;
   private static final int SLOW_AMPLIFIER = 3;
   private static final int PULSE_INTERVAL_START = 10;
   private static final int PULSE_INTERVAL_END = 4;
   private static final float PULSE_PITCH = 0.6F;
   private static final int INHALE_TICK = 39;
   private static final float RUMBLE_MIN = 0.04F;
   private static final float RUMBLE_MAX = 0.12F;
   private static final double SHAKE_RANGE = 20.0;
   private static final int INRUSH_INTERVAL = 2;
   private static final double KNOT_SCALE_START = 0.3;
   private static final double KNOT_SCALE_END = 1.5;
   private static final Vec3 KNOT_LIFT = new Vec3(0.0, 1.0, 0.0);
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
   private static final float BASE_DAMAGE = 12.0F;
   private static final float DAMAGE_PER_SNUFF = 2.0F;
   private static final float SNUFF_BONUS_CAP = 10.0F;
   private static final float EDGE_FRACTION = 0.5F;
   private static final int IGNITE_SECONDS = 4;
   private static final double KNOCKBACK = 1.1;
   private static final double KNOCKBACK_LIFT = 0.45;
   private static final double BURST_VISUAL_RADIUS = 8.0;
   private static final float SLAM_MAGNITUDE = 0.35F;
   private static final int FLASH_TICKS = 8;

   private BackdraftEngine() {
   }

   public static boolean hasAir(Caster caster) {
      return !caster.body().isUnderWater();
   }

   public static BackdraftEngine.Cast begin(Caster caster) {
      ServerLevel level = caster.level();
      FlameTone tone = FlameTone.of(caster);
      Vec3 at = caster.body().position();
      PyroFlames.sound(caster, at, SoundEvents.GENERIC_EXTINGUISH_FIRE, 1.2F, 0.8F, tone);
      PyroFlames.sound(caster, at, SoundEvents.BEACON_DEACTIVATE, 1.2F, 0.5F, tone);
      return new BackdraftEngine.Cast();
   }

   public static boolean tick(Caster caster, BackdraftEngine.Cast cast) {
      ServerLevel level = caster.level();
      Vec3 center = caster.body().position();
      int t = cast.tick++;
      if (t == 43) {
         cast.blast = reignite(caster, level, center, cast.snuffed);
         return true;
      } else if (t > 43) {
         return tail(caster, level, center, t - 43);
      } else {
         starve(caster, level, center, cast, t);
         return true;
      }
   }

   private static void starve(Caster caster, ServerLevel level, Vec3 center, BackdraftEngine.Cast cast, int t) {
      LivingEntity body = caster.body();
      FlameTone tone = FlameTone.of(caster);
      float progress = (float)t / 43.0F;
      Vec3 knot = center.add(KNOT_LIFT);
      if (t % 10 == 0) {
         body.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 14, 3, true, false, false));
      }

      if (t == 0 || t == 20) {
         cast.snuffed = cast.snuffed + extinguishBlocks(level, center, knot, tone);
      }

      int gathered = 0;
      double strength = 0.02 + 0.06 * (double)progress;

      for (LivingEntity e : level.getEntitiesOfClass(
         LivingEntity.class, body.getBoundingBox().inflate(12.0), ex -> ex.isAlive() && ex.distanceToSqr(center) <= 144.0
      )) {
         if (e.isOnFire()) {
            e.clearFire();
            if (e != body && cast.snuffedEntities.add(e.getUUID())) {
               cast.snuffed++;
               if (gathered++ < 24) {
                  PyroFlames.gather(level, e.position().add(0.0, (double)e.getBbHeight() * 0.5, 0.0), knot, tone);
               }
            }
         }

         if (e != body && caster.canHarm(e)) {
            Vec3 toCenter = center.subtract(e.position());
            if (!(toCenter.length() < 1.0)) {
               Vec3 dir = e.onGround() ? new Vec3(toCenter.x, 0.0, toCenter.z) : toCenter;
               if (!(dir.lengthSqr() < 1.0E-6)) {
                  Harm.push(body, e, dir.normalize().scale(strength));
               }
            }
         }
      }

      if (t < 39) {
         if (t >= cast.nextPulse) {
            PyroFlames.sound(caster, center, SoundEvents.WARDEN_HEARTBEAT, 1.0F, 0.6F, tone);
            int interval = Math.round(Mth.lerp(progress, 10.0F, 4.0F));
            Fx.shake(level, center, 20.0, Mth.lerp(progress, 0.04F, 0.12F), 0, interval);
            cast.nextPulse = t + interval;
         }
      } else if (t == 39) {
         PyroFlames.sound(caster, center, SoundEvents.BREEZE_INHALE, 1.2F, 0.8F, tone);
      }

      if (t % 2 == 0) {
         PyroFlames.inrush(level, knot, 12.0, t, progress, tone);
         PyroFlames.knot(level, knot, Mth.lerp((double)progress, 0.3, 1.5), tone);
      }
   }

   private static BackdraftEngine.Blast reignite(Caster caster, ServerLevel level, Vec3 center, int snuffed) {
      LivingEntity body = caster.body();
      PyroHeatState.gainBackdraft(caster);
      FlameTone tone = FlameTone.of(caster);
      float bonus = Math.min(2.0F * (float)snuffed, 10.0F);
      float damage = caster.scaledDamage(12.0F + bonus, BALANCE_KEY, PyroHeatState.heatDamageBonus(caster));
      List<LivingEntity> hit = new ArrayList<>();

      for (LivingEntity victim : level.getEntitiesOfClass(
         LivingEntity.class, body.getBoundingBox().inflate(12.0), e -> e != body && e.isAlive() && e.distanceToSqr(center) <= 144.0 && caster.canHarm(e)
      )) {
         float edge = (float)(victim.position().distanceTo(center) / 12.0);
         float share = Mth.lerp(Mth.clamp(edge, 0.0F, 1.0F), 1.0F, 0.5F);
         victim.igniteForSeconds(4.0F);
         Dmg.hurt(victim, level, caster.damageSource(DamageTypes.ON_FIRE), damage * share);
         Vec3 away = new Vec3(victim.getX() - center.x, 0.0, victim.getZ() - center.z);
         Vec3 dir = away.lengthSqr() < 1.0E-6 ? Vec3.ZERO : away.normalize();
         Harm.push(body, victim, dir.scale(1.1).add(0.0, 0.45, 0.0));
         hit.add(victim);
      }

      Vec3 knot = center.add(KNOT_LIFT);
      PyroFlames.burst(level, knot, 8.0, tone);
      Fx.shake(level, center, 30.0, 0.35F, 4, 16);
      Fx.screenShake(level, center, 18.0, 8, tone.style().mid(), true);
      return new BackdraftEngine.Blast(hit, snuffed, bonus);
   }

   private static boolean tail(Caster caster, ServerLevel level, Vec3 center, int sinceSlam) {
      if (sinceSlam >= 10) {
         return false;
      } else {
         if (sinceSlam % 2 == 0) {
            PyroFlames.stream(level, center.add(KNOT_LIFT), UP, 6.0, 2.4, FlameTone.of(caster));
         }

         return true;
      }
   }

   private static int extinguishBlocks(ServerLevel level, Vec3 center, Vec3 knot, FlameTone tone) {
      BlockPos min = BlockPos.containing(center.x - 12.0, center.y - 12.0, center.z - 12.0);
      BlockPos max = BlockPos.containing(center.x + 12.0, center.y + 12.0, center.z + 12.0);
      int snuffed = 0;
      double r2 = 144.0;

      for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
         if (!(pos.distToCenterSqr(center.x, center.y, center.z) > r2) && level.getBlockState(pos).is(BlockTags.FIRE)) {
            level.removeBlock(pos, false);
            if (snuffed < 24) {
               PyroFlames.gather(level, Vec3.atCenterOf(pos), knot, tone);
            }

            snuffed++;
         }
      }

      return snuffed;
   }

   public static record Blast(List<LivingEntity> hit, int snuffed, float bonus) {
   }

   public static final class Cast {
      private int tick;
      private int snuffed;
      private final Set<UUID> snuffedEntities = new HashSet<>();
      private int nextPulse;
      @Nullable
      private BackdraftEngine.Blast blast;

      @Nullable
      public BackdraftEngine.Blast takeBlast() {
         BackdraftEngine.Blast landed = this.blast;
         this.blast = null;
         return landed;
      }
   }
}
