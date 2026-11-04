package com.awakenedmod.awakened.content.limitless.purple;

import com.awakenedmod.awakened.content.limitless.LimitlessCast;
import com.awakenedmod.awakened.content.limitless.LimitlessHands;
import com.awakenedmod.awakened.content.limitless.LimitlessPower;
import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.FallGuard;
import com.awakenedmod.awakened.core.Lab;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.power.PowerResource;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.Vec3;

public final class PurpleAbility extends Ability {
   public static final String ID = "limitless_purple";
   private static final String ANIM_CHARGE = "limitless_purple_charge";
   private static final String ANIM_RELEASE = "limitless_purple_release";
   private static final Lab.Value BEAT = Lab.SERVER.def("limitless.purple.beat_ticks", 10.0, 4.0, 30.0, 1.0);
   private static final Lab.Value PULL = Lab.SERVER.def("limitless.purple.pull_ticks", 6.0, 2.0, 20.0, 1.0);
   private static final Lab.Value RECOIL = Lab.SERVER.def("limitless.purple.recoil", 0.45, 0.0, 1.5, 0.05);
   private static final Lab.Value SLOW = Lab.SERVER.def("limitless.purple.over_slow", 0.6, 0.0, 1.0, 0.05);
   private static final float PULL_HUSH = 0.2F;
   private static final ResourceLocation SLOW_ID = AwId.of("limitless_purple_overcharge_slow");
   private static final ResourceLocation FALL_GUARD_ID = AwId.of("limitless_purple_recoil_guard");
   private static final int GUARD_GRACE = 8;
   private static final int GUARD_MAX = 30;
   private final Map<UUID, PurpleAbility.Charge> charges = this.activeState();
   private final Map<UUID, List<PurpleSweep>> sweeps = this.playerState();
   private final FallGuard guard = new FallGuard(FALL_GUARD_ID, 8, 30);

