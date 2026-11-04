package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class TideHookAbility extends Ability {
   private static final float COST = 20.0F;
   private static final int UNLOCK = 7;
   private static final float WATER = 2.0F;
   private static final double AIM_RANGE = 22.0;
   private static final double MIN_HOLD = 2.0;
   private static final double MAX_HOLD = 6.0;
   private static final double HOLD_STEP = 1.5;
   private static final int MAX_TICKS = 60;
   private static final double FOLLOW_GAIN = 0.85;
   private static final double FOLLOW_MAX_SPEED = 2.0;
   private static final double PULL_MIN = 0.9;
   private static final double PULL_PER_BLOCK = 0.06;
   private static final double PULL_MAX = 2.3;
   private static final int TRAIL_TICKS = 6;
   private static final double THROW_SPEED = 1.3;
   private static final float THROW_DAMAGE = 5.0F;
   public static final String GRIP_CLIP = "hydro_hook_grip";
   private final Map<UUID, TideHookAbility.Grab> grabs = this.activeState();
   private final Map<UUID, TideHookAbility.Trail> trails = this.playerState();

   public TideHookAbility() {
      super("tide_hook", 20.0F, 7);
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean usesGripControls() {
      return true;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(5.0).range(22.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.grabs.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 hand = AimUtil.handPosition(player);
         double range = (double)MasteryScaling.scaled(22.0F, player);
         double maxHold = (double)MasteryScaling.scaled(6.0F, player);
         LivingEntity target = AimUtil.aimedLivingEntity(player, range, e -> Harm.controls(player, e));
         if (target != null) {
            Hydro.Cast cast = Hydro.take(player, 2.0F);
            Hydro.show(player, cast, hand);
            double holdDist = Math.max(2.0, Math.min(maxHold, player.getEyePosition().distanceTo(target.position())));
            this.grabs.put(player.getUUID(), new TideHookAbility.Grab(target, holdDist, maxHold, 60, cast));
            PowerService.playAnimation(player, "hydro_hook_grip", true);
            TideHookVisual.grip(player, target, cast.wet());
            HydroFx.droplets(level, waist(target), 10, 0.4, cast.wet());
            Fx.sound(level, player.position(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0F, 0.8F);
            Fx.sound(level, target.position(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.6F, 1.3F);
            return true;
         } else {
            HitResult hit = player.pick(range, 1.0F, false);
            if (hit.getType() != Type.BLOCK) {
               return this.fail(player, "awakened.hydrokinetic.no_hook", new Object[0]);
            } else {
               Hydro.Cast cast = Hydro.take(player, 2.0F);
               Hydro.show(player, cast, hand);
               Vec3 to = hit.getLocation();
               Direction face = ((BlockHitResult)hit).getDirection();
               Vec3 normal = new Vec3((double)face.getStepX(), (double)face.getStepY(), (double)face.getStepZ());
               Vec3 toLatch = to.subtract(player.getEyePosition());
               double pull = Math.max(0.9, Math.min(2.3, 0.9 + toLatch.length() * 0.06));
               Vec3 dir = toLatch.normalize();
               player.setDeltaMovement(dir.x * pull, dir.y * pull + 0.35, dir.z * pull);
               player.hurtMarked = true;
               player.resetFallDistance();
               player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false));
               HydroJetFx.tendril(level, player, to, normal, cast.wet());
               HydroJetFx.latch(level, to, normal, cast.wet());
               Fx.sound(level, player.position(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 1.0F, 1.2F);
               return true;
            }
         }
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return this.grabs.containsKey(player.getUUID()) ? null : "hydro_hook_cast";
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.trailTick(player);
      TideHookAbility.Grab grab = this.grabs.get(player.getUUID());
      if (grab != null) {
         LivingEntity target = grab.target;
         if (target.isAlive() && !target.isRemoved()) {
            ServerLevel level = player.serverLevel();
            Vec3 hold = player.getEyePosition().add(player.getLookAngle().scale(grab.holdDist));
            Vec3 center = waist(target);
            Vec3 toHold = hold.subtract(center);
            double dist = toHold.length();
            Vec3 desired = dist < 0.05 ? Vec3.ZERO : toHold.normalize().scale(Math.min(dist * 0.85, 2.0));
            target.setDeltaMovement(desired);
            target.hurtMarked = true;
            target.resetFallDistance();
            Harm.effect(player, target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8, 2, false, false));
            Harm.effect(player, target, new MobEffectInstance(MobEffects.WEAKNESS, 8, 1, false, false));
            if (grab.ticksLeft % 4 == 0) {
               Fx.burst(level, ParticleTypes.FALLING_WATER, center, 3, (double)target.getBbWidth() * 0.6, 0.0);
            }

            if (--grab.ticksLeft <= 0) {
               this.drop(player);
            }
         } else {
            if (Foes.defeatedBy(target, player)) {
               QuestSystem.feat(player, "hydro_drown_hold");
            }

            this.grabs.remove(player.getUUID());
            PowerService.stopHeldAnimation(player, "hydro_hook_grip");
            TideHookVisual.clear(player);
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      this.drop(player);
   }

   @Override
   public void onSecondaryAction(ServerPlayer player, int action) {
      if (this.grabs.containsKey(player.getUUID())) {
         switch (action) {
            case 0:
               this.fling(player);
               break;
            case 1:
               this.adjustHold(player, 1.5);
               break;
            case 2:
               this.adjustHold(player, -1.5);
         }
      }
   }

   private void drop(ServerPlayer player) {
      TideHookAbility.Grab grab = this.grabs.remove(player.getUUID());
      if (grab != null) {
         PowerService.stopHeldAnimation(player, "hydro_hook_grip");
         TideHookVisual.clear(player);
         if (grab.target.isAlive()) {
            HydroFx.droplets(player.serverLevel(), waist(grab.target), 14, 0.5, grab.cast.wet());
            Fx.sound(player.serverLevel(), player.position(), SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.5F, 0.8F);
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.grabs.containsKey(player.getUUID())) {
         PowerService.stopHeldAnimation(player, "hydro_hook_grip");
      }

      TideHookVisual.clear(player);
      super.clearPlayer(player);
   }

   private void adjustHold(ServerPlayer player, double delta) {
      TideHookAbility.Grab grab = this.grabs.get(player.getUUID());
      if (grab != null) {
         grab.holdDist = Math.max(2.0, Math.min(grab.maxHold, grab.holdDist + delta));
      }
   }

   private static Vec3 waist(LivingEntity target) {
      return target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
   }

   private void fling(ServerPlayer player) {
      TideHookAbility.Grab grab = this.grabs.remove(player.getUUID());
      if (grab != null) {
         PowerService.stopHeldAnimation(player, "hydro_hook_grip");
         TideHookVisual.clear(player);
         if (grab.target.isAlive()) {
            PowerService.playAnimation(player, "hydro_hook_fling");
            LivingEntity target = grab.target;
            ServerLevel level = player.serverLevel();
            Vec3 look = player.getLookAngle();
            boolean wet = grab.cast.wet();
            Vec3 waist = waist(target);
            HydroJetFx.whip(level, AimUtil.handPosition(player), waist.add(look.scale(2.0)), wet);
            target.setDeltaMovement(look.x * 1.3, look.y * 1.3 + 0.2, look.z * 1.3);
            target.hurtMarked = true;
            Hydro.hit(player, target, Hydro.damage(player, this, 5.0F, grab.cast));
            HydroFx.splash(level, waist, look, 1.0, wet);
            HydroFx.impactRing(level, waist, new Vec3(0.0, 1.0, 0.0), 1.2 + (double)target.getBbWidth(), wet);
            HydroFx.droplets(level, waist, 20, 0.5, wet);
            Fx.sound(level, target.position(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 1.0F, 0.8F);
            Fx.shakeFor(player, player.getEyePosition(), 4.0, 0.05F, 1, 3);
            this.trails.put(player.getUUID(), new TideHookAbility.Trail(target, 6, wet));
            MasterySystem.award(player, MasteryReason.HIT, target);
         }
      }
   }

   private void trailTick(ServerPlayer player) {
      TideHookAbility.Trail trail = this.trails.get(player.getUUID());
      if (trail != null) {
         LivingEntity body = trail.target;
         if (--trail.ticksLeft >= 0 && body.isAlive() && body.level() == player.serverLevel()) {
            Vec3 behind = waist(body).subtract(body.getDeltaMovement().scale(0.5));
            HydroFx.droplets(player.serverLevel(), behind, 5, 0.25, trail.wet);
         } else {
            this.trails.remove(player.getUUID());
         }
      }
   }

   private static final class Grab {
      private final LivingEntity target;
      private double holdDist;
      private final double maxHold;
      private int ticksLeft;
      private final Hydro.Cast cast;

      private Grab(LivingEntity target, double holdDist, double maxHold, int ticksLeft, Hydro.Cast cast) {
         this.target = target;
         this.holdDist = holdDist;
         this.maxHold = maxHold;
         this.ticksLeft = ticksLeft;
         this.cast = cast;
      }
   }

   private static final class Trail {
      private final LivingEntity target;
      private int ticksLeft;
      private final boolean wet;

      private Trail(LivingEntity target, int ticksLeft, boolean wet) {
         this.target = target;
         this.ticksLeft = ticksLeft;
         this.wet = wet;
      }
   }
}
