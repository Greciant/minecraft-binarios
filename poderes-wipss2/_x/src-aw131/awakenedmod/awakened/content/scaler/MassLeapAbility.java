package com.awakenedmod.awakened.content.scaler;

import com.awakenedmod.awakened.content.vfx.Vfx;
import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Headroom;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.RealisticMovement;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.StaminaSystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.phys.Vec3;

public final class MassLeapAbility extends Ability {
   private static final int UNLOCK = 3;
   private static final float COST = 10.0F;
   private static final int MAX_CHARGE_TICKS = 16;
   private static final float MIN_CHARGE_FRAC = 0.4F;
   private static final double STEER_RATE = 0.1;
   private static final float FULL_SPEED = 2.2F;
   private static final float SPRING_MIN = 0.6F;
   private static final float SPRING_MAX = 1.8F;
   private static final double LOOK_FLAT = 0.3;
   private static final double FLAT_LIFT = 0.45;
   private static final double BRACE_SLOW = -0.85;
   public static final ResourceLocation BRACE_SLOW_ID = AwId.of("scaler_leap_brace");
   private static final float COMPRESS_START = 0.3F;
   private static final float COMPRESS_PER_CHARGE = 0.7F;
   private static final float COMPRESS_LAUNCH_BONUS = 0.35F;
   private static final int REGROW_DELAY_TICKS = 6;
   private static final int MIN_AIRBORNE_TICKS = 2;
   private static final int MAX_FLIGHT_TICKS = 120;
   private final Map<UUID, MassLeapAbility.Charge> charging = this.activeState();
   private final Map<UUID, MassLeapAbility.Flight> flying = this.playerState();

   public MassLeapAbility() {
      super("mass_leap", 10.0F, 3);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsRange().build();
   }

   private static float fxScale(ServerPlayer player) {
      return Mth.clamp(player.getScale(), 0.12F, 2.5F);
   }

