package com.awakenedmod.awakened.content.regenerator;

import com.awakenedmod.awakened.core.AimUtil;
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

public final class RendAbility extends RegenAbility {
   private static final int UNLOCK = 6;
   private static final double RANGE = 3.6;
   private static final float BASE_DAMAGE = 8.0F;
   private static final float EXECUTE_SCALE = 1.6F;
   private static final float HEAL_ON_HIT = 3.0F;
   private static final float HEAL_ON_KILL = 6.0F;
   private static final double KNOCKBACK = 0.3;

   public RendAbility() {
      super("rend", 12.0F, 6);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().boostsDamage().build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      LivingEntity target = AimUtil.aimedLivingEntity(player, 3.6, e -> Harm.allowed(player, e));
      if (target == null) {
         return this.fail(player, "awakened.regenerator.rend_no_target", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         float missing = 1.0F - target.getHealth() / Math.max(1.0F, target.getMaxHealth());
         float damage = MasteryScaling.scaledDamage(8.0F * (1.0F + 1.6F * missing), player, this.balanceKey());
         clawHit(player, target, damage);
         Harm.knockback(player, target, 0.3, player.getX() - target.getX(), player.getZ() - target.getZ());
         boolean killed = !target.isAlive() || target.getHealth() <= 0.0F;
         player.heal(killed ? 6.0F : 3.0F);
         Vec3 c = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
         Fx.burst(level, ParticleTypes.CRIT, c, 16, 0.4, 0.15);
         Fx.burst(level, ParticleTypes.SWEEP_ATTACK, c, 1, 0.0, 0.0);
         Fx.sound(level, player.position(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.8F);
         player.swing(InteractionHand.MAIN_HAND);
         PowerService.playAnimation(player, "rend");
         MasterySystem.award(player, killed ? MasteryReason.KILL : MasteryReason.HIT, target);
         return true;
      }
   }
}
