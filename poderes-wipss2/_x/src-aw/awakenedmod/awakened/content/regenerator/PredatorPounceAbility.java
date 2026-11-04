package com.awakenedmod.awakened.content.regenerator;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class PredatorPounceAbility extends RegenAbility {
   private static final int UNLOCK = 3;
   private static final double SPEED = 1.425;
   private static final double LIFT = 0.62;
   private static final int MIN_AIR_TICKS = 3;
   private static final int MAX_AIR_TICKS = 30;
   private static final double SLAM_RADIUS = 3.6;
   private static final float DAMAGE = 10.0F;
   private static final float HEAL_PER_FOE = 3.0F;
   private static final double KNOCKBACK = 0.7;
   private static final int WEAKNESS_TICKS = 100;
   private final Map<UUID, Integer> airborne = this.activeState();

   public PredatorPounceAbility() {
      super("predator_pounce", 16.0F, 3);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(10.0).build();
   }

   @Override
   public boolean requiresFootingWhenRealistic() {
      return true;
   }

   @Override
   public boolean activate(ServerPlayer player) {
      Vec3 look = player.getLookAngle();
      double vy = Math.max(0.62, look.y * 1.425);
      player.setDeltaMovement(look.x * 1.425, vy, look.z * 1.425);
      player.hurtMarked = true;
      player.resetFallDistance();
      this.airborne.put(player.getUUID(), 0);
      PowerService.playAnimation(player, "predator_pounce");
      Fx.burst(player.serverLevel(), ParticleTypes.CRIT, player.position().add(0.0, 0.2, 0.0), 12, 0.4, 0.05);
      Fx.sound(player.serverLevel(), player.position(), SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 0.9F, 1.4F);
      player.swing(InteractionHand.MAIN_HAND);
      return true;
   }

   @Override
   public void serverTick(ServerPlayer player) {
      Integer ticks = this.airborne.get(player.getUUID());
      if (ticks != null) {
         ServerLevel level = player.serverLevel();
         player.resetFallDistance();
         Fx.burst(level, ParticleTypes.CRIT, player.position().add(0.0, 0.5, 0.0), 2, 0.2, 0.01);
         int elapsed = ticks + 1;
         boolean land = elapsed >= 3 && player.onGround() || elapsed >= 30;
         if (!land) {
            this.airborne.put(player.getUUID(), elapsed);
         } else {
            this.airborne.remove(player.getUUID());
            PowerService.playAnimation(player, "predator_pounce_land");
            this.slam(player, level);
         }
      }
   }

   private void slam(ServerPlayer player, ServerLevel level) {
      Vec3 center = player.position();
      float damage = MasteryScaling.scaledDamage(10.0F, player, this.balanceKey());
      int hits = 0;
      LivingEntity threat = null;

      for (LivingEntity target : level.getEntitiesOfClass(
         LivingEntity.class,
         player.getBoundingBox().inflate(3.6),
         e -> e != player && e.isAlive() && e.distanceToSqr(center) <= 12.96 && Harm.allowed(player, e)
      )) {
         clawHit(player, target, damage);
         Harm.knockback(player, target, 0.7, center.x - target.getX(), center.z - target.getZ());
         Harm.effect(player, target, new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true));
         hits++;
         if (threat == null && Foes.counts(player, target)) {
            threat = target;
         }
      }

      if (hits > 0) {
         player.heal(3.0F * (float)hits);
         MasterySystem.award(player, MasteryReason.HIT, threat);
      }

      Fx.burst(level, ParticleTypes.EXPLOSION, center.add(0.0, 0.2, 0.0), 1, 0.0, 0.0);
      Fx.burst(level, ParticleTypes.CRIT, center.add(0.0, 0.2, 0.0), 30, 1.8, 0.2);
      Fx.burst(level, ParticleTypes.SWEEP_ATTACK, center.add(0.0, 0.4, 0.0), 2, 0.5, 0.0);
      Fx.sound(level, center, SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 1.2F);
      Fx.sound(level, center, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0F, 0.7F);
   }
}
