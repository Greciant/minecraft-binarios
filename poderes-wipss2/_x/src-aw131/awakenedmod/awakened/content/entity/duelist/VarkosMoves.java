package com.awakenedmod.awakened.content.entity.duelist;

import com.awakenedmod.awakened.content.entity.ThreatMoves;
import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.Fx;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class VarkosMoves {
   private static final float DAGGER_SLASH_DMG = 4.0F;
   private static final int DAGGER_SLASHES = 8;
   private static final double DAGGER_REACH = 3.5;
   private static final double DAGGER_DRIFT_SPEED = 0.09;
   private static final double DAGGER_DRIFT_TOTAL = 2.5;
   private static final double DAGGER_DRIFT_STANDOFF = 1.0;
   private static final float IAI_DMG = 14.0F;
   private static final double IAI_RADIUS = 2.5;
   private static final int IAI_FLIGHT_TICKS = 10;
   private static final float HAMMER_DMG = 30.0F;
   private static final double HAMMER_SPEED = 1.2;
   private static final double HAMMER_WIDTH = 1.2;
   private static final double HAMMER_OVERSHOOT = 3.0;
   private static final double HAMMER_FAN_DEG = 20.0;
   private static final int HAMMER_LEAD_CAP = 35;
   private static final int HESITATION_GRACE = 15;
   private static final double HESITATION_CUT = 0.4;
   public static final int VIOLET_COLOR = 11563263;
   public static final DustParticleOptions VIOLET = Fx.dust(11563263, 1.3F);
   static volatile int iaiHold = 20;
   public static final VarkosMove DAGGER_FLURRY = new VarkosMove() {
      @Override
      public byte action() {
         return 1;
      }

      @Override
      public int windup() {
         return 20;
      }

      @Override
      public int recover() {
         return 55;
      }

      @Override
      public double minRange() {
         return 0.0;
      }

      @Override
      public double maxRange() {
         return 5.0;
      }

      @Override
      public int weight() {
         return 30;
      }

      @Override
      public boolean rooted() {
         return true;
      }

      @Override
      public int lookLockAt() {
         return this.windup() - 4;
      }

      @Override
      public int retrackAt() {
         return Integer.MAX_VALUE;
      }

      @Override
      public float lockedTurnSpeed() {
         return 6.0F;
      }

      @Override
      public SoundEvent commitCue() {
         return SoundEvents.PLAYER_ATTACK_KNOCKBACK;
      }

      @Override
      public float commitCuePitch() {
         return 1.5F;
      }

      @Override
      public double dangerRadius() {
         return 3.5;
      }

      public boolean canStart(VarkosEntity self, ServerLevel level, LivingEntity target) {
         return self.distanceToSqr(target) <= 25.0;
      }

      public void windupTick(VarkosEntity self, ServerLevel level, LivingEntity target, int tick) {
         Fx.burst(
            level,
            VarkosMoves.VIOLET,
            self.position().add(VarkosMoves.forwardFlat(self).scale(0.5)).add(0.0, (double)self.getBbHeight() * 0.4, 0.0),
            2,
            0.25,
            0.02
         );
         int drawTick = Math.round((float)self.currentWindup() * 0.45F);
         if (tick == drawTick || tick == drawTick + 2) {
            Fx.sound(level, self.position(), (SoundEvent)SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.HOSTILE, 0.9F, tick == drawTick ? 1.55F : 1.8F);
         }
      }

      public void strike(VarkosEntity self, ServerLevel level, LivingEntity target) {
         self.flurryDriftLeft = 2.5;
         VarkosMoves.slashArc(self, level, 3.5, 65.0, 4.0F);
         Fx.sound(level, self.position(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.0F, 1.4F);
      }

      public void recoverTick(VarkosEntity self, ServerLevel level, LivingEntity target, int ticksSinceStrike) {
         if (ticksSinceStrike <= 30) {
            if (target != null) {
               VarkosMoves.driveFlurryDrift(self, target);
            }

            int interval = Math.round(3.75F);
            if (interval > 0 && ticksSinceStrike % interval == 0) {
               VarkosMoves.slashArc(self, level, 3.5, 65.0, 4.0F);
               Fx.sound(level, self.position(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.9F, 1.3F + self.getRandom().nextFloat() * 0.4F);
            }
         }
      }

      public boolean finishedEarly(VarkosEntity self, int moveTick) {
         return VarkosMoves.hesitationCut(self, moveTick, 30);
      }
   };
   public static final VarkosMove IAI_DASH = new VarkosMove() {
      @Override
      public byte action() {
         return 4;
      }

      @Override
      public int windup() {
         return Math.max(10, Math.min(30, VarkosMoves.iaiHold));
      }

      @Override
      public int recover() {
         return 40;
      }

      @Override
      public double minRange() {
         return 4.0;
      }

      @Override
      public double maxRange() {
         return 20.0;
      }

      @Override
      public int weight() {
         return 30;
      }

      @Override
      public boolean rooted() {
         return true;
      }

      @Override
      public int lookLockAt() {
         return this.windup();
      }

      @Override
      public int retrackAt() {
         return Integer.MAX_VALUE;
      }

      @Override
      public boolean fixedTempo() {
         return true;
      }

      @Override
      public SoundEvent windupCue() {
         return (SoundEvent)SoundEvents.CROSSBOW_LOADING_END.value();
      }

      @Override
      public float windupCuePitch() {
         return 1.1F;
      }

      @Override
      public float windupCueVolume() {
         return 0.8F;
      }

      @Override
      public SoundEvent commitCue() {
         return SoundEvents.PLAYER_ATTACK_SWEEP;
      }

      @Override
      public float commitCuePitch() {
         return 0.6F;
      }

      @Override
      public double dangerRadius() {
         return 2.5;
      }

      @Override
      public boolean dangerAtTarget() {
         return true;
      }

      public boolean canStart(VarkosEntity self, ServerLevel level, LivingEntity target) {
         return target.onGround();
      }

      public void windupTick(VarkosEntity self, ServerLevel level, LivingEntity target, int tick) {
         if (tick == 1 || tick == self.currentWindup() - 1) {
            Fx.burst(
               level,
               VarkosMoves.VIOLET,
               self.position().add(VarkosMoves.forwardFlat(self).scale(0.4)).add(0.0, (double)self.getBbHeight() * 0.35, 0.0),
               4,
               0.2,
               0.01
            );
         }
      }

      public void strike(VarkosEntity self, ServerLevel level, LivingEntity target) {
         self.iaiChainArmed = false;
         VarkosMoves.driveDash(self, VarkosMoves.dashDest(self, target), true);
         Fx.sound(level, self.position(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.4F, 0.5F);
         Fx.burst(level, VarkosMoves.VIOLET, self.position().add(0.0, 0.3, 0.0), 12, 0.4, 0.06);
      }

      public void recoverTick(VarkosEntity self, ServerLevel level, LivingEntity target, int ticksSinceStrike) {
         if (ticksSinceStrike < 10) {
            VarkosMoves.driveDash(self, VarkosMoves.dashDest(self, target), false);
         } else {
            if (ticksSinceStrike == 10) {
               Vec3 center = VarkosMoves.dashDest(self, target);
               boolean hit = false;
               AABB area = new AABB(center, center).inflate(3.5, 2.5, 3.5);

               for (Player victim : level.getEntitiesOfClass(Player.class, area, VarkosEntity::isHostileTarget)) {
                  Vec3 p = new Vec3(victim.getX(), center.y, victim.getZ());
                  if (p.distanceToSqr(center) <= 6.25 && self.hurtChallenger(level, victim, 14.0F)) {
                     hit = true;
                     Fx.burst(level, ParticleTypes.CRIT, victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0), 10, 0.4, 0.1);
                  }
               }

               Fx.sound(level, center, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 1.4F, hit ? 0.9F : 0.6F);
               Vfx.ring(level, center.add(0.0, 0.1, 0.0), 2.5, 11563263, 12);
               Fx.burst(level, VarkosMoves.VIOLET, center.add(0.0, 0.3, 0.0), 14, 1.5, 0.05);
               if (hit) {
                  self.tryArmIaiChain();
               }
            }
         }
      }

      public boolean finishedEarly(VarkosEntity self, int moveTick) {
         return self.iaiChainArmed && moveTick - self.currentWindup() >= 12 ? true : VarkosMoves.hesitationCut(self, moveTick, 10);
      }
   };
   public static final VarkosMove HAMMER_SLAM = new VarkosMove() {
      @Override
      public byte action() {
         return 9;
      }

      @Override
      public int windup() {
         return 28;
      }

      @Override
      public int recover() {
         return 26;
      }

      @Override
      public double minRange() {
         return 9.0;
      }

      @Override
      public double maxRange() {
         return 48.0;
      }

      @Override
      public int weight() {
         return 30;
      }

      @Override
      public boolean rooted() {
         return true;
      }

      @Override
      public int lookLockAt() {
         return this.windup() - 5;
      }

      @Override
      public int retrackAt() {
         return Integer.MAX_VALUE;
      }

      @Override
      public SoundEvent windupCue() {
         return (SoundEvent)SoundEvents.ARMOR_EQUIP_NETHERITE.value();
      }

      @Override
      public float windupCuePitch() {
         return 0.5F;
      }

      @Override
      public float windupCueVolume() {
         return 1.1F;
      }

      @Override
      public SoundEvent commitCue() {
         return SoundEvents.ANVIL_LAND;
      }

      @Override
      public float commitCuePitch() {
         return 0.7F;
      }

      public void windupTick(VarkosEntity self, ServerLevel level, LivingEntity target, int tick) {
         if (tick % 4 == 0) {
            Fx.burst(level, VarkosMoves.VIOLET, self.position().add(0.0, (double)self.getBbHeight() + 0.6, 0.0), 3, 0.3, 0.03);
         }

         if (tick == 2) {
            Fx.sound(level, self.position(), SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.HOSTILE, 0.9F, 0.55F);
         }

         if (tick == self.currentWindup() - 8) {
            Fx.sound(level, self.position(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 1.4F, 0.6F);
         }
      }

      public void strike(VarkosEntity self, ServerLevel level, LivingEntity target) {
         Vec3 origin = self.position();
         double floorY = HammerCrack.floorY(level, origin);
         origin = new Vec3(origin.x, floorY, origin.z);
         Vec3 lockPoint = VarkosMoves.ledPoint(self, target, origin);
         Vec3 flat = new Vec3(lockPoint.x - origin.x, 0.0, lockPoint.z - origin.z);
         double lockDist = flat.length();
         Vec3 dir = lockDist < 1.0E-4 ? VarkosMoves.forwardFlat(self) : flat.normalize();
         double length = lockDist + 3.0;
         Set<UUID> slamHits = new HashSet<>();
         self.watchSlamForFutility(slamHits, (double)self.distanceTo(target));
         self.spawnHammerCrack(new HammerCrack.Crack(origin, dir, length, 1.2, 1.2, 30.0F, slamHits));
         if (self.escalationPhase() >= 2) {
            float spread = (float)Math.toRadians(20.0);
            self.spawnHammerCrack(new HammerCrack.Crack(origin, dir.yRot(spread), length, 1.2, 1.2, 30.0F, slamHits));
            self.spawnHammerCrack(new HammerCrack.Crack(origin, dir.yRot(-spread), length, 1.2, 1.2, 30.0F, slamHits));
         }

         ThreatMoves.shockwave(level, self, 2.0, 0.0F, 0.0F, 0, 0.2, 0.1);
         Fx.sound(level, self.position(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.6F, 0.6F);
         Fx.shake(level, self.position(), 16.0, 0.3F, 0, 12);
         Vfx.ring(level, origin.add(0.0, 0.1, 0.0), 1.5, 11563263, 12);
      }
   };
   public static final List<VarkosMove> KIT_P1 = List.of(IAI_DASH, HAMMER_SLAM);
   public static final List<VarkosMove> KIT_P2 = List.of(DAGGER_FLURRY, IAI_DASH, HAMMER_SLAM);

   private VarkosMoves() {
   }

   private static Vec3 chest(VarkosEntity self) {
      return self.position().add(0.0, (double)self.getBbHeight() * 0.5, 0.0);
   }

   private static Vec3 forwardFlat(VarkosEntity self) {
      Vec3 look = self.getLookAngle();
      Vec3 fwd = new Vec3(look.x, 0.0, look.z);
      return fwd.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : fwd.normalize();
   }

   private static boolean hesitationCut(VarkosEntity self, int moveTick, int activeRecoverTicks) {
      int w = self.currentWindup();
      int total = self.currentTotal();
      int window = total - w - activeRecoverTicks;
      int cut = (int)((double)window * 0.4);
      return cut > 0 && moveTick - w > activeRecoverTicks && moveTick >= total - cut && !self.hurtWithin(15);
   }

   private static void slashArc(VarkosEntity self, ServerLevel level, double reach, double halfAngleDeg, float damage) {
      Vec3 origin = chest(self);
      Vec3 fwd = forwardFlat(self);
      double cos = Math.cos(Math.toRadians(halfAngleDeg));
      double selfHalf = (double)self.getBbWidth() * 0.5;

      for (Player victim : level.getEntitiesOfClass(Player.class, self.getBoundingBox().inflate(reach + 1.0), VarkosEntity::isHostileTarget)) {
         Vec3 to = victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0).subtract(origin);
         if (!(Math.abs(to.y) > 2.0)) {
            Vec3 flatTo = new Vec3(to.x, 0.0, to.z);
            double gap = flatTo.length() - selfHalf - (double)victim.getBbWidth() * 0.5;
            if (!(gap > reach) && (!(flatTo.lengthSqr() > 1.0E-4) || !(flatTo.normalize().dot(fwd) < cos)) && self.hurtChallenger(level, victim, damage)) {
               Fx.burst(level, ParticleTypes.CRIT, victim.position().add(0.0, (double)victim.getBbHeight() * 0.5, 0.0), 5, 0.25, 0.06);
            }
         }
      }

      Vec3 bladeMid = origin.add(fwd.scale(reach * 0.6));
      Fx.burst(level, ParticleTypes.SWEEP_ATTACK, bladeMid, 1, 0.2, 0.0);
      Fx.burst(level, VIOLET, bladeMid, 3, 0.3, 0.02);
   }

   private static Vec3 dashDest(VarkosEntity self, @Nullable LivingEntity target) {
      Vec3 snap = self.committedAim();
      if (snap != null) {
         return new Vec3(snap.x, self.getY(), snap.z);
      } else {
         return target != null ? target.position() : self.position();
      }
   }

   private static void driveFlurryDrift(VarkosEntity self, LivingEntity target) {
      if (!(self.flurryDriftLeft <= 0.0)) {
         Vec3 flat = new Vec3(target.getX() - self.getX(), 0.0, target.getZ() - self.getZ());
         double gap = flat.length() - (double)self.getBbWidth() * 0.5 - (double)target.getBbWidth() * 0.5;
         double step = Math.min(0.09, Math.min(self.flurryDriftLeft, gap - 1.0));
         if (!(step <= 0.0)) {
            Vec3 dir = flat.normalize();
            self.setDeltaMovement(dir.x * step, self.getDeltaMovement().y, dir.z * step);
            self.hurtMarked = true;
            self.flurryDriftLeft -= step;
         }
      }
   }

   private static void driveDash(VarkosEntity self, Vec3 dest, boolean launch) {
      Vec3 flat = new Vec3(dest.x - self.getX(), 0.0, dest.z - self.getZ());
      double dist = flat.length();
      if (launch) {
         self.iaiDashSpeed = Math.min(2.2, dist / 10.0);
      }

      double step = Math.min(self.iaiDashSpeed, dist);
      Vec3 dir = dist < 1.0E-4 ? Vec3.ZERO : flat.normalize();
      self.setDeltaMovement(dir.x * step, self.getDeltaMovement().y, dir.z * step);
      if (launch) {
         self.hurtMarked = true;
      }

      if (dist > 0.5) {
         self.steerCommittedFacing((float)Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0F);
      }
   }

   private static Vec3 ledPoint(VarkosEntity self, LivingEntity target, Vec3 origin) {
      Vec3 anchor = self.moveAnchor();
      if (anchor == null) {
         return target.position();
      } else {
         int elapsed = Math.max(1, self.currentWindup());
         double lead = Math.min(35.0, horizDist(origin, target.position()) / 1.2);
         Vec3 led = ThreatMoves.predictPos(target, anchor, elapsed, (float)lead);
         lead = Math.min(35.0, horizDist(origin, led) / 1.2);
         return ThreatMoves.predictPos(target, anchor, elapsed, (float)lead);
      }
   }

   private static double horizDist(Vec3 a, Vec3 b) {
      double dx = b.x - a.x;
      double dz = b.z - a.z;
      return Math.sqrt(dx * dx + dz * dz);
   }
}
