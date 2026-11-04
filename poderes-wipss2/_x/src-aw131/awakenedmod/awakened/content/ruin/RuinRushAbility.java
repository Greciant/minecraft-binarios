package com.awakenedmod.awakened.content.ruin;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.MoveKeys;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class RuinRushAbility extends RuinAbility {
   public static final String ID = "ruin_rush";
   public static final String CLIP_LOOP = "ruin_rush";
   public static final String CLIP_DRAG = "ruin_rush_drag";
   private static final String CLIP_SLAM = "ruin_rush_slam";
   private static final String CLIP_END = "ruin_rush_end";
   private static final ResourceLocation STEP_ID = AwId.of("ruin_rush_step");
   private final Map<UUID, RuinRushAbility.Dash> dashes = this.activeState();
   private final Map<UUID, RuinRushAbility.Pin> pins = this.playerState();

   public RuinRushAbility() {
      super("ruin_rush", 10.0F, 5);
   }

   @Override
   public String costLabel() {
      return holdCostLabel(this.billed(10.0F), this.billed(0.5F));
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return "ruin_rush_start";
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(6.0).build();
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
   public boolean activate(ServerPlayer player) {
      if (busy(player)) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else if (tooDeep(player)) {
         return this.fail(player, "awakened.ruin.rush_deep_water", new Object[0]);
      } else {
         RuinRushAbility.Dash dash = new RuinRushAbility.Dash(AimUtil.lookFlat(player), player.position(), MoveKeys.of(player));
         this.dashes.put(player.getUUID(), dash);
         stepBonus(player, true);
         steer(player, dash);
         RuinRushFx.start(player.serverLevel(), player, dash.dir);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.tickPin(player);
      RuinRushAbility.Dash dash = this.dashes.get(player.getUUID());
      if (dash != null) {
         if (dash.slamTicks >= 0) {
            this.slamTick(player, dash);
         } else if (!this.drainTick(player, 0.5F, dash.ticks)) {
            this.end(player, dash, false);
            ranDry(player);
         } else if (tooDeep(player)) {
            this.end(player, dash, false);
         } else if (movementKeyPressed(dash, MoveKeys.of(player)) && dash.ticks >= 5) {
            this.end(player, dash, true);
         } else {
            ServerLevel level = player.serverLevel();
            steer(player, dash);
            dash.ticks++;
            if (dash.ticks == 4 && dash.drag == null) {
               PowerService.playAnimation(player, "ruin_rush", true);
            }

            if (dash.drag == null) {
               LivingEntity hit = bodyInPath(player, dash);
               if (hit != null && !this.catchBody(player, dash, hit)) {
                  return;
               }
            } else if (!this.drag(player, dash)) {
               return;
            }

            sweep(player, level, dash);
            Vec3 now = player.position();
            RuinRushFx.trail(level, player, dash.from, now, dash.dir, dash.ticks);
            dash.from = now;
            if (dash.ticks > 1 && player.horizontalCollision && (dash.drag == null || dash.drag.tallWallAhead(dash.dir))) {
               if (dash.drag != null) {
                  this.slam(player, dash);
               } else {
                  RuinRushFx.bump(level, player, RuinDrag.wallAhead(player, player.position(), dash.dir));
                  this.end(player, dash, false);
               }
            } else if (dash.released && dash.ticks >= 8) {
               this.end(player, dash, false);
            }
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      RuinRushAbility.Dash dash = this.dashes.get(player.getUUID());
      if (dash != null && dash.slamTicks < 0) {
         if (dash.ticks >= 8) {
            this.end(player, dash, false);
         } else {
            dash.released = true;
         }
      }
   }

   @Override
   protected boolean yieldToPress(ServerPlayer player) {
      RuinRushAbility.Dash dash = this.dashes.get(player.getUUID());
      if (dash != null && dash.slamTicks < 0) {
         this.end(player, dash, false);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public float onIncomingDamage(ServerPlayer player, DamageSource source, float amount) {
      return this.dashes.containsKey(player.getUUID()) ? amount * 0.5F : amount;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      RuinRushAbility.Dash dash = this.dashes.remove(player.getUUID());
      if (dash != null) {
         stepBonus(player, false);
         RuinRushFx.silence(player);
         if (dash.drag != null) {
            dash.drag.letGo(null);
            RuinRushFx.dragSilence(dash.drag.body);
         }

         PowerService.stopAnimation(player);
         PowerService.playAnimation(player, "ruin_rush_end");
      }

      this.releasePin(player);
      super.clearPlayer(player);
   }

   private static void steer(ServerPlayer player, RuinRushAbility.Dash dash) {
      dash.dir = AimUtil.lookFlat(player);
      if (player.onGround()) {
         dash.vy = -0.4;
      } else {
         dash.vy = (Math.min(dash.vy, 0.0) - 0.08) * 0.98;
      }

      player.setDeltaMovement(dash.dir.x * 1.2, dash.vy, dash.dir.z * 1.2);
      player.hurtMarked = true;
      player.resetFallDistance();
   }

   private static void sweep(ServerPlayer player, ServerLevel level, RuinRushAbility.Dash dash) {
      Vec3 middle = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
      Vec3 side = new Vec3(-dash.dir.z, 0.0, dash.dir.x);

      for (LivingEntity body : Harm.hostilesOnly(player, AimUtil.livingAround(player, middle, 1.6))) {
         if (dash.passed.add(body.getId()) && RuinStacks.add(body, 1, player, RuinStacks.touchPoint(player, body))) {
            double sign = body.position().subtract(player.position()).dot(side) >= 0.0 ? 1.0 : -1.0;
            Harm.push(player, body, side.x * sign * 0.7, 0.25, side.z * sign * 0.7);
            RuinRushFx.pass(level, body);
         }
      }
   }

   private static LivingEntity bodyInPath(ServerPlayer player, RuinRushAbility.Dash dash) {
      AABB path = player.getBoundingBox().expandTowards(dash.dir.scale(1.6));
      Vec3 middle = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
      double reachSqr = 12.25;
      return AimUtil.aimedLivingSoft(player, 5.1, 25.0, body -> {
         if (body.getBoundingBox().intersects(path) && RuinGripAbility.grabbable(player, body) && !(body.getBoundingBox().distanceToSqr(middle) > reachSqr)) {
            Vec3 flat = body.position().subtract(player.position()).multiply(1.0, 0.0, 1.0);
            return flat.lengthSqr() >= 1.0E-4 && flat.normalize().dot(dash.dir) > 0.0;
         } else {
            return false;
         }
      });
   }

   private boolean catchBody(ServerPlayer player, RuinRushAbility.Dash dash, LivingEntity body) {
      Vec3 touch = RuinStacks.touchPoint(player, body);
      if (dash.passed.add(body.getId())) {
         RuinStacks.add(body, 1, player, touch);
      }

      this.releasePin(player);
      dash.drag = RuinDrag.take(body);
      dash.dragTicks = 0;
      RuinRushFx.catchBody(player.serverLevel(), player, body, touch);
      PowerService.stopAnimation(player);
      PowerService.playAnimation(player, "ruin_rush_drag", true);
      Vec3 spot = dash.drag.placeAhead(player, dash.dir);
      if (spot == null) {
         if (dash.drag.tallWallAhead(dash.dir)) {
            this.slam(player, dash);
         } else {
            this.end(player, dash, false);
         }

         return false;
      } else {
         dash.drag.moveTo(player, spot);
         return true;
      }
   }

   private boolean drag(ServerPlayer player, RuinRushAbility.Dash dash) {
      RuinDrag drag = dash.drag;
      if (drag.lost(player)) {
         this.end(player, dash, false);
         return false;
      } else {
         dash.dragTicks++;
         Vec3 spot = drag.placeAhead(player, dash.dir);
         if (spot == null) {
            if (drag.tallWallAhead(dash.dir)) {
               this.slam(player, dash);
            } else {
               this.end(player, dash, false);
            }

            return false;
         } else {
            Vec3 was = drag.at;
            drag.moveTo(player, spot);
            ServerLevel level = player.serverLevel();
            LivingEntity body = drag.body;
            if (dash.dragTicks % 10 == 0) {
               Vec3 touch = RuinStacks.touchPoint(player, body);
               if (RuinStacks.add(body, 1, player, touch)) {
                  RuinFx.gripStack(level, body, touch, RuinStacks.stacks(body));
               }
            }

            RuinRushFx.dragTick(level, player, body, was, spot, dash.dir, dash.dragTicks);
            return true;
         }
      }
   }

   private void slam(ServerPlayer player, RuinRushAbility.Dash dash) {
      stepBonus(player, false);
      player.setDeltaMovement(0.0, Math.min(dash.vy, 0.0), 0.0);
      player.hurtMarked = true;
      player.resetFallDistance();
      dash.slamTicks = 0;
      dash.wall = dash.drag.wallAhead(dash.dir);
      RuinRushFx.wallHit(player.serverLevel(), player, dash.drag.body, dash.wall);
      PowerService.stopAnimation(player);
      PowerService.playAnimation(player, "ruin_rush_slam");
      PowerService.sync(player);
   }

   private void slamTick(ServerPlayer player, RuinRushAbility.Dash dash) {
      RuinDrag drag = dash.drag;
      if (drag.lost(player)) {
         this.dashes.remove(player.getUUID());
         drag.letGo(null);
         RuinRushFx.silence(player);
         PowerService.sync(player);
      } else {
         drag.moveTo(player, drag.at);
         if (++dash.slamTicks >= 5) {
            this.impact(player, dash);
         }
      }
   }

   private void impact(ServerPlayer player, RuinRushAbility.Dash dash) {
      this.dashes.remove(player.getUUID());
      RuinDrag drag = dash.drag;
      LivingEntity body = drag.body;
      ServerLevel level = player.serverLevel();
      drag.driveInto(player, dash.dir);
      RuinStacks.add(body, 10, player, body.position());
      float damage = MasteryScaling.scaledDamage(6.0F, player, this.balanceKey());
      Dmg.hurt(body, level, player.damageSources().playerAttack(player), damage);
      RuinStacks.payout(body, MasteryScaling.scaledDamage(0.5F, player, this.balanceKey()));
      MasterySystem.award(player, MasteryReason.HIT, body);
      RuinFeats.slammed(player, body);
      RuinRushFx.wallSlam(level, player, body, dash.wall);
      if (drag.lost(player)) {
         drag.letGo(null);
      } else {
         this.pins.put(player.getUUID(), new RuinRushAbility.Pin(drag, 20));
      }

      PowerService.sync(player);
   }

   private void tickPin(ServerPlayer player) {
      RuinRushAbility.Pin pin = this.pins.get(player.getUUID());
      if (pin != null) {
         if (!pin.drag.lost(player) && --pin.ticksLeft > 0) {
            pin.drag.pinTick(player);
         } else {
            this.releasePin(player);
         }
      }
   }

   private void releasePin(ServerPlayer player) {
      RuinRushAbility.Pin pin = this.pins.remove(player.getUUID());
      if (pin != null) {
         pin.drag.letGo(Vec3.ZERO);
      }
   }

   private void end(ServerPlayer player, RuinRushAbility.Dash dash, boolean stopDead) {
      if (this.dashes.remove(player.getUUID()) != null) {
         stepBonus(player, false);
         double keep = stopDead ? 0.0 : 0.3;
         player.setDeltaMovement(dash.dir.x * 1.2 * keep, Math.min(dash.vy, 0.0), dash.dir.z * 1.2 * keep);
         player.hurtMarked = true;
         player.resetFallDistance();
         if (dash.drag != null) {
            double tumble = 0.72;
            dash.drag.letGo(new Vec3(dash.dir.x * tumble, 0.2, dash.dir.z * tumble));
            RuinRushFx.dragRelease(player.serverLevel(), dash.drag.body);
         }

         RuinRushFx.end(player.serverLevel(), player, stopDead);
         PowerService.stopAnimation(player);
         PowerService.playAnimation(player, "ruin_rush_end");
         PowerService.sync(player);
      }
   }

   private static boolean movementKeyPressed(RuinRushAbility.Dash dash, MoveKeys.Keys now) {
      MoveKeys.Keys was = dash.keys;
      dash.keys = now;
      return now.forward() && !was.forward() || now.backward() && !was.backward() || now.left() && !was.left() || now.right() && !was.right();
   }

   private static boolean tooDeep(ServerPlayer player) {
      return player.getFluidHeight(FluidTags.WATER) > 1.0;
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

   private static final class Dash {
      int ticks;
      Vec3 dir;
      double vy;
      Vec3 from;
      MoveKeys.Keys keys;
      boolean released;
      final Set<Integer> passed = new HashSet<>();
      RuinDrag drag;
      int dragTicks;
      int slamTicks = -1;
      BlockHitResult wall;

      Dash(Vec3 dir, Vec3 from, MoveKeys.Keys keys) {
         this.dir = dir;
         this.from = from;
         this.keys = keys;
      }
   }

   private static final class Pin {
      final RuinDrag drag;
      int ticksLeft;

      Pin(RuinDrag drag, int ticksLeft) {
         this.drag = drag;
         this.ticksLeft = ticksLeft;
      }
   }
}
