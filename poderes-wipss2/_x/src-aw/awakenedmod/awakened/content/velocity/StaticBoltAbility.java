package com.awakenedmod.awakened.content.velocity;

import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.data.PowerData;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import com.awakenedmod.awakened.core.quest.QuestSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class StaticBoltAbility extends VelocityHoldAbility {
   public static final String ID = "static_bolt";
   public static final double RANGE = 32.0;
   private static final double AIM_CONE_DEG = 5.0;
   private static final int UNLOCK_LEVEL = 5;
   private static final float MIN_SPEND = 20.0F;
   private static final float DRAIN_PER_SECOND = 30.0F;
   private static final float MAX_SPEND = 100.0F;
   private static final int CRACKLE_EVERY = 5;
   private static final int CAP_SECONDS = 60;
   private static final float BASE_DAMAGE = 4.0F;
   private static final float DAMAGE_PER_POWER = 10.0F;
   private static final float FULL_BAR_DAMAGE = 14.0F;
   private static final double KNOCKBACK = 0.4;
   private static final int CHAIN_GEAR = 3;
   private static final double CHAIN_RANGE = 6.0;
   private static final float CHAIN_FRACTION = 0.6F;
   private static final int CHAIN_TARGETS = 1;
   private static final float FEAT_POWER = 0.99F;
   private static final String FULL_BOLT_FEAT = "velocity_full_bolt";
   private static final int TICKS_PER_SECOND = 20;
   private static final int CAP_TICKS = 1200;
   private static final float DRAIN_PER_TICK = 1.5F;
   public static final String HOLD_CLIP = "velocity_bolt_hold";
   private static final String THROW_CLIP = "velocity_static_bolt";
   private static final float CRACKLE_VOLUME = 0.16F;
   private static final float CRACKLE_PITCH_MIN = 1.15F;
   private static final float CRACKLE_PITCH_MAX = 2.0F;
   private final Map<UUID, StaticBoltAbility.PendingCrack> pending = this.playerState();

   public StaticBoltAbility() {
      super("static_bolt", 5, 1200, VelocityHoldAbility.Input.HELD);
   }

   @Override
   protected VelocityHoldAbility.Hold newHold() {
      return new StaticBoltAbility.BoltHold();
   }

   @Override
   protected boolean begin(ServerPlayer player, VelocityHoldAbility.Hold hold) {
      PowerData data = PowerService.data(player);
      if (Momentum.value(data) < 20.0F) {
         return this.fail(player, "awakened.feedback.velocity_no_momentum", new Object[0]);
      } else {
         pour(player, data, (StaticBoltAbility.BoltHold)hold, 20.0F);
         PowerService.playAnimation(player, "velocity_bolt_hold");
         return true;
      }
   }

   @Override
   protected void tick(ServerPlayer player, VelocityHoldAbility.Hold hold) {
      StaticBoltAbility.BoltHold bolt = (StaticBoltAbility.BoltHold)hold;
      PowerData data = PowerService.data(player);
      float take = Math.min(1.5F, Math.min(100.0F - bolt.spent, Momentum.value(data)));
      if (take > 0.0F) {
         pour(player, data, bolt, take);
      }

      if (hold.ticks % 5 == 0) {
         Fx.sound(player.serverLevel(), player.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.16F, Mth.lerp(power(bolt), 1.15F, 2.0F));
      }
   }

   @Override
   protected void end(ServerPlayer player, VelocityHoldAbility.Hold hold, VelocityHoldAbility.End end) {
      StaticBoltAbility.BoltHold bolt = (StaticBoltAbility.BoltHold)hold;
      VelocityVisualState.setCharge(player, 0);
      PowerService.stopAnimation(player);
      if (end != VelocityHoldAbility.End.TORN_DOWN) {
         this.throwBolt(player, power(bolt));
      }
   }

   private void throwBolt(ServerPlayer player, float power) {
      ServerLevel level = player.serverLevel();
      PowerData data = PowerService.data(player);
      int gear = Momentum.chosenGear(data);
      StaticBoltAbility.Aim aim = aim(player);
      Vec3 from = VelocityFx.throwHand(player);
      PowerService.playAnimation(player, "velocity_static_bolt");
      VelocityFx.throwBolt(level, VelocityPalette.of(player), from, aim.point(), gear, power);
      VelocityFx.gearBurst(level, player, gear, false);
      this.pending.put(player.getUUID(), new StaticBoltAbility.PendingCrack(level, aim.point(), VelocityFx.boltTravelTicks()));
      this.strike(player, level, data, aim, from, gear, power);
   }

   private void strike(ServerPlayer player, ServerLevel level, PowerData data, StaticBoltAbility.Aim aim, Vec3 from, int gear, float power) {
      LivingEntity first = aim.target();
      if (first != null) {
         float damage = Momentum.empoweredDamage(player, 4.0F + 10.0F * power, this.balanceKey());
         List<LivingEntity> struck = new ArrayList<>(2);
         if (hit(player, level, first, damage, aim.point().subtract(from), struck)) {
            if (Gearbox.effectiveGear(player, data) >= 3) {
               LivingEntity next = chainTarget(player, level, first);
               if (next != null) {
                  Vec3 a = first.getBoundingBox().getCenter();
                  Vec3 b = next.getBoundingBox().getCenter();
                  VelocityFx.chainBolt(level, VelocityPalette.of(player), a, b, gear, power);
                  hit(player, level, next, damage * 0.6F, b.subtract(a), struck);
               }
            }

            report(player, struck, power);
         }
      }
   }

   private static boolean hit(ServerPlayer player, ServerLevel level, LivingEntity target, float damage, Vec3 along, List<LivingEntity> struck) {
      if (!Dmg.hurtNoKnockback(target, level, Dmg.by(player, DamageTypes.LIGHTNING_BOLT), damage)) {
         return false;
      } else {
         Vec3 flat = AimUtil.horizontalDir(Vec3.ZERO, along);
         Harm.knockback(player, target, Momentum.empowered(player, 0.4), -flat.x, -flat.z);
         struck.add(target);
         return true;
      }
   }

   @Nullable
   private static LivingEntity chainTarget(ServerPlayer player, ServerLevel level, LivingEntity first) {
      Vec3 centre = first.getBoundingBox().getCenter();
      LivingEntity best = null;
      double bestSqr = 36.0;

      for (LivingEntity candidate : level.getEntitiesOfClass(
         LivingEntity.class, new AABB(centre, centre).inflate(6.0), e -> e != player && e != first && e.isAlive()
      )) {
         if (Harm.allowed(player, candidate)) {
            double distSqr = candidate.getBoundingBox().getCenter().distanceToSqr(centre);
            if (distSqr < bestSqr) {
               bestSqr = distSqr;
               best = candidate;
            }
         }
      }

      return best;
   }

   private static void report(ServerPlayer player, List<LivingEntity> struck, float power) {
      MasterySystem.award(player, MasteryReason.HIT, struck);
      if (!(power < 0.99F)) {
         for (LivingEntity victim : struck) {
            if (Foes.defeatedBy(victim, player)) {
               QuestSystem.feat(player, "velocity_full_bolt");
               return;
            }
         }
      }
   }

   private static float power(StaticBoltAbility.BoltHold hold) {
      return Mth.clamp(hold.spent / 100.0F, 0.2F, 1.0F);
   }

   private static void pour(ServerPlayer player, PowerData data, StaticBoltAbility.BoltHold hold, float amount) {
      Momentum.set(data, Momentum.value(data) - amount);
      hold.spent = Math.min(100.0F, hold.spent + amount);
      VelocityVisualState.setCharge(player, Math.round(hold.spent / 100.0F * 100.0F));
      VelocityPower.momentumSpent(player);
   }

   @Override
   public void serverTick(ServerPlayer player) {
      this.tickPendingCrack(player);
      super.serverTick(player);
   }

   private void tickPendingCrack(ServerPlayer player) {
      StaticBoltAbility.PendingCrack crack = this.pending.get(player.getUUID());
      if (crack != null) {
         if (crack.ticksLeft() <= 1) {
            this.pending.remove(player.getUUID());
            if (player.serverLevel() == crack.level()) {
               VelocityFx.boltImpactCrack(crack.level(), crack.pos());
            }
         } else {
            this.pending.put(player.getUUID(), crack.tick());
         }
      }
   }

   static StaticBoltAbility.Aim aim(ServerPlayer player) {
      Vec3 eye = player.getEyePosition();
      Vec3 point = AimUtil.aimedPosition(player, 32.0);
      LivingEntity target = AimUtil.aimedLivingSoft(player, 32.0, 5.0, e -> Harm.allowed(player, e));
      if (target != null) {
         Vec3 body = target.getBoundingBox().getCenter();
         if (body.distanceToSqr(eye) < point.distanceToSqr(eye)) {
            return new StaticBoltAbility.Aim(target, body);
         }
      }

      return new StaticBoltAbility.Aim(null, point);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(14.0).knockback(0.4).build();
   }

   static record Aim(@Nullable LivingEntity target, Vec3 point) {
   }

   private static final class BoltHold extends VelocityHoldAbility.Hold {
      float spent;
   }

   private static record PendingCrack(ServerLevel level, Vec3 pos, int ticksLeft) {
      StaticBoltAbility.PendingCrack tick() {
         return new StaticBoltAbility.PendingCrack(this.level, this.pos, this.ticksLeft - 1);
      }
   }
}
