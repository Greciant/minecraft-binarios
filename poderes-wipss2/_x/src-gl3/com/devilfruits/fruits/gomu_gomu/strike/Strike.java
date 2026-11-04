package com.devilfruits.fruits.gomu_gomu.strike;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.core.world.TerrainDestruction;
import com.devilfruits.fruits.gomu_gomu.GomuFx;
import com.devilfruits.fruits.gomu_gomu.GomuSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class Strike {
   public final AbilityContext ctx;
   public final StrikeKind kind;
   public final Vec3 aim;
   public final double reach;
   public final float damage;
   public final int pierce;
   public final long start;
   private final Strike.OnHit onHit;
   private final double[] end = new double[2];
   private final boolean[] stopped = new boolean[2];
   private final Vec3[] last = new Vec3[2];
   private final int[] hits = new int[2];
   private final Set<Integer> struck = new HashSet<>();
   private final int[] arms;
   private boolean returning;
   @Nullable
   private Breaker breaker;
   private int broken;
   private boolean frozen;

   private Strike(AbilityContext ctx, StrikeKind kind, Vec3 aim, double reach, float damage, int pierce, int[] arms, Strike.OnHit onHit) {
      this.ctx = ctx;
      this.kind = kind;
      this.aim = aim.normalize();
      this.reach = reach;
      this.damage = damage;
      this.pierce = pierce;
      this.arms = arms;
      this.onHit = onHit;
      this.start = ctx.level().getGameTime();
      this.end[0] = reach;
      this.end[1] = reach;
   }

   public static Strike start(AbilityContext ctx, StrikeKind kind, double reach, float damage, int pierce, int[] arms, Strike.OnHit onHit) {
      Strike s = create(ctx, kind, reach, damage, pierce, arms, onHit);
      ServerScheduler.schedule(ctx.level(), 1, age -> s.tick(age + 1));
      return s;
   }

   public static Strike create(AbilityContext ctx, StrikeKind kind, double reach, float damage, int pierce, int[] arms, Strike.OnHit onHit) {
      Strike s = new Strike(ctx, kind, ctx.look(), reach, damage, pierce, arms, onHit);
      ServerPlayer player = ctx.player();
      int armBits = 0;

      for (int arm : arms) {
         armBits |= arm > 0 ? 1 : 2;
      }

      ServerFx.send(player, GomuFx.STRIKE, player.position(), s.aim, (float)reach, s.start, kind.code | armBits << 4);
      AbilityManager.setBusy(player, (int)Math.ceil(StrikePath.doneTick(kind, reach)) + 1);
      return s;
   }

   public boolean tick(int t) {
      ServerPlayer player = this.ctx.player();
      if (!player.isRemoved() && player.isAlive() && player.level() == this.ctx.level()) {
         if (t == this.kind.release()) {
            player.level().playSound(null, player.getX(), player.getY() + 1.2, player.getZ(), this.launchSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
         }

         boolean done = true;

         for (int i = 0; i < this.arms.length; i++) {
            int arm = this.arms[i];
            int slot = arm > 0 ? 0 : 1;
            if ((double)t < StrikePath.doneTick(this.kind, this.end[slot])) {
               done = false;
            }

            if (!this.stopped[slot] && t > this.kind.release()) {
               this.sweep(player, arm, slot, t);
            }
         }

         if (!this.returning && !this.frozen && (long)t >= this.fistBackTick()) {
            this.returning = true;
            player.level().playSound(null, player.getX(), player.getY() + 1.2, player.getZ(), GomuSounds.FIST_BACK.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
         }

         return done;
      } else {
         return true;
      }
   }

   private SoundEvent launchSound() {
      return switch (this.kind) {
         case BAZOOKA -> (SoundEvent)GomuSounds.BAZOOKA_LAUNCH.get();
         case RIFLE -> (SoundEvent)GomuSounds.RIFLE_LAUNCH.get();
         default -> (SoundEvent)GomuSounds.PUNCH_LAUNCH.get();
      };
   }

   public long fistBackTick() {
      double first = Double.MAX_VALUE;

      for (int arm : this.arms) {
         first = Math.min(first, StrikePath.returnedTick(this.kind, this.end[arm > 0 ? 0 : 1]));
      }

      return Math.round(first - 2.0);
   }

   private void sweep(ServerPlayer player, int arm, int slot, int t) {
      Vec3 shoulder = StrikePath.shoulder(player, player.position(), this.aim, arm);
      double d1 = Math.min(StrikePath.distance(this.kind, (double)t, this.end[slot]), this.end[slot]);
      double d0 = Math.max(0.0, StrikePath.distance(this.kind, (double)(t - 1), this.end[slot]));
      if (!(d1 <= 0.0)) {
         Vec3 from = this.last[slot] != null ? this.last[slot] : StrikePath.fist(this.kind, shoulder, this.aim, arm, d0);
         Vec3 to = StrikePath.fist(this.kind, shoulder, this.aim, arm, d1);
         int steps = Math.max(1, (int)Math.ceil(from.distanceTo(to)));

         for (int k = 0; k < steps; k++) {
            Vec3 a = from.lerp(to, (double)k / (double)steps);
            Vec3 b = from.lerp(to, (double)(k + 1) / (double)steps);
            Vec3 hit = this.segment(player, arm, slot, a, b);
            if (hit != null) {
               return;
            }
         }

         this.last[slot] = to;
         if (d1 >= this.end[slot]) {
            this.stopped[slot] = true;
         }
      }
   }

   public Strike breaks(@Nullable Breaker breaker) {
      this.breaker = breaker;
      return this;
   }

   public int broken() {
      return this.broken;
   }

   @Nullable
   private Vec3 segment(ServerPlayer player, int arm, int slot, Vec3 a, Vec3 b) {
      return this.segment(player, arm, slot, a, b, 0);
   }

   @Nullable
   private Vec3 segment(ServerPlayer player, int arm, int slot, Vec3 a, Vec3 b, int depth) {
      ServerLevel level = this.ctx.level();
      BlockHitResult block = PhysicsWorld.clip(level, new ClipContext(a, b, Block.COLLIDER, Fluid.NONE, player));
      Vec3 wall = block.getType() == Type.BLOCK ? PhysicsWorld.worldLocation(level, block) : null;
      Vec3 stopAt = wall != null ? wall : b;
      List<Entity> bodies = new ArrayList<>(Targeting.inCapsule(level, player, a, stopAt, this.kind.radius));
      bodies.removeIf(ex -> this.struck.contains(ex.getId()));
      bodies.sort((x, y) -> Double.compare(x.getBoundingBox().getCenter().distanceToSqr(a), y.getBoundingBox().getCenter().distanceToSqr(a)));

      for (Entity e : bodies) {
         this.struck.add(e.getId());
         Vec3 at = e.getBoundingBox().clip(a, stopAt).orElse(e.getBoundingBox().getCenter());
         this.onHit.hit(this, arm, at, e);
         if (++this.hits[slot] > this.pierce) {
            return this.stop(player, arm, slot, at, true);
         }
      }

      if (wall == null) {
         return null;
      } else if (depth < 4 && this.breakThrough(player, block)) {
         ServerFx.send(player, GomuFx.STRIKE_HIT, wall, this.aim, (float)this.end[slot], this.start, this.kind.code | (arm > 0 ? 1 : 2) << 4 | 128);
         return this.segment(player, arm, slot, wall, b, depth + 1);
      } else {
         this.onHit.hit(this, arm, wall, null);
         return this.stop(player, arm, slot, wall, false);
      }
   }

   private boolean breakThrough(ServerPlayer player, BlockHitResult block) {
      if (this.breaker != null && this.broken < this.breaker.maxBlocks() && this.ctx.blockDamageAllowed()) {
         ServerLevel level = this.ctx.level();
         BlockPos hit = block.getBlockPos();
         BlockState struck = level.getBlockState(hit);
         if (!this.breaker.breaks(level, hit, struck)) {
            return false;
         } else {
            Vec3 at = block.getLocation();
            double r = this.breaker.radius();
            int ir = (int)Math.ceil(r);
            List<BlockPos> round = new ArrayList<>();
            round.add(hit);

            for (int dx = -ir; dx <= ir; dx++) {
               for (int dy = -ir; dy <= ir; dy++) {
                  for (int dz = -ir; dz <= ir; dz++) {
                     BlockPos pos = hit.offset(dx, dy, dz);
                     if (!pos.equals(hit) && pos.distToCenterSqr(at) <= r * r) {
                        round.add(pos);
                     }
                  }
               }
            }

            round.subList(1, round.size()).sort(Comparator.comparingDouble(p -> p.distToCenterSqr(at)));
            Vec3 fly = this.aim.scale(0.35).add(0.0, 0.15, 0.0);

            for (BlockPos pos : round) {
               if (this.broken >= this.breaker.maxBlocks()) {
                  break;
               }

               BlockState state = level.getBlockState(pos);
               if (this.breaker.breaks(level, pos, state) && TerrainDestruction.tearOut(level, player, pos, fly, this.ctx.budget(), this.broken * 2, true)) {
                  this.broken++;
               }
            }

            if (!level.getBlockState(hit).isAir()) {
               return false;
            } else {
               SoundType sound = struck.getSoundType(level, hit, player);
               level.playSound(null, hit, sound.getBreakSound(), SoundSource.BLOCKS, sound.getVolume() + 0.5F, sound.getPitch() * 0.8F);
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private Vec3 stop(ServerPlayer player, int arm, int slot, Vec3 at, boolean body) {
      Vec3 shoulder = StrikePath.shoulder(player, player.position(), this.aim, arm);
      this.end[slot] = Math.max(0.5, Math.min(this.reach, at.subtract(shoulder).dot(this.aim)));
      this.stopped[slot] = true;
      this.last[slot] = at;
      ServerFx.send(player, GomuFx.STRIKE_HIT, at, this.aim, (float)this.end[slot], this.start, this.kind.code | (arm > 0 ? 1 : 2) << 4 | (body ? 1 : 0) << 6);
      return at;
   }

   public void freeze() {
      this.stopped[0] = true;
      this.stopped[1] = true;
      this.frozen = true;
   }

   public Vec3 shoulder(int arm) {
      return StrikePath.shoulder(this.ctx.player(), this.ctx.player().position(), this.aim, arm);
   }

   public double end(int arm) {
      return this.end[arm > 0 ? 0 : 1];
   }

   public void nudge(Vec3 at, double power) {
      BlockImpactService.push(this.ctx, ImpactShape.sphere(at, this.kind.radius + 0.4), this.aim, power);
   }

   public interface OnHit {
      void hit(Strike var1, int var2, Vec3 var3, @Nullable Entity var4);
   }
}
