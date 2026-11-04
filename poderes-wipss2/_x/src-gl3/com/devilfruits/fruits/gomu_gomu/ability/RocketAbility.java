package com.devilfruits.fruits.gomu_gomu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.physics.PhysicsStructure;
import com.devilfruits.core.physics.PhysicsWorld;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.gomu_gomu.GomuFx;
import com.devilfruits.fruits.gomu_gomu.GomuGomu;
import com.devilfruits.fruits.gomu_gomu.GomuSounds;
import com.devilfruits.fruits.gomu_gomu.GomuTriggers;
import com.devilfruits.fruits.gomu_gomu.strike.Strike;
import com.devilfruits.fruits.gomu_gomu.strike.StrikeKind;
import com.devilfruits.fruits.gomu_gomu.strike.StrikePath;
import com.devilfruits.registry.DFAttachments;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class RocketAbility extends Ability {
   static final double FEET_CLEAR = 0.3;

   public RocketAbility() {
      super(
         GomuGomu.id("rocket"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(20)
            .cooldown(200)
            .mastery(4)
            .damage(8.0)
            .range(20.0)
            .knockback(1.2)
            .extra("pullAccel", 1.2, 0.05, 3.0, "Blocks per tick the pull adds to the user's speed every tick.")
            .extra("maxPullSpeed", 4.4, 0.2, 8.0, "Top speed of the pull in blocks per tick.")
            .extra("maxPullTicks", 40.0, 5.0, 200.0, "The pull lets go after this many ticks at the latest.")
            .extra("releaseDistance", 2.0, 0.5, 6.0, "The arms let go this close to the anchor.")
            .extra("arrivalHop", 1.1, 0.0, 3.0, "Upward speed the user gets as the arms let go at the anchor (blocks per tick).")
            .extra("arrivalCarry", 0.6, 0.0, 2.0, "Speed toward the anchor kept at that release (blocks per tick).")
            .extra("momentumKeepTicks", 10.0, 0.0, 60.0, "Ticks a pull cut short keeps its speed after the release (almost no air drag).")
            .extra("rocketSlamDamage", 10.0, 0.0, 100.0, "Damage a body takes when the flying user slams into it at full speed.")
            .extra("structureImpulse", 1.0, 0.0, 10.0, "Factor on the push a Sable structure the arms anchor to gets toward the user.")
            .build()
      );
   }

   @Override
   public boolean coatable() {
      return true;
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults()).blockDamage(0.0).manual().build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerFx.animate(player, ctx.animation(GomuGomu.id("rocket")));
      player.level().playSound(null, player.getX(), player.getY() + 1.2, player.getZ(), GomuSounds.STRETCH.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
      RocketAbility.Flight flight = new RocketAbility.Flight(ctx);
      flight.strike = Strike.start(ctx, StrikeKind.ROCKET, ctx.config().range(), ctx.damage(), 0, new int[]{1, -1}, flight::hit);
      ServerScheduler.schedule(ctx.level(), 1, age -> flight.watch(age + 1));
   }

   public static final class Flight {
      final AbilityContext ctx;
      final ServerPlayer player;
      final Vec3 start;
      Strike strike;
      @Nullable
      Vec3 anchor;
      @Nullable
      Entity anchorEntity;
      Vec3 anchorOffset = Vec3.ZERO;
      @Nullable
      BlockPos anchorBlock;
      boolean released;
      boolean arrived;
      int pullAge;
      final Set<Integer> slammed = new HashSet<>();

      Flight(AbilityContext ctx) {
         this.ctx = ctx;
         this.player = ctx.player();
         this.start = this.player.position();
      }

      void hit(Strike strike, int arm, Vec3 at, @Nullable Entity target) {
         if (this.anchor == null) {
            ServerLevel level = this.ctx.level();
            BlockPos block = BlockPos.containing(at.subtract(strike.aim.scale(0.05)).add(strike.aim.scale(0.1)));
            if (target != null || level.isLoaded(block)) {
               PhysicsStructure under = PhysicsWorld.structureUnder(this.player);
               if (target == null && under != null && under == PhysicsWorld.structureAt(level, block)) {
                  strike.freeze();
               } else {
                  strike.freeze();
                  this.anchor = at;
                  this.anchorEntity = target;
                  this.anchorBlock = target == null ? block : null;
                  if (target != null) {
                     this.anchorOffset = at.subtract(target.position());
                     if (this.ctx.hurt(target, this.ctx.damage())) {
                        Targeting.launch(target, strike.aim.scale((double)this.ctx.knockback() * 0.4).add(0.0, 0.2, 0.0));
                     }
                  } else {
                     Vec3 toUser = this.start.subtract(at).normalize();
                     BlockImpactService.push(this.ctx, ImpactShape.sphere(at, 0.8), toUser, 40.0 * this.ctx.extra("structureImpulse"));
                  }

                  ServerFx.send(this.player, GomuFx.ROCKET_ANCHOR, at, strike.aim, 1.0F, strike.start, target == null ? 0 : target.getId() + 1);
                  level.playSound(null, at.x, at.y, at.z, GomuSounds.PUNCH_HIT.get(), SoundSource.PLAYERS, 1.0F, 0.8F);
               }
            }
         }
      }

      boolean watch(int t) {
         if (this.player.isRemoved() || !this.player.isAlive() || this.player.level() != this.ctx.level()) {
            return true;
         } else if (this.anchor == null) {
            if ((double)t > StrikePath.endTick(StrikeKind.ROCKET, this.strike.reach) + 1.0) {
               ServerFx.stopAnimation(this.player);
               DFAttachments.data(this.player)
                  .startCooldown(
                     this.ctx.ability().id(),
                     this.ctx.level().getGameTime(),
                     (int)Math.round((double)AbilityManager.cooldown(this.ctx.entry(), this.ctx.data(), 1.0F) * 0.5)
                  );
               return true;
            } else {
               return false;
            }
         } else {
            return this.released ? this.momentum() : this.pull();
         }
      }

      private boolean pull() {
         this.pullAge++;
         this.player.fallDistance = 0.0F;
         Vec3 target = this.anchorPoint();
         Vec3 from = this.anchorEntity == null ? this.player.position().subtract(0.0, 0.3, 0.0) : this.player.getBoundingBox().getCenter();
         Vec3 to = target == null ? Vec3.ZERO : target.subtract(from);
         boolean blocked = this.pullAge > 3 && (this.player.horizontalCollision || this.player.verticalCollision && this.player.getDeltaMovement().y > 0.1);
         if (target != null && !((double)this.pullAge > this.ctx.extra("maxPullTicks"))) {
            Vec3 v = this.player.getDeltaMovement().add(to.normalize().scale(this.ctx.extra("pullAccel")));
            double cap = this.ctx.extra("maxPullSpeed");
            if (v.length() > cap) {
               v = v.normalize().scale(cap);
            }

            if (!blocked && !(to.length() - v.length() < this.ctx.extra("releaseDistance"))) {
               Targeting.moveSelf(this.player, v);
               this.slam(v);
               return false;
            } else {
               this.release(true, to);
               return false;
            }
         } else {
            this.release(false, to);
            return false;
         }
      }

      @Nullable
      private Vec3 anchorPoint() {
         if (this.anchorEntity != null) {
            return this.anchorEntity.isAlive() && !this.anchorEntity.isRemoved() && this.anchorEntity.level() == this.player.level()
               ? this.anchorEntity.position().add(this.anchorOffset)
               : null;
         } else {
            return this.anchorBlock != null && this.player.level().isLoaded(this.anchorBlock) && !this.player.level().getBlockState(this.anchorBlock).isAir()
               ? this.anchor
               : null;
         }
      }

      private void release(boolean arrived, Vec3 to) {
         this.released = true;
         this.arrived = arrived;
         if (this.player.position().distanceTo(this.start) >= 15.0) {
            GomuTriggers.ROCKET_GAP_CROSSED.get().trigger(this.player);
         }

         this.pullAge = 0;
         Vec3 v = this.player.getDeltaMovement();
         if (arrived) {
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            Vec3 carry = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(this.ctx.extra("arrivalCarry"));
            Targeting.moveSelf(this.player, carry.add(0.0, this.ctx.extra("arrivalHop"), 0.0));
         }

         ServerFx.send(this.player, GomuFx.ROCKET_RELEASE, this.player.position(), v, (float)v.length(), this.strike.start, 0);
         this.player
            .level()
            .playSound(null, this.player.getX(), this.player.getY() + 1.0, this.player.getZ(), GomuSounds.SNAP.get(), SoundSource.PLAYERS, 1.0F, 0.7F);
         double from = to.length();
         long delay = Math.round(StrikePath.retracted(from) * (double)StrikeKind.ROCKET.retract - 2.0);
         ServerScheduler.later(
            this.ctx.level(),
            (int)Math.max(0L, delay),
            () -> this.player
                  .level()
                  .playSound(
                     null, this.player.getX(), this.player.getY() + 1.2, this.player.getZ(), GomuSounds.FIST_BACK.get(), SoundSource.PLAYERS, 1.0F, 1.0F
                  )
         );
         ServerFx.animate(this.player, GomuGomu.id("rocket_release"));
      }

      private boolean momentum() {
         this.pullAge++;
         this.player.fallDistance = 0.0F;
         if (this.arrived) {
            return this.pullAge > 20;
         } else {
            Vec3 v = this.player.getDeltaMovement();
            if ((double)this.pullAge <= this.ctx.extra("momentumKeepTicks") && !this.player.onGround()) {
               Targeting.moveSelf(this.player, new Vec3(v.x / 0.91 * 0.99, v.y, v.z / 0.91 * 0.99));
               this.slam(v);
               return false;
            } else {
               return true;
            }
         }
      }

      private void slam(Vec3 v) {
         double speed = v.length();
         if (!(speed < 0.6)) {
            for (Entity e : Targeting.inBox(
               this.ctx.level(), this.player, this.player.getBoundingBox().inflate(0.6).expandTowards(v), x -> Targeting.canHurt(this.player, x)
            )) {
               if (this.slammed.add(e.getId())) {
                  float scale = (float)Mth.clamp(speed / this.ctx.extra("maxPullSpeed"), 0.2, 1.0);
                  if (this.ctx.hurt(e, (float)this.ctx.extra("rocketSlamDamage") * scale)) {
                     Targeting.launch(e, v.normalize().scale(1.2 * (double)scale + 0.4).add(0.0, 0.35, 0.0));
                  }

                  ServerFx.send(this.player, GomuFx.ROCKET_SLAM, e.getBoundingBox().getCenter(), v, (float)speed, this.ctx.seed(), 0);
                  this.ctx.level().playSound(null, e.getX(), e.getY() + 1.0, e.getZ(), GomuSounds.BAZOOKA_HIT.get(), SoundSource.PLAYERS, 1.0F, 0.9F);
               }
            }
         }
      }

      public boolean anchored() {
         return this.anchor != null;
      }
   }
}
