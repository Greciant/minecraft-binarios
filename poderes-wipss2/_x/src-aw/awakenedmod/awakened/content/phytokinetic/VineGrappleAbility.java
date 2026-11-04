package com.awakenedmod.awakened.content.phytokinetic;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class VineGrappleAbility extends Ability {
   public static final String ID = "vine_grapple";
   private static final float COST = 10.0F;
   private static final int UNLOCK = 5;
   private static final double RANGE = 20.0;
   private static final double WIDTH = 0.08;
   private static final int WINDUP_TICKS = 2;
   private static final double GROW_PER_TICK = 4.5;
   private static final double PULL_SPEED = 1.7;
   private static final double FOLIAGE_PULL_MULT = 1.3;
   private static final double FOLIAGE_KICK = 0.35;
   private static final int PULL_RAMP_TICKS = 3;
   private static final double PULL_RAMP_MIN = 0.55;
   private static final double PULL_LIFT = 0.1;
   private static final double ANCHOR_STANDOFF = 1.0;
   private static final double ARRIVE_DIST = 0.9;
   private static final double ARRIVE_BRAKE_DIST = 3.5;
   private static final double ARRIVE_DAMP = 0.2;
   private static final int MAX_PULL_TICKS = 40;
   private static final int SETTLE_TICKS = 30;
   private static final int CORD_EVERY = 2;
   private static final int CORD_LIFE = 3;
   private static final double CORD_SAG = 0.03;
   private final Map<UUID, VineGrappleAbility.Grapple> grapples = this.activeState();

   public VineGrappleAbility() {
      super("vine_grapple", 10.0F, 5);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().range(20.0).build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.grapples.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 eye = player.getEyePosition();
         double range = MasteryScaling.scaled(20.0, player);
         Vec3 far = eye.add(player.getViewVector(1.0F).scale(range));
         BlockHitResult hit = level.clip(new ClipContext(eye, far, Block.COLLIDER, Fluid.NONE, player));
         if (hit.getType() != Type.BLOCK) {
            return this.fail(player, "awakened.feedback.no_grapple_anchor", new Object[0]);
         } else {
            BlockState struck = level.getBlockState(hit.getBlockPos());
            boolean foliage = struck.is(BlockTags.LEAVES) || struck.is(BlockTags.LOGS);
            Vec3 surface = hit.getLocation();
            Direction face = hit.getDirection();
            Vec3 reelTarget = surface.add((double)face.getStepX() * 1.0, (double)face.getStepY() * 1.0, (double)face.getStepZ() * 1.0);
            this.grapples.put(player.getUUID(), new VineGrappleAbility.Grapple(surface, reelTarget, foliage));
            Vec3 hand = AimUtil.handPosition(player);
            PhytoFx.gatherAtHand(level, hand);
            Fx.sound(level, hand, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 0.8F, 1.3F);
            return true;
         }
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      VineGrappleAbility.Grapple grapple = this.grapples.get(player.getUUID());
      if (grapple != null) {
         ServerLevel level = player.serverLevel();
         grapple.tick++;
         switch (grapple.phase) {
            case WINDUP:
               this.tickWindup(player, level, grapple);
               break;
            case GROW:
               this.tickGrow(player, level, grapple);
               break;
            case PULL:
               this.tickPull(player, level, grapple);
               break;
            case SETTLE:
               this.tickSettle(player, grapple);
         }
      }
   }

   private void tickWindup(ServerPlayer player, ServerLevel level, VineGrappleAbility.Grapple grapple) {
      if (grapple.tick < 2) {
         PhytoFx.gatherAtHand(level, AimUtil.handPosition(player));
      } else {
         Vec3 hand = AimUtil.handPosition(player);
         grapple.path = PhytoFx.whipPath(level.getRandom(), hand, grapple.anchor);
         grapple.travelTicks = PhytoFx.growVine(level, level.getRandom(), grapple.path, 0.08, 4.5);
         toPhase(grapple, VineGrappleAbility.Phase.GROW);
         Fx.burst(level, PhytoFx.LEAF_DUST, hand, 8, 0.25, 0.04);
         Fx.sound(level, hand, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0F, 0.7F);
      }
   }

   private void tickGrow(ServerPlayer player, ServerLevel level, VineGrappleAbility.Grapple grapple) {
      double t = Math.min(1.0, (double)grapple.tick / (double)grapple.travelTicks);
      PhytoFx.tipMotes(level, PhytoFx.pointAt(grapple.path, t), grapple.tick);
      if (grapple.tick >= grapple.travelTicks) {
         PhytoFx.rootBurst(level, level.getRandom(), grapple.anchor.add(0.0, -0.1, 0.0));
         PhytoFx.leafBurst(level, grapple.anchor, 6);
         Fx.sound(level, grapple.anchor, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.PLAYERS, 1.1F, 0.8F);
         Fx.sound(level, grapple.anchor, SoundEvents.AZALEA_PLACE, SoundSource.PLAYERS, 1.0F, 0.9F);
         Fx.sound(level, player.position(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.9F, 1.1F);
         grapple.pullStartDist = Math.max(1.0, chest(player).distanceTo(grapple.reelTarget));
         toPhase(grapple, VineGrappleAbility.Phase.PULL);
      }
   }

   private void tickPull(ServerPlayer player, ServerLevel level, VineGrappleAbility.Grapple grapple) {
      Vec3 toAnchor = grapple.reelTarget.subtract(chest(player));
      double dist = toAnchor.length();
      if (!(dist <= 0.9) && grapple.tick < 40 && (!player.horizontalCollision || grapple.tick <= 1)) {
         Vec3 dir = toAnchor.scale(1.0 / dist);
         double peak = grapple.foliage ? 2.21 : 1.7;
         double ramp = Math.min(1.0, ((double)grapple.tick + 1.0) / 3.0);
         double brake = Math.min(1.0, dist / 3.5);
         double speed = peak * (0.55 + 0.44999999999999996 * ramp) * brake;
         double lift = 0.1 * (1.0 - Math.max(0.0, Math.min(1.0, dir.y))) * brake;
         player.setDeltaMovement(dir.x * speed, dir.y * speed + lift, dir.z * speed);
         player.hurtMarked = true;
         player.resetFallDistance();
         Vec3 hand = AimUtil.handPosition(player);
         if (grapple.tick % 2 == 0) {
            PhytoFx.tautVine(level, PhytoFx.sagPath(hand, grapple.anchor, 0.03), 0.064, 3);
         }

         double consumed = Math.max(0.0, Math.min(1.0, 1.0 - dist / grapple.pullStartDist));
         RandomSource random = level.getRandom();

         for (int i = 0; i < 2; i++) {
            Vec3 fleck = PhytoFx.pointAt(grapple.path, random.nextDouble() * consumed);
            Fx.burst(level, PhytoFx.LEAF_DUST, fleck, 1, 0.12, 0.02);
         }

         if (grapple.tick % 3 == 0) {
            Fx.burst(level, ParticleTypes.HAPPY_VILLAGER, PhytoFx.pointAt(grapple.path, consumed), 1, 0.15, 0.02);
         }

         if (grapple.tick % 5 == 0) {
            Fx.sound(level, hand, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 0.5F, 0.9F + (float)consumed * 0.5F);
         }
      } else {
         this.arrive(player, level, grapple, dist < 0.05 ? player.getLookAngle() : toAnchor.scale(1.0 / dist));
      }
   }

   private void arrive(ServerPlayer player, ServerLevel level, VineGrappleAbility.Grapple grapple, Vec3 dir) {
      PowerService.stopAnimation(player);
      player.setDeltaMovement(player.getDeltaMovement().scale(0.2));
      player.hurtMarked = true;
      player.resetFallDistance();
      PhytoFx.leafBurst(level, grapple.anchor, 8);
      Fx.sound(level, grapple.anchor, SoundEvents.VINE_BREAK, SoundSource.PLAYERS, 0.9F, 1.2F);
      Fx.sound(level, grapple.anchor, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 0.7F, 1.3F);
      if (grapple.foliage) {
         Vec3 v = player.getDeltaMovement();
         player.setDeltaMovement(v.x + dir.x * 0.35, v.y, v.z + dir.z * 0.35);
         player.hurtMarked = true;
         PhytoFx.leafBurst(level, grapple.anchor, 14);
         PhytoFx.fallingPetals(level, grapple.anchor, -10960310, 8);
         Fx.sound(level, grapple.anchor, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.PLAYERS, 1.1F, 1.0F);
         Fx.sound(level, grapple.anchor, SoundEvents.CHERRY_LEAVES_PLACE, SoundSource.PLAYERS, 1.0F, 0.8F);
         MasterySystem.award(player, MasteryReason.SURVIVAL);
      }

      toPhase(grapple, VineGrappleAbility.Phase.SETTLE);
   }

   private void tickSettle(ServerPlayer player, VineGrappleAbility.Grapple grapple) {
      ServerLevel level = player.serverLevel();
      player.resetFallDistance();
      if (grapple.tick <= 6) {
         Fx.burst(level, PhytoFx.LEAF_DUST, grapple.anchor, 2, 0.3, 0.03);
      }

      if (grapple.tick >= 2 && player.onGround() || grapple.tick >= 30) {
         this.grapples.remove(player.getUUID());
      }
   }

   private static void toPhase(VineGrappleAbility.Grapple grapple, VineGrappleAbility.Phase phase) {
      grapple.phase = phase;
      grapple.tick = 0;
   }

   private static Vec3 chest(ServerPlayer player) {
      return player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.grapples.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
      }

      super.clearPlayer(player);
   }

   private static final class Grapple {
      final Vec3 anchor;
      final Vec3 reelTarget;
      final boolean foliage;
      VineGrappleAbility.Phase phase = VineGrappleAbility.Phase.WINDUP;
      int tick;
      List<Vec3> path;
      int travelTicks;
      double pullStartDist;

      Grapple(Vec3 anchor, Vec3 reelTarget, boolean foliage) {
         this.anchor = anchor;
         this.reelTarget = reelTarget;
         this.foliage = foliage;
      }
   }

   private static enum Phase {
      WINDUP,
      GROW,
      PULL,
      SETTLE;
   }
}