   @Override
   public boolean requiresFootingWhenRealistic() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      return this.startCharge(player, false);
   }

   @Override
   public boolean usesSneakActivate() {
      return true;
   }

   @Override
   public void onSecondaryAction(ServerPlayer player, int action) {
      if (action == 3) {
         if (RealisticMovement.blocksAirLaunch(player)) {
            this.fail(player, "awakened.feedback.no_footing", new Object[0]);
         } else if (this.charging.containsKey(player.getUUID())) {
            this.fail(player, "awakened.scaler.leap_busy", new Object[0]);
         } else {
            PowerData data = PowerService.data(player);
            float cost = this.billed(10.0F);
            if (!StaminaSystem.canAfford(data, cost)) {
               this.fail(player, "awakened.scaler.tired", new Object[0]);
            } else {
               if (!data.infiniteStamina()) {
                  data.setStamina(data.stamina() - cost);
                  data.setLastSpendTime(player.serverLevel().getGameTime());
               }

               this.startCharge(player, true);
            }
         }
      }
   }

   private boolean startCharge(ServerPlayer player, boolean compressed) {
      if (this.charging.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.scaler.leap_busy", new Object[0]);
      } else {
         this.charging.put(player.getUUID(), new MassLeapAbility.Charge(compressed));
         Scaling.setModifier(player, Attributes.MOVEMENT_SPEED, BRACE_SLOW_ID, -0.85, Operation.ADD_MULTIPLIED_TOTAL);
         PowerService.playAnimation(player, "scaler_leap_charge");
         ServerLevel level = player.serverLevel();
         float s = fxScale(player);
         Fx.burst(level, ParticleTypes.CLOUD, player.position().add(0.0, 0.15 * (double)s, 0.0), Math.max(2, Math.round(8.0F * s)), 0.35 * (double)s, 0.03);
         Fx.sound(level, player.position(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.8F, 0.6F);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.tickCharge(player);
      this.tickFlight(player);
   }

   private void tickCharge(ServerPlayer player) {
      MassLeapAbility.Charge charge = this.charging.get(player.getUUID());
      if (charge != null) {
         if (charge.compressed && !player.isShiftKeyDown()) {
            this.abortCharge(player);
         } else {
            charge.ticks = Math.min(16, charge.ticks + 1);
            float f = (float)charge.ticks / 16.0F;
            if (charge.compressed) {
               PowerService.data(player).setVar("scaler_compress", 0.3F + 0.7F * f);
            }

            ServerLevel level = player.serverLevel();
            float s = fxScale(player);
            if (s >= 0.5F || charge.ticks % 2 == 0) {
               Fx.burst(
                  level,
                  ParticleTypes.CLOUD,
                  player.position().add(0.0, 0.1 * (double)s, 0.0),
                  Math.max(1, Math.round((1.0F + f * 2.0F) * s)),
                  (0.3 + 0.2 * (double)f) * (double)s,
                  0.02
               );
            }

            if (charge.ticks % 8 == 0) {
               Fx.sound(level, player.position(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.5F, 0.6F + f * 0.8F);
            }
         }
      }
   }

   private void abortCharge(ServerPlayer player) {
      if (this.charging.remove(player.getUUID()) != null) {
         PowerService.stopAnimation(player);
         Scaling.removeModifier(player, Attributes.MOVEMENT_SPEED, BRACE_SLOW_ID);
         PowerService.data(player).setVar("scaler_compress", 0.0F);
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      MassLeapAbility.Charge charge = this.charging.remove(player.getUUID());
      if (charge != null) {
         Scaling.removeModifier(player, Attributes.MOVEMENT_SPEED, BRACE_SLOW_ID);
         this.launch(player, charge);
      }
   }

   private void launch(ServerPlayer player, MassLeapAbility.Charge charge) {
      HitchControl.detach(player, false);
      float chargeFrac = Mth.clamp((float)charge.ticks / 16.0F, 0.0F, 1.0F);
      float f = 0.4F + 0.6F * chargeFrac;
      PowerData data = PowerService.data(player);
      float spring = Mth.clamp((float)(1.0 / Math.sqrt((double)Scaling.stage(data).scale())), 0.6F, 1.8F);
      float depth = charge.compressed ? 0.3F + 0.7F * chargeFrac : 0.0F;
      double speed = (double)(MasteryScaling.scaled(2.2F * f, player) * (1.0F + 0.35F * depth));
      Vec3 look = player.getLookAngle();
      Vec3 velocity = new Vec3(look.x * speed * (double)spring, look.y * speed * Math.sqrt((double)spring), look.z * speed * (double)spring);
      if (Math.abs(look.y) < 0.3) {
         velocity = velocity.add(0.0, 0.45 * (double)f, 0.0);
      }

      player.setDeltaMovement(velocity);
      player.hurtMarked = true;
      player.resetFallDistance();
      this.flying.put(player.getUUID(), new MassLeapAbility.Flight(charge.compressed));
      ServerLevel level = player.serverLevel();
      float scale = fxScale(player);
      Vec3 feet = player.position().add(0.0, 0.1 * (double)scale, 0.0);
      Vfx.ring(level, feet, (0.8 + (double)f * 0.6) * (double)scale, 6021288, 10);
      Fx.burst(level, ParticleTypes.CLOUD, feet, Math.max(3, Math.round((14.0F + f * 16.0F) * Math.min(scale, 2.0F))), 0.5 * (double)scale, 0.12);
      Fx.sound(level, player.position(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 1.1F, 0.7F);
      Fx.sound(level, player.position(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.7F, 1.5F);
      PowerService.playAnimation(player, "scaler_leap_launch");
   }

   private void tickFlight(ServerPlayer player) {
      MassLeapAbility.Flight flight = this.flying.get(player.getUUID());
      if (flight != null) {
         player.resetFallDistance();
         PowerData data = PowerService.data(player);
         if (!player.onGround()) {
            steer(player);
         }

         if (flight.compressed && flight.ticks >= 6 && Headroom.fits(player, (double)Scaling.stage(data).scale())) {
            regrow(player, data, flight);
         }

         ServerLevel level = player.serverLevel();
         float s = fxScale(player);
         Fx.burst(
            level, Fx.dust(6021288, 1.1F * s), player.position().add(0.0, (double)player.getBbHeight() * 0.4, 0.0), s < 0.5F ? 1 : 2, 0.15 * (double)s, 0.0
         );
         boolean landed = flight.airborneTicks >= 2 && (player.onGround() || player.isInWater());
         if (!landed && ++flight.ticks < 120) {
            if (!player.onGround()) {
               flight.airborneTicks++;
            }
         } else {
            this.flying.remove(player.getUUID());
            if (flight.compressed) {
               data.setVar("scaler_compress", 0.0F);
            }
         }
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

   private static void regrow(ServerPlayer player, PowerData data, MassLeapAbility.Flight flight) {
      flight.compressed = false;
      data.setVar("scaler_compress", 0.0F);
      ServerLevel level = player.serverLevel();
      Fx.sound(level, player.position(), SoundEvents.SLIME_JUMP, SoundSource.PLAYERS, 0.7F, 1.1F);
      Fx.burst(level, ParticleTypes.CLOUD, player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0), 4, 0.25, 0.04);
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      this.abortCharge(player);
      Scaling.removeModifier(player, Attributes.MOVEMENT_SPEED, BRACE_SLOW_ID);
      this.flying.remove(player.getUUID());
      PowerService.data(player).setVar("scaler_compress", 0.0F);
      super.clearPlayer(player);
   }

   private static final class Charge {
      private final boolean compressed;
      private int ticks;

      private Charge(boolean compressed) {
         this.compressed = compressed;
      }
   }

   private static final class Flight {
      private boolean compressed;
      private int ticks;
      private int airborneTicks;

      private Flight(boolean compressed) {
         this.compressed = compressed;
      }
   }
}
