package com.awakenedmod.awakened.content.entity.boss;

import com.awakenedmod.awakened.content.entity.Telegraphs;
import com.awakenedmod.awakened.content.entity.ThreatMoves;
import com.awakenedmod.awakened.core.Fx;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class MoveScheduler<T extends AwakenedBossEntity> {
   private static final int WINDUP_FLOOR = 12;
   private static final int RECOVER_FLOOR = 8;
   private final T boss;
   @Nullable
   private BossMove<? super T> current;
   private int moveTick;
   private boolean struck;
   private int curWindup;
   private int curTotal;
   @Nullable
   private Vec3 committedAim;
   @Nullable
   private Vec3 moveAnchor;
   @Nullable
   private Vec3 frozenLookPoint;
   private float lockedYaw;
   private float lockedHeadYaw;
   private int moveEventTick = -1;
   private final Map<BossMove<? super T>, Integer> cooldowns = new HashMap<>();
   private int circleFrame;
   private boolean circleClockwise;
   private int circleFlipAt;
   private boolean circling;

   public MoveScheduler(T boss) {
      this.boss = boss;
      boss.attachScheduler(this);
   }

   @Nullable
   public <M extends BossMove<? super T>> M pickMove(ServerLevel level, LivingEntity target, List<M> pool, boolean selfGrounded) {
      double dsq = this.boss.distanceToSqr(target);
      boolean towering = target.getY() - this.boss.getY() >= this.boss.defenseProfile().towerHeight();
      List<M> eligible = new ArrayList<>();
      List<Integer> weights = new ArrayList<>();
      int totalWeight = 0;

      for (M m : pool) {
         if (!m.requiresGround() || selfGrounded) {
            double min = m.minRange();
            double max = m.maxRange();
            if (!(dsq < min * min) && !(dsq > max * max)) {
               Integer readyAt = this.cooldowns.get(m);
               if ((readyAt == null || this.boss.tickCount >= readyAt) && m.canStart(this.boss, level, target)) {
                  int weight = Math.max(1, m.weight());
                  if (towering) {
                     if (m.punishesElevation()) {
                        weight *= 4;
                     }

                     if (m.needsGroundedTarget()) {
                        weight = Math.max(1, weight / 4);
                     }
                  }

                  eligible.add(m);
                  weights.add(weight);
                  totalWeight += weight;
               }
            }
         }
      }

      if (eligible.isEmpty()) {
         return null;
      } else {
         int roll = this.boss.getRandom().nextInt(totalWeight);

         for (int i = 0; i < eligible.size(); i++) {
            roll -= weights.get(i);
            if (roll < 0) {
               return eligible.get(i);
            }
         }

         return eligible.get(eligible.size() - 1);
      }
   }

   public void start(ServerLevel level, BossMove<? super T> move, LivingEntity target) {
      this.current = move;
      this.moveTick = 0;
      this.struck = false;
      this.moveAnchor = target.position();
      this.committedAim = null;
      this.frozenLookPoint = null;
      this.moveEventTick = -1;
      this.circling = false;
      float f = move.fixedTempo() ? 1.0F : this.tempo();
      this.curWindup = Math.max(Math.min(12, move.windup()), Math.round((float)move.windup() * f));
      int recover = Math.max(Math.min(8, move.recover()), Math.round((float)move.recover() * f));
      this.curTotal = this.curWindup + recover;
      if (move.cooldownTicks() > 0) {
         this.cooldowns.put(move, this.boss.tickCount + move.cooldownTicks());
      }

      if (move.rooted()) {
         this.boss.getNavigation().stop();
      }

      this.boss.setAction(move.action());
      this.boss.setActionProgress(0.0F);
      this.boss.setMoveBudget(this.curWindup, this.curTotal - this.curWindup);
      this.boss.getLookControl().setLookAt(target);
   }

   public void advance(ServerLevel level, @Nullable LivingEntity target) {
      BossMove<? super T> move = this.current;
      if (move != null) {
         this.moveTick++;
         int w = this.curWindup;
         int total = this.curTotal;
         this.boss.setActionProgress(Math.min(1.0F, (float)this.moveTick / (float)total));
         float tf = (float)w / (float)Math.max(1, move.windup());
         int lockAt = Math.max(2, Math.round((float)move.lookLockAt() * tf));
         int retrackAt = move.retrackAt() == Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.round((float)move.retrackAt() * tf);
         boolean locked = this.moveTick >= lockAt && this.moveTick < retrackAt;
         if (locked) {
            if (this.committedAim == null) {
               this.committedAim = target != null
                  ? target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0)
                  : this.boss.position().add(this.boss.getLookAngle().scale(8.0));
               this.lockedYaw = this.boss.getYRot();
               this.lockedHeadYaw = this.boss.getYHeadRot();
               this.frozenLookPoint = this.boss.getEyePosition().add(this.boss.getLookAngle().scale(16.0));
            }

            float chase = move.lockedTurnSpeed();
            if (chase > 0.0F && target != null) {
               double dx = target.getX() - this.boss.getX();
               double dz = target.getZ() - this.boss.getZ();
               if (dx * dx + dz * dz > 1.0E-4) {
                  float want = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
                  this.lockedYaw = Mth.approachDegrees(this.lockedYaw, want, chase);
                  this.lockedHeadYaw = Mth.approachDegrees(this.lockedHeadYaw, want, chase);
                  this.frozenLookPoint = this.boss.getEyePosition().add(Vec3.directionFromRotation(0.0F, this.lockedHeadYaw).scale(16.0));
               }
            }

            this.boss.setYRot(this.lockedYaw);
            this.boss.setYBodyRot(this.lockedYaw);
            this.boss.setYHeadRot(this.lockedHeadYaw);
            this.boss.getLookControl().setLookAt(this.frozenLookPoint.x, this.frozenLookPoint.y, this.frozenLookPoint.z);
         } else if (target != null) {
            this.boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
         }

         if (move.rooted()) {
            this.boss.getNavigation().stop();
            Vec3 dm = this.boss.getDeltaMovement();
            this.boss.setDeltaMovement(0.0, dm.y, 0.0);
         }

         if (this.moveTick == 1 && move.windupCue() != null) {
            Fx.sound(level, this.boss.position(), move.windupCue(), SoundSource.HOSTILE, move.windupCueVolume(), move.windupCuePitch());
         }

         if (this.moveTick == w - 3) {
            if (move.commitCue() != null) {
               Telegraphs.commitCue(level, this.boss.position(), move.commitCue(), move.commitCuePitch());
            }

            if (move.dangerRadius() > 0.0) {
               Vec3 c = move.dangerAtTarget() && target != null ? target.position() : this.boss.position();
               Telegraphs.dangerClose(level, c, move.dangerRadius());
            }
         }

         if (this.moveTick <= w && target != null) {
            move.windupTick(this.boss, level, target, this.moveTick);
         }

         if (!this.struck && this.moveTick >= w) {
            if (target != null && target.isAlive()) {
               move.strike(this.boss, level, target);
            }

            this.struck = true;
         }

         if (this.struck && this.moveTick > w) {
            move.recoverTick(this.boss, level, target, this.moveTick - w);
         }

         if (this.moveTick >= total || move.finishedEarly(this.boss, this.moveTick)) {
            this.clearMove();
         }
      }
   }

   public void interrupt() {
      this.clearMove();
      this.circling = false;
      this.boss.getNavigation().stop();
   }

   private void clearMove() {
      this.current = null;
      this.struck = false;
      this.committedAim = null;
      this.frozenLookPoint = null;
      this.moveEventTick = -1;
      this.boss.setAction((byte)0);
      this.boss.setActionProgress(0.0F);
      this.boss.setMoveBudget(0, 0);
   }

   private float tempo() {
      return switch (this.boss.escalationPhase()) {
         case 1 -> 1.0F;
         case 2 -> 0.85F;
         default -> 0.7F;
      };
   }

   public boolean tickCircling(LivingEntity target, boolean allowed, double minBand, double maxBand, double orbitRadius, double navSpeedMult) {
      double dsq = this.boss.distanceToSqr(target);
      if (allowed && this.boss.onGround() && !(dsq < minBand * minBand) && !(dsq > maxBand * maxBand)) {
         this.circling = true;
         if (this.boss.tickCount >= this.circleFlipAt) {
            this.circleClockwise = this.boss.getRandom().nextBoolean();
            this.circleFlipAt = this.boss.tickCount + 120 + this.boss.getRandom().nextInt(120);
         }

         this.circleFrame++;
         ThreatMoves.circleAround(this.boss, target, orbitRadius, 1.0, this.circleClockwise, this.circleFrame, 0.0, navSpeedMult);
         this.boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
         return true;
      } else {
         this.circling = false;
         return false;
      }
   }

   public void stopCircling() {
      this.circling = false;
   }

   public boolean isRunning() {
      return this.current != null;
   }

   public boolean suppressesMovement() {
      return this.current != null || this.circling;
   }

   @Nullable
   public Vec3 committedAim() {
      return this.committedAim;
   }

   public void steerLockedFacing(float yawDeg) {
      if (this.current != null && this.committedAim != null) {
         this.lockedYaw = yawDeg;
         this.lockedHeadYaw = yawDeg;
         this.frozenLookPoint = this.boss.getEyePosition().add(Vec3.directionFromRotation(0.0F, yawDeg).scale(16.0));
         this.boss.setYRot(yawDeg);
         this.boss.setYBodyRot(yawDeg);
         this.boss.setYHeadRot(yawDeg);
      }
   }

   @Nullable
   public Vec3 moveAnchor() {
      return this.moveAnchor;
   }

   public boolean latchMoveEvent() {
      if (this.moveEventTick >= 0) {
         return false;
      } else {
         this.moveEventTick = Math.max(0, this.moveTick - this.curWindup);
         return true;
      }
   }

   public int moveEventTick() {
      return this.moveEventTick;
   }

   public int currentWindup() {
      return this.curWindup;
   }

   public int currentTotal() {
      return this.curTotal;
   }

   public int currentMoveTick() {
      return this.moveTick;
   }

   public boolean isInRecover() {
      return this.current != null && this.struck;
   }
}
