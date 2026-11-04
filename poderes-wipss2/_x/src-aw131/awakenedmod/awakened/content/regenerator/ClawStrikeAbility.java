package com.awakenedmod.awakened.content.regenerator;

import com.awakenedmod.awakened.core.Foes;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ClawStrikeAbility extends RegenAbility {
   private static final double RANGE = 3.2;
   private static final double FRONT_DOT = 0.3;
   private static final float DAMAGE = 7.0F;
   private static final float HEAL_PER_HIT = 2.0F;
   private static final double KNOCKBACK = 0.3;

   public ClawStrikeAbility() {
      super("claw_strike", 10.0F, 0);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(7.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      Vec3 eye = player.getEyePosition();
      Vec3 look = player.getLookAngle();
      Vec3 front = eye.add(look.scale(1.4));
      Fx.burst(level, ParticleTypes.SWEEP_ATTACK, front, 2, 0.3, 0.0);
      Fx.burst(level, ParticleTypes.CRIT, front, 10, 0.4, 0.1);
      Fx.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 0.9F);
      player.swing(InteractionHand.MAIN_HAND);
      PowerService.playAnimation(player, "claw_strike");
      float damage = MasteryScaling.scaledDamage(7.0F, player, this.balanceKey());
      int hits = 0;
      LivingEntity threat = null;

      for (LivingEntity target : level.getEntitiesOfClass(
         LivingEntity.class, player.getBoundingBox().inflate(3.2), e -> e != player && e.isAlive() && inFrontCone(eye, look, e) && Harm.allowed(player, e)
      )) {
         clawHit(player, target, damage);
         Harm.knockback(player, target, 0.3, player.getX() - target.getX(), player.getZ() - target.getZ());
         hits++;
         if (threat == null && Foes.counts(player, target)) {
            threat = target;
         }
      }

      if (hits > 0) {
         player.heal(2.0F);
         MasterySystem.award(player, MasteryReason.HIT, threat);
      }

      return true;
   }

   private static boolean inFrontCone(Vec3 eye, Vec3 look, LivingEntity target) {
      Vec3 toTarget = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0).subtract(eye);
      return toTarget.lengthSqr() < 1.0E-4 ? true : toTarget.normalize().dot(look) >= 0.3;
   }
}