   public PurpleAbility() {
      super("limitless_purple", 45.0F, 5);
      PurpleVoices.preloadLabKeys();
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of()
         .damage((double)PurpleStrength.NORMAL.damage())
         .value("limitless_purple_over_damage", AbilityScaling.Unit.RAW, (double)PurpleStrength.OVERCHARGED.damage())
         .build();
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (!this.charges.containsKey(player.getUUID()) && !LimitlessPower.castingOther(player, this)) {
         int now = (int)player.serverLevel().getGameTime();
         boolean over = PowerService.data(player).masteryLevel() >= 8;
         PurpleAbility.Charge charge = new PurpleAbility.Charge(PurpleCast.started(now, over, BEAT.getI(), PULL.getI()));
         this.charges.put(player.getUUID(), charge);
         PurpleCast.set(player, charge.cast);
         PowerService.playAnimation(player, "limitless_purple_charge", true);
         return true;
      } else {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      PurpleAbility.Charge charge = this.charges.get(player.getUUID());
      if (charge != null) {
         this.hold(player, charge);
      }

      this.tickSweeps(player);
      this.guard.tick(player, false);
   }

   private void hold(ServerPlayer player, PurpleAbility.Charge charge) {
      ServerLevel level = player.serverLevel();
      int now = (int)level.getGameTime();
      PurpleCast cast = charge.cast;
      int ticks = now - cast.start();
      if (cast.pulling()) {
         PurpleVoices.charging(level, player, ticks, 1.0F - 0.8F * cast.pullIn((double)now));
         if (now >= cast.launchAt()) {
            this.launch(player, charge);
         }
      } else {
         PurpleVoices.charging(level, player, ticks, 1.0F);
         if (ticks >= 50) {
            if (!cast.over()) {
               if (ticks >= 150) {
                  this.pullIn(player, charge, PurpleStrength.NORMAL);
               }
            } else {
               this.overcharge(player, charge, now, ticks);
            }
         }
      }
   }

   private void overcharge(ServerPlayer player, PurpleAbility.Charge charge, int now, int ticks) {
      PurpleCast cast = charge.cast;
      if (cast.full((double)now)) {
         this.pullIn(player, charge, PurpleStrength.OVERCHARGED);
      } else {
         int beat = (int)cast.beats((double)now) + 1;
         if (beat > charge.beatsHeard) {
            charge.beatsHeard = beat;
            PurpleVoices.beat(player.serverLevel(), player, beat);
         }

         PowerResource resource = this.resource(player);
         float drain = StaminaSystem.effectiveCost(45.0F / (float)(4 * cast.beat()), this.balanceKey());
         if (!resource.canAfford(player, drain)) {
            PowerService.feedback(player, 0);
            this.pullIn(player, charge, PurpleStrength.NORMAL);
         } else {
            resource.sustain(player, drain);
            slow(player, true);
            if (player.isSprinting()) {
               player.setSprinting(false);
            }

            if (ticks % 5 == 0) {
               PowerService.sync(player);
            }
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      PurpleAbility.Charge charge = this.charges.get(player.getUUID());
      if (charge != null && !charge.cast.pulling()) {
         if ((int)player.serverLevel().getGameTime() - charge.cast.start() >= 50) {
            this.pullIn(player, charge, PurpleStrength.NORMAL);
         } else {
            this.cancel(player);
         }
      }
   }

   private void pullIn(ServerPlayer player, PurpleAbility.Charge charge, PurpleStrength strength) {
      charge.cast = charge.cast.pulling((int)player.serverLevel().getGameTime(), strength);
      PurpleCast.set(player, charge.cast);
      if (charge.cast.over()) {
         PowerService.sync(player);
      }
   }

   private void launch(ServerPlayer player, PurpleAbility.Charge charge) {
      this.charges.remove(player.getUUID());
      ServerLevel level = player.serverLevel();
      int now = (int)level.getGameTime();
      PurpleStrength strength = charge.cast.strength();
      Vec3 merge = LimitlessHands.purpleMerge(player.getEyePosition(), (double)player.getYRot(), (double)player.getXRot());
      Vec3 fwd = player.getLookAngle().normalize();
      PurpleSphereEntity sphere = PurpleSphereEntity.spawn(
         level, player.getId(), charge.cast.start(), now, merge, fwd, LimitlessCast.seed(player.getId(), charge.cast.start()), strength
      );
      float damage = MasteryScaling.scaledDamage(strength.damage(), player, this.balanceKey());
      this.sweeps.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(new PurpleSweep(sphere, damage));
      PurpleCast.set(player, charge.cast.ended(now, true));
      PurpleVoices.chargeEnded(level, player, true);
      PurpleWake.launch(level, player, merge, fwd, strength);
      slow(player, false);
      this.recoil(player, strength);
      PowerService.stopHeldAnimation(player, "limitless_purple_charge");
      PowerService.playAnimation(player, "limitless_purple_release");
   }

   private void recoil(ServerPlayer player, PurpleStrength strength) {
      double push = RECOIL.get() * Math.sqrt(strength.ballScale());
      Vec3 look = player.getLookAngle();
      Vec3 back = new Vec3(-look.x, 0.0, -look.z);
      if (!(push <= 0.0) && !(back.lengthSqr() < 1.0E-4) && player.onGround() && !player.isPassenger() && !player.isFallFlying()) {
         back = back.normalize().scale(push);
         Vec3 was = player.getDeltaMovement();
         player.setDeltaMovement(was.x * 0.3 + back.x, was.y, was.z * 0.3 + back.z);
         player.hurtMarked = true;
         player.resetFallDistance();
         this.guard.arm(player);
      }
   }

   private static void slow(ServerPlayer player, boolean on) {
      Attrs.set(player, Attributes.MOVEMENT_SPEED, SLOW_ID, on ? -SLOW.get() : 0.0, Operation.ADD_MULTIPLIED_TOTAL);
   }

   private void cancel(ServerPlayer player) {
      PurpleAbility.Charge charge = this.charges.remove(player.getUUID());
      if (charge != null) {
         this.resource(player).refund(player, this.effectiveCost(player) * 0.5F);
         int now = (int)player.serverLevel().getGameTime();
         PurpleCast.set(player, charge.cast.ended(now, false));
         PurpleVoices.chargeEnded(player.serverLevel(), player, false);
         slow(player, false);
         PowerService.stopHeldAnimation(player, "limitless_purple_charge");
      }
   }

   private void tickSweeps(ServerPlayer player) {
      List<PurpleSweep> list = this.sweeps.get(player.getUUID());
      if (list != null) {
         ServerLevel level = player.serverLevel();
         list.removeIf(s -> !s.tick(player, level));
         if (list.isEmpty()) {
            this.sweeps.remove(player.getUUID());
         }
      }
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.cancel(player);
      this.guard.clear(player);
      List<PurpleSweep> list = this.sweeps.remove(player.getUUID());
      if (list != null) {
         list.forEach(PurpleSweep::end);
      }

      super.clearPlayer(player);
   }

   private static final class Charge {
      PurpleCast cast;
      int beatsHeard;

      Charge(PurpleCast cast) {
         this.cast = cast;
      }
   }
}
