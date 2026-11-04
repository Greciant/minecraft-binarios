package com.awakenedmod.awakened.content.brawler;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.AwakenedAttachments;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.PowerMelee;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.Vec3;

public final class ShoulderBargeAbility extends BrawlerAbility {
   private static final ResourceLocation STEP_ID = AwId.of("brawler_barge_step");
   private static final String CLIP_BRACE_GROUND = "brawler_barge_brace";
   private static final String CLIP_BRACE_AIR = "brawler_barge_air_brace";
   private static final String CLIP_RUN = "brawler_barge_run";
   private static final String CLIP_RECOVER_GROUND = "brawler_barge_recover";
   private static final String CLIP_RECOVER_AIR = "brawler_barge_air_recover";
   private final Map<UUID, ShoulderBargeAbility.Dash> dashes = this.activeState();

   public ShoulderBargeAbility() {
      super("brawler_shoulder_barge", 8.0F, 5);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(4.0).build();
   }

   @Override
   public boolean requiresFootingWhenRealistic() {
      return true;
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      ShoulderBargeAbility.Dash dash = this.dashes.get(player.getUUID());
      return dash != null && dash.air ? "brawler_barge_air_brace" : "brawler_barge_brace";
   }

   @Override
   protected boolean press(ServerPlayer player) {
      ShoulderBargeAbility.Dash running = this.dashes.get(player.getUUID());
      if (running != null) {
         if (running.ticks < 6) {
            return false;
         }

         this.end(player, running, false);
      }

      ShoulderBargeAbility.Dash dash = new ShoulderBargeAbility.Dash();
      dash.air = airborne(player);
      dash.startedAt = player.serverLevel().getGameTime();
      this.dashes.put(player.getUUID(), dash);
      if (dash.air) {
         BrawlerLift.endHover(player);
      } else {
         stepBonus(player, true);
      }

      BrawlerFx.bargeStart(player.serverLevel(), player, dash.air);
      return true;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      ShoulderBargeAbility.Dash dash = this.dashes.get(player.getUUID());
      if (dash != null) {
         ServerLevel level = player.serverLevel();
         if (dash.ticks < 0) {
            if (dash.air) {
               hang(player);
            }

            if (++dash.braceTicks < castTicks(dash)) {
               return;
            }

            dash.ticks = 0;
            BrawlerIframes.grant(player, 6);
            steer(player, dash);
            dash.trailFrom = chest(player);
            BrawlerFx.bargeLaunch(level, player, dash.dir, dash.air);
            PowerService.playAnimation(player, "brawler_barge_run");
         } else {
            if (left(player, dash)) {
               this.end(player, dash, true);
               return;
            }

            steer(player, dash);
         }

         BrawlerFx.bargeTrail(level, player, dash.trailFrom, dash.dir, dash.air, dash.ticks);
         dash.trailFrom = chest(player);
         this.sweep(player, level, dash);
         carry(player, dash);
         dash.ticks++;
         if (player.horizontalCollision) {
            this.end(player, dash, true);
         } else if (dash.ticks >= 6) {
            if (!dash.held || dash.ticks >= 120) {
               this.end(player, dash, true);
            } else if (!BrawlerStamina.take(player, this.billed(holdDrain(player)))) {
               this.end(player, dash, true);
               PowerService.feedback(player, 0);
            } else {
               dash.heldTicks++;
            }
         }
      }
   }

   private static float holdDrain(ServerPlayer player) {
      double up = Math.max(0.0, player.getLookAngle().y);
      return (float)(0.55F * (1.0 + 0.5 * up));
   }

   @Override
   public void onRelease(ServerPlayer player) {
      ShoulderBargeAbility.Dash dash = this.dashes.get(player.getUUID());
      if (dash != null) {
         dash.held = false;
         if (dash.ticks >= 6) {
            this.end(player, dash, true);
         }
      }
   }

   private static boolean airborne(ServerPlayer player) {
      return !player.onGround() && !player.isInWater() && !player.isPassenger();
   }

   private static int castTicks(ShoulderBargeAbility.Dash dash) {
      return dash.air ? 4 : 4;
   }

   private static Vec3 chest(ServerPlayer player) {
      return player.position().add(0.0, (double)player.getBbHeight() * 0.55, 0.0);
   }

   private static void hang(ServerPlayer player) {
      Vec3 v = player.getDeltaMovement();
      player.setDeltaMovement(v.x * 0.6, Math.max(v.y * 0.6, 0.03), v.z * 0.6);
      player.hurtMarked = true;
      player.resetFallDistance();
   }

   private static void steer(ServerPlayer player, ShoulderBargeAbility.Dash dash) {
      boolean climbing = dash.held && dash.ticks >= 6 && player.getLookAngle().y >= 0.26;
      if (dash.air && player.onGround() && !climbing) {
         dash.air = false;
         stepBonus(player, true);
      } else if (!dash.air && climbing) {
         dash.air = true;
         stepBonus(player, false);
      }

      dash.flat = AimUtil.lookFlat(player);
      if (dash.air) {
         dash.dir = player.getLookAngle();
         player.setDeltaMovement(dash.dir.scale(0.72).add(0.0, 0.04, 0.0));
      } else {
         dash.dir = dash.flat;
         player.setDeltaMovement(dash.dir.x * 0.72, player.getDeltaMovement().y, dash.dir.z * 0.72);
      }

      player.hurtMarked = true;
      player.resetFallDistance();
   }

   private static boolean left(ServerPlayer player, ShoulderBargeAbility.Dash dash) {
      return dash.ticks < 6 ? false : AwakenedAttachments.get(player).lastSpendTime() > dash.startedAt || PowerMelee.lastSwingTick(player) > dash.startedAt;
   }

