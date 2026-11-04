package com.awakenedmod.awakened.content.hydro;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.LoopSound;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.stamina.ChargeBill;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class WaterLanceAbility extends Ability {
   public static final int MAX_CHARGE_TICKS = 40;
   public static final int FULL_HOLD = 20;
   public static final String CHARGE_CLIP = "hydro_lance_charge";
   public static final String THROW_CLIP = "hydro_lance_throw";
   private static final double SPEED_BASE = 1.9;
   private static final double SPEED_AP_CAP = 2.2;
   private static final double SPEED_CHARGE_BONUS = 0.9;
   private static final double GRAVITY_TAP = 0.03;
   private static final double GRAVITY_FULL = 0.007;
   private static final int MAX_FLIGHT = 6;
   private static final int FEED_EVERY = 3;
   private static final int FEED_LIFE = 6;
   private static final String HUM = "hydro_lance";
   public static final double HAND_WIDTH = 0.31;
   public static final double HAND_DROP = 0.25;
   public static final double HAND_REACH = 0.7;
   public static final float HOLD_LIFT_DEG = 75.0F;
   public static final float HOLD_TWIST_DEG = 25.0F;
   private static final float WATER_TAP = 0.25F;
   private static final float WATER_FULL = 1.5F;
   private static final float STAMINA_TAP = 5.0F;
   private static final float STAMINA_FULL = 16.0F;
   private static final float DAMAGE_TAP = 4.0F;
   private static final float DAMAGE_FULL = 13.0F;
   private final Map<UUID, WaterLanceAbility.Charge> charging = this.activeState();
   private final Map<UUID, Deque<HydroLance>> flying = this.playerState();

   public WaterLanceAbility() {
      super("water_lance", 5.0F, 0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public float staminaCost(ServerPlayer player) {
      return 0.0F;
   }

   @Override
   public String costLabel() {
      return (int)this.billed(5.0F) + "-" + (int)this.billed(16.0F);
   }

   @Override
   public String activationAnimation(ServerPlayer player) {
      return null;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      if (this.charging.containsKey(player.getUUID())) {
         return this.fail(player, "awakened.fail.busy", new Object[0]);
      } else if (!this.charge(player, 5.0F)) {
         HydroFeedback.tired(player);
         return false;
      } else {
         Hydro.Cast look = Hydro.peek(player);
         WaterLanceAbility.Charge charge = new WaterLanceAbility.Charge(look.wet(), ChargeBill.opened(this.billed(5.0F), this.billed(16.0F)));
         this.charging.put(player.getUUID(), charge);
         PowerService.playAnimation(player, "hydro_lance_charge", true);
         HydroVisual.startLance(player, look.wet());
         Hydro.show(player, look, hand(player));
         HydroFx.sound(player.serverLevel(), player.position(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, 0.5F, 1.4F);
         return true;
      }
   }

   @Override
   public void serverTick(ServerPlayer player) {
      WaterLanceAbility.Charge charge = this.charging.get(player.getUUID());
      if (charge != null) {
         charge.ticks++;
         float f = charge.fraction();
         charge.bill.payTo(player, this.resource(player), f);
         LoopSound.keep(
            player, "hydro_lance", SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.PLAYERS, Mth.lerp(f, 0.25F, 0.6F), Mth.lerp(f, 0.8F, 1.4F)
         );
         if (charge.wet && charge.ticks % 3 == 0) {
            Hydro.Cast feed = Hydro.peek(player);
            if (feed.wet()) {
               HydroFx.ribbon(player.serverLevel(), feed.origin(), hand(player), true, 6);
            }
         }

         if (charge.ticks == 40) {
            ready(player);
         }

         if (charge.ticks >= 60) {
            this.charging.remove(player.getUUID());
            this.fire(player, charge, 1.0F);
         }
      }

      Deque<HydroLance> lances = this.flying.get(player.getUUID());
      if (lances != null) {
         lances.removeIf(Entity::isRemoved);
         if (lances.isEmpty()) {
            this.flying.remove(player.getUUID());
         }
      }
   }

   @Override
   public void onRelease(ServerPlayer player) {
      WaterLanceAbility.Charge charge = this.charging.remove(player.getUUID());
      if (charge != null) {
         this.fire(player, charge, charge.fraction());
      }
   }

   private static void ready(ServerPlayer player) {
      HydroFx.sound(player.serverLevel(), hand(player), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 0.4F, 1.8F);
      Fx.shakeFor(player, player.getEyePosition(), 4.0, 0.03F, 0, 3);
   }

   @Override
   protected boolean cancelsOnDimensionChange() {
      return true;
   }

   @Override
   public void clearPlayer(ServerPlayer player) {
      Deque<HydroLance> lances = this.flying.get(player.getUUID());
      if (lances != null) {
         lances.forEach(Entity::discard);
      }

      WaterLanceAbility.Charge charge = this.charging.get(player.getUUID());
      if (charge != null) {
         PowerService.stopHeldAnimation(player, "hydro_lance_charge");
         this.charging.remove(player.getUUID());
         charge.bill.refund(player, this.resource(player));
      }

      LoopSound.stop(player, "hydro_lance");
      HydroVisual.endLance(player);
      super.clearPlayer(player);
   }

   private void fire(ServerPlayer player, WaterLanceAbility.Charge charge, float f) {
      PowerService.stopHeldAnimation(player, "hydro_lance_charge");
      LoopSound.stop(player, "hydro_lance");
      HydroVisual.endLance(player);
      f = Math.min(f, charge.bill.paidCharge());
      PowerService.playAnimation(player, "hydro_lance_throw");
      Hydro.Cast cast = Hydro.take(player, Mth.lerp(f, 0.25F, 1.5F));
      Vec3 eye = player.getEyePosition();
      double speed = 1.9 * (1.0 + (double)f * 0.9) * Math.min(2.2, Math.max(1.0, (double)MasteryScaling.scaled(1.0F, player)));
      double gravity = Mth.lerp((double)f, 0.03, 0.007);
      LivingEntity locked = AimUtil.aimedLivingEntity(player, 128.0, e -> Harm.allowed(player, e));
      Vec3 dir;
      if (locked != null) {
         Vec3 tc = locked.position().add(0.0, (double)locked.getBbHeight() * 0.5, 0.0);
         double dist = Math.sqrt((tc.x - eye.x) * (tc.x - eye.x) + (tc.z - eye.z) * (tc.z - eye.z));
         double t = dist / Math.max(0.1, speed);
         dir = tc.add(0.0, 0.5 * gravity * t * t, 0.0).subtract(eye).normalize();
      } else {
         dir = player.getLookAngle();
      }

      int pierce = 1 + (int)(f * 3.0F);
      float damage = Hydro.damage(player, this, Mth.lerp(f, 4.0F, 13.0F), cast);
      double knock = 0.3 + (double)f * 0.4;
      boolean leftArm = player.getMainArm() == HumanoidArm.LEFT;
      HydroLance lance = HydroLance.launch(player, eye, dir.scale(speed), gravity, damage, knock, pierce, HydroLance.style(f, cast.wet(), leftArm));
      Deque<HydroLance> list = this.flying.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>());
      list.addLast(lance);

      while (list.size() > 6) {
         list.removeFirst().discard();
      }

      HydroFx.sound(player.serverLevel(), player.position(), SoundEvents.PLAYER_SPLASH, 0.6F, 1.25F - f * 0.35F);
      if (f >= 0.99F) {
         HydroFx.sound(player.serverLevel(), player.position(), (SoundEvent)SoundEvents.TRIDENT_THROW.value(), 0.8F, 1.1F);
      }

      if (cast.source() == Hydro.Source.NETHER) {
         Hydro.evaporate(player.serverLevel(), hand(player));
      }
   }

   private static Vec3 hand(ServerPlayer player) {
      double side = AimUtil.handSign(player, false);
      return AimUtil.shoulderHand(player.getEyePosition(), (double)player.yBodyRot - side * 25.0, -75.0, side, 0.31, 0.25, 0.7);
   }

   private static final class Charge {
      private final boolean wet;
      private final ChargeBill bill;
      private int ticks;

      private Charge(boolean wet, ChargeBill bill) {
         this.wet = wet;
         this.bill = bill;
      }

      private float fraction() {
         return Math.min(1.0F, (float)this.ticks / 40.0F);
      }
   }
}
