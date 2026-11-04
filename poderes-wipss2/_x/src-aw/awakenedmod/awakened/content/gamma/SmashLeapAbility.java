package com.awakenedmod.awakened.content.gamma;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class SmashLeapAbility extends Ability {
   private static final float COST = 18.0F;
   private static final int MAX_CHARGE_TICKS = 10;
   private static final float MIN_CHARGE_FRAC = 0.5F;
   private static final double H_SPEED = 4.2;
   private static final double V_SPEED = 1.4;
   private static final double PITCH_GAIN = 0.5;
   private static final double PITCH_H_TRADE = 0.35;
   private static final double RAGE_LOW = 0.6;
   private static final double RAGE_GAIN = 0.7;
   private static final double STEER_RATE = 0.1;
   private static final int MIN_AIRBORNE_TICKS = 2;
   private static final int MAX_FLIGHT_TICKS = 120;
   private static final float LAND_DAMAGE = 5.0F;
   private static final double LAND_RADIUS = 2.2;
   private static final double LAND_RADIUS_PER_RAGE = 2.2;
   private static final double LAND_KNOCKBACK = 0.9;
   private static final double LAND_KNOCKUP = 0.5;
   private static final double LAND_FALL_REF = 6.0;
   private final Map<UUID, SmashLeapAbility.Charge> charging = this.activeState();
   private final Map<UUID, SmashLeapAbility.Flight> flying = this.playerState();

   public SmashLeapAbility() {
      super("gamma_leap", 18.0F, 5);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(5.0).boostsRange().build();
   }

   @Override
   public boolean requiresFootingWhenRealistic() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.charging.containsKey(player.getUUID())) {
         return this.fail(player, "ability.awakened.gamma_leap.busy", new Object[0]);
      } else {
         this.charging.put(player.getUUID(), new SmashLeapAbility.Charge());
         PowerService.playAnimation(player, "gamma_leap_charge");
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.tickCharge(player);
      this.tickFlight(player);
   }

   private void tickCharge(ServerPlayer player) {
      SmashLeapAbility.Charge charge = this.charging.get(player.getUUID());
      if (charge != null) {
         charge.ticks = Math.min(10, charge.ticks + 1);
         GammaFx.leapChargeTick(player.serverLevel(), player, (float)charge.ticks / 10.0F);
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      SmashLeapAbility.Charge charge = this.charging.remove(player.getUUID());
      if (charge != null) {
         this.launch(player, charge);
      }
   }

   private void launch(ServerPlayer player, SmashLeapAbility.Charge charge) {
      float rage = Rage.rage(PowerService.data(player));
      float chargeFrac = Mth.clamp((float)charge.ticks / 10.0F, 0.0F, 1.0F);
      float f = 0.5F + 0.5F * chargeFrac;
      double power = (0.6 + 0.7 * (double)rage) * (double)MasteryScaling.scaled(f, player);
      Vec3 look = player.getLookAngle();
      Vec3 lookH = new Vec3(look.x, 0.0, look.z);
      Vec3 hDir = lookH.lengthSqr() < 1.0E-4 ? forward(player.getYRot()) : lookH.normalize();
      double up = look.y;
      double hSpeed = Math.max(0.0, 4.2 - Math.max(0.0, up) * 4.2 * 0.35) * power;
      double vSpeed = Math.max(0.25, 1.4 + up * 0.5) * power;
      player.setDeltaMovement(hDir.scale(hSpeed).add(0.0, vSpeed, 0.0));
      player.hurtMarked = true;
      player.resetFallDistance();
      this.flying.put(player.getUUID(), new SmashLeapAbility.Flight(rage, player.getY()));
      PowerService.stopAnimation(player);
      PowerService.playAnimation(player, "gamma_leap");
      GammaFx.leapLaunch(player.serverLevel(), player, rage);
   }

   private static Vec3 forward(float yawDeg) {
      double rad = Math.toRadians((double)yawDeg);
      return new Vec3(-Math.sin(rad), 0.0, Math.cos(rad));
   }

   private void tickFlight(ServerPlayer player) {
      SmashLeapAbility.Flight flight = this.flying.get(player.getUUID());
      if (flight != null) {
         player.resetFallDistance();
         flight.apexY = Math.max(flight.apexY, player.getY());
         if (!player.onGround()) {
            steer(player);
         }

         boolean landed = flight.airborneTicks >= 2 && (player.onGround() || player.isInWater());
         if (!landed && ++flight.ticks < 120) {
            if (!player.onGround()) {
               flight.airborneTicks++;
            }
         } else {
            this.flying.remove(player.getUUID());
            if (landed) {
               this.impact(player, flight.rage, flight.apexY - player.getY());
            }
         }
      }
   }

   private void impact(ServerPlayer player, float rage, double fell) {
      ServerLevel level = player.serverLevel();
      Vec3 pos = player.position();
      double fallScale = Mth.clamp(fell / 6.0 + 0.7, 0.7, 2.2);
      PowerService.playAnimation(player, "gamma_leap_land");
      GammaFx.leapLand(level, pos, rage, fallScale);
      double radius = (2.2 + 2.2 * (double)rage) * fallScale;
      float factor = (0.5F + rage) * (float)fallScale;
      float damage = MasteryScaling.scaledDamage(5.0F, player, this.balanceKey(), factor - 1.0F);
      int hits = 0;
      int threats = 0;

      for (LivingEntity victim : Harm.only(player, AimUtil.livingAround(player, pos, radius))) {
         Dmg.hurt(victim, level, Dmg.by(player, DamageTypes.PLAYER_ATTACK), damage);
         Vec3 push = victim.position().subtract(pos);
         push = push.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : new Vec3(push.x, 0.0, push.z).normalize();
         victim.push(push.x * 0.9 * (double)factor, 0.5, push.z * 0.9 * (double)factor);
         victim.hurtMarked = true;
         hits++;
         if (Foes.counts(player, victim)) {
            threats++;
         }
      }

      if (hits >= 2 && threats > 0) {
         MasterySystem.award(player, MasteryReason.HIT);
      }

      if (threats >= 2) {
         QuestSystem.feat(player, "gamma_quake");
      }
   }

   private static void steer(ServerPlayer player) {
      Vec3 v = player.getDeltaMovement();
      Vec3 horizontal = new Vec3(v.x, 0.0, v.z);
      double hSpeed = horizontal.length();
      if (!(hSpeed < 0.2)) {
         Vec3 look = player.getLookAngle();
         Vec3 lookH = new Vec3(look.x, 0.0, look.z);
         if (!(lookH.lengthSqr() < 1.0E-4)) {
            Vec3 dir = horizontal.scale(1.0 / hSpeed);
            Vec3 steered = dir.add(lookH.normalize().subtract(dir).scale(0.1)).normalize();
            player.setDeltaMovement(steered.x * hSpeed, v.y, steered.z * hSpeed);
            player.hurtMarked = true;
         }
      }
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      boolean wasCharging = this.charging.remove(player.getUUID()) != null;
      this.flying.remove(player.getUUID());
      if (wasCharging) {
         PowerService.stopAnimation(player);
      }

      super.clearPlayer(player);
   }

   private static final class Charge {
      private int ticks;
   }

   private static final class Flight {
      private final float rage;
      private double apexY;
      private int ticks;
      private int airborneTicks;

      private Flight(float rage, double launchY) {
         this.rage = rage;
         this.apexY = launchY;
      }
   }
}