   private static void stepBonus(ServerPlayer player, boolean on) {
      AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
      if (step != null) {
         step.removeModifier(STEP_ID);
         if (on) {
            step.addTransientModifier(new AttributeModifier(STEP_ID, 0.5, Operation.ADD_VALUE));
         }
      }
   }

   private void sweep(ServerPlayer player, ServerLevel level, ShoulderBargeAbility.Dash dash) {
      Vec3 shoulder = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);

      for (LivingEntity target : AimUtil.livingAround(player, shoulder, 1.4)) {
         if (!dash.struck.contains(target.getId()) && Harm.allowed(player, target)) {
            dash.struck.add(target.getId());
            this.strike(player, level, dash, target);
         }
      }
   }

   private void strike(ServerPlayer player, ServerLevel level, ShoulderBargeAbility.Dash dash, LivingEntity target) {
      Mark was = Marks.current(target);
      float bonus = MarkBonus.of(MarkBonus.SHOULDER_BARGE, was, Marks.flavour(target));
      boolean transition = Marks.consumeTransition(player, target, this.id());
      float damage = MasteryScaling.scaledDamage(4.0F, player, this.balanceKey(), bonus, BrawlerCombo.bonusFraction(player));
      if (Dmg.hurtThrough(target, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage)) {
         dash.landed.add(target);
      }

      dash.transition |= transition;
      BrawlerFx.bargeContact(level, player, target);
      if (dash.carried == null && !BrawlerTargets.unliftable(target)) {
         dash.carried = target;
         dash.carriedWas = was;
         Marks.release(target);
         BrawlerHitStop.moving(target, 6);
      } else if (was == Mark.GROUNDED) {
         BrawlerFollowUp.shove(player, target, dash.flat.scale(0.75 * Marks.displacementScale(target)), 5, Mark.GROUNDED, 44, Mark.Flavour.NONE, this.id());
      } else {
         Marks.apply(player, target, Mark.PINNED, 44, Mark.Flavour.NONE, this.id());
      }
   }

   private static void carry(ServerPlayer player, ShoulderBargeAbility.Dash dash) {
      LivingEntity carried = dash.carried;
      if (carried != null) {
         if (carried.isAlive() && !carried.isRemoved()) {
            if (dash.ticks >= 5) {
               BrawlerHitStop.moving(carried, 3);
            }

            Vec3 lead = player.position().add(dash.dir.scale(1.0));
            Vec3 pull = lead.subtract(carried.position()).scale(0.3);
            double y;
            if (dash.air) {
               y = dash.dir.y * 0.72 + 0.04 + pull.y;
            } else {
               y = dash.carriedWas == Mark.AIRBORNE ? 0.0 : carried.getDeltaMovement().y;
            }

            Harm.setMotion(player, carried, dash.dir.x * 0.72 + pull.x, y, dash.dir.z * 0.72 + pull.z);
            carried.resetFallDistance();
         } else {
            dash.carried = null;
         }
      }
   }

   private void end(ServerPlayer player, ShoulderBargeAbility.Dash dash, boolean recover) {
      this.dashes.remove(player.getUUID());
      stepBonus(player, false);
      ServerLevel level = player.serverLevel();
      LivingEntity carried = dash.carried;
      if (carried != null && carried.isAlive() && !carried.isRemoved()) {
         Vec3 wall = MarkHang.nearestWall(carried, 3.0, dash.flat);
         boolean intoWall = wall != null;
         BrawlerFollowUp.shove(
            player,
            carried,
            (intoWall ? wall : dash.flat).scale(intoWall ? 0.9 : 0.75),
            intoWall ? 3 : 5,
            intoWall ? Mark.PINNED : Mark.GROUNDED,
            intoWall ? 44 : 44,
            intoWall ? Mark.Flavour.WALL : Mark.Flavour.NONE,
            this.id()
         );
         BrawlerFx.bargeImpact(level, carried, intoWall);
      }

      if (!dash.landed.isEmpty()) {
         BrawlerCombo.abilityHit(player, dash.landed.get(0), dash.landed.subList(1, dash.landed.size()), dash.transition ? 3 : 2, 4);
      }

      if (dash.ticks >= 0) {
         double d = 0.3;
         player.setDeltaMovement(player.getDeltaMovement().multiply(d, dash.air ? d : 1.0, d));
         player.hurtMarked = true;
         BrawlerLift.coverFall(player, 12);
         BrawlerFx.bargeEnd(level, player, dash.air);
      }

      if (dash.heldTicks > 0) {
         PowerData data = PowerService.data(player);
         StaminaSystem.suppressRegen(data, level.getGameTime());
         PowerService.sync(player);
      }

      PowerService.stopAnimation(player);
      if (recover) {
         PowerService.playAnimation(player, dash.air ? "brawler_barge_air_recover" : "brawler_barge_recover");
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      if (this.dashes.containsKey(player.getUUID())) {
         stepBonus(player, false);
         PowerService.stopAnimation(player);
      }

      super.clearPlayer(player);
   }

   private static final class Dash {
      boolean air;
      int braceTicks;
      int ticks = -1;
      boolean held = true;
      int heldTicks;
      long startedAt;
      Vec3 dir = Vec3.ZERO;
      Vec3 flat = Vec3.ZERO;
      Vec3 trailFrom;
      final Set<Integer> struck = new HashSet<>();
      final List<LivingEntity> landed = new ArrayList<>();
      boolean transition;
      LivingEntity carried;
      Mark carriedWas;
   }
}
