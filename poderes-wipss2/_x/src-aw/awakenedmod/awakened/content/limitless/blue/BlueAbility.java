package com.awakenedmod.awakened.content.limitless.blue;

import com.awakenedmod.awakened.content.limitless.LimitlessFx;
import com.awakenedmod.awakened.content.limitless.LimitlessPower;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.FallGuard;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.GravityOverride;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.PulledBody;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.PowerResource;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class BlueAbility extends Ability implements BlueClumps {
   public static final String ID = "limitless_blue";
   public static final ResourceLocation GRAVITY_ID = AwId.of("limitless_blue_gravity");
   private static final ResourceLocation FALL_GUARD_ID = AwId.of("limitless_blue_fall_guard");
   private static final int SYNC_TICKS = 5;
   private static final String ANIM_POINT = "limitless_blue_point";
   private static final String ANIM_HOLD = "limitless_blue_hold";
   private final Map<UUID, BlueAbility.Press> presses = this.activeState();
   private final Map<UUID, BlueAbility.Hold> holds = this.activeState();
   private final Map<UUID, BlueField> fields = this.playerState();
   private final FallGuard guard = new FallGuard(FALL_GUARD_ID, 10, 400);
   static final AbilityScaling.Entry PULL_RADIUS = new AbilityScaling.Entry("radius", AbilityScaling.Unit.BLOCKS, AbilityScaling.Kind.MULTIPLIER, 8.0, 13, 0.0);
   private static final float PRESS_COST = Math.min(10.0F, 4.0F);

   public BlueAbility() {
      super("limitless_blue", 10.0F, 0);
   }

   @Override
   public float staminaCost(Player viewer) {
      return PRESS_COST;
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return this.holds.containsKey(player.getUUID()) ? null : "limitless_blue_point";
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(3.0).value("damage_per_second", AbilityScaling.Unit.RAW, 2.0).add(PULL_RADIUS).build();
   }

   @Override
   public String costLabel() {
      return Math.round(this.billed(10.0F)) + " / " + Math.round(this.billed(12.0F)) + "/s";
   }

   @Override
   public int cooldownRemainingTicks(ServerPlayer player) {
      BlueField field = this.fields.get(player.getUUID());
      return field != null ? Math.max(1, field.ticksLeft()) : 0;
   }

   @Override
   public int cooldownLengthTicks(ServerPlayer player) {
      return 43;
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (!this.presses.containsKey(player.getUUID()) && !this.holds.containsKey(player.getUUID()) && !LimitlessPower.castingOther(player, this)) {
         PowerResource resource = this.resource(player);
         if (!resource.canAfford(player, this.billed(PRESS_COST))) {
            resource.refuse(player);
            return this.failSilently(player);
         } else {
            BlueAbility.Press press = new BlueAbility.Press(BlueAim.tap(player), BlueMotion.target(player), BlueAim.hold(player));
            if (!this.chains(player)) {
               this.presses.put(player.getUUID(), press);
               return true;
            } else {
               return this.startHold(player, press) || this.failSilently(player);
            }
         }
      } else {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      }
   }

   private boolean chains(ServerPlayer player) {
      return this.guard.armed(player) && !player.onGround() && !player.isInWater();
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.guard.tick(player, this.holds.containsKey(player.getUUID()));
      this.tickFields(player);
      BlueAbility.Press press = this.presses.get(player.getUUID());
      if (press != null && ++press.ticks >= 8) {
         this.presses.remove(player.getUUID());
         this.startHold(player, press);
      }

      BlueAbility.Hold hold = this.holds.get(player.getUUID());
      if (hold != null) {
         this.tickHold(player, hold);
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      BlueAbility.Press press = this.presses.remove(player.getUUID());
      if (press != null) {
         this.fireTap(player, press);
      } else {
         if (this.holds.containsKey(player.getUUID())) {
            this.endHold(player, false);
         }
      }
   }

   private void fireTap(ServerPlayer player, BlueAbility.Press press) {
      if (!this.charge(player, 10.0F)) {
         this.resource(player).refuse(player);
      } else {
         BlueOrbEntity orb = BlueOrbEntity.spawn(player.serverLevel(), press.orb.resolve(player), false);
         this.fields.put(player.getUUID(), new BlueField(player, orb, this.balanceKey()));
         PowerService.sync(player);
      }
   }

   private void tickFields(ServerPlayer player) {
      BlueField field = this.fields.get(player.getUUID());
      if (field != null) {
         if (field.tick(player, player.serverLevel())) {
            StaminaSystem.suppressRegen(PowerService.data(player), player.serverLevel().getGameTime());
         } else {
            this.fields.remove(player.getUUID());
            PowerService.sync(player);
         }
      }
   }

   @Override
   public double orbAlong(ServerPlayer player, Vec3 from, Vec3 dir, double max) {
      BlueField field = this.fields.get(player.getUUID());
      return field != null ? field.orbAlong(player.serverLevel(), from, dir, max) : max;
   }

   @Override
   public List<BluePop> popAt(ServerPlayer player, Vec3 at) {
      BlueField field = this.fields.get(player.getUUID());
      ServerLevel level = player.serverLevel();
      if (field != null && field.touches(level, at)) {
         this.fields.remove(player.getUUID());
         PowerService.sync(player);
         return List.of(field.pop(level));
      } else {
         return List.of();
      }
   }

   private boolean startHold(ServerPlayer player, BlueAbility.Press press) {
      if (PulledBody.carried(player)) {
         return this.fail(player, "awakened.limitless.blue_cannot_pull", new Object[0]);
      } else if (!this.charge(player, 4.0F)) {
         this.resource(player).refuse(player);
         return false;
      } else {
         ServerLevel level = player.serverLevel();
         BlueOrbEntity orb = BlueOrbEntity.spawn(level, press.pullOrb, true);
         this.holds.put(player.getUUID(), new BlueAbility.Hold(press.pullTo, orb, player.position()));
         BlueHoldState.set(player, BlueHoldState.of(player).pulling(press.pullTo));
         PowerService.playAnimation(player, "limitless_blue_hold", true);
         GravityOverride.set(player, GRAVITY_ID, 0.03);
         player.resetFallDistance();
         this.guard.arm(player);
         LimitlessFx.pullStarted(level, player);
         return true;
      }
   }

   private void tickHold(ServerPlayer player, BlueAbility.Hold hold) {
      hold.ticks++;
      Vec3 pos = player.position();
      double moved = pos.distanceTo(hold.lastPos);
      hold.lastPos = pos;
      hold.travelled += moved;
      if (moved > 6.0 || PulledBody.mustLetGo(player) || !hold.orb.isAlive()) {
         this.endHold(player, false);
      } else if (hold.ticks > 2 && BlueMotion.arrived(pos, hold.target, 0.6)) {
         this.endHold(player, true);
      } else {
         PowerResource resource = this.resource(player);
         float drain = this.billed(0.6F);
         if (!resource.canAfford(player, drain)) {
            PowerService.feedback(player, 0);
            this.endHold(player, false);
         } else {
            resource.sustain(player, drain);
            if (player.tickCount % 5 == 0) {
               PowerService.sync(player);
            }

            if (!hold.rewarded && hold.travelled >= 8.0) {
               LivingEntity foe = foeNear(player);
               if (foe != null) {
                  hold.rewarded = true;
                  MasterySystem.award(player, MasteryReason.HIT, foe);
               }
            }

            GravityOverride.set(player, GRAVITY_ID, 0.03);
            player.resetFallDistance();
            this.guard.arm(player);
            LimitlessFx.pulling(player, moved);
         }
      }
   }

   private void endHold(ServerPlayer player, boolean arrived) {
      BlueAbility.Hold hold = this.holds.remove(player.getUUID());
      BlueHoldState.set(player, BlueHoldState.of(player).ended());
      GravityOverride.clear(player, GRAVITY_ID);
      if (hold != null) {
         PowerService.stopHeldAnimation(player, "limitless_blue_hold");
         hold.orb.letGo();
         player.resetFallDistance();
         this.guard.arm(player);
         LimitlessFx.pullEnded(player);
         PowerService.sync(player);
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.presses.remove(player.getUUID());
      this.endHold(player, false);
      BlueField field = this.fields.remove(player.getUUID());
      if (field != null) {
         field.end(player.serverLevel());
      }

      this.guard.clear(player);
      super.clearPlayer(player);
   }

   @Nullable
   private static LivingEntity foeNear(ServerPlayer player) {
      List<LivingEntity> near = player.serverLevel()
         .getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(8.0), e -> e != player && e.isAlive() && Foes.counts(player, e));
      return near.isEmpty() ? null : near.get(0);
   }

   private static final class Hold {
      final Vec3 target;
      final BlueOrbEntity orb;
      Vec3 lastPos;
      double travelled;
      boolean rewarded;
      int ticks;

      Hold(Vec3 target, BlueOrbEntity orb, Vec3 from) {
         this.target = target;
         this.orb = orb;
         this.lastPos = from;
      }
   }

   private static final class Press {
      final BlueAim.Tap orb;
      final Vec3 pullTo;
      final Vec3 pullOrb;
      int ticks;

      Press(BlueAim.Tap orb, Vec3 pullTo, Vec3 pullOrb) {
         this.orb = orb;
         this.pullTo = pullTo;
         this.pullOrb = pullOrb;
      }
   }
}
