package com.awakenedmod.awakened.content.teleporter;

import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.AbilityScaling;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasteryScaling;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class PhaseStrikeAbility extends Ability {
   private static final float COST = 14.0F;
   private static final int UNLOCK = 3;
   private static final double RANGE = 18.0;
   private static final float DAMAGE = 7.0F;
   private static final double KNOCKBACK = 0.3;
   private static final int IFRAME_TICKS = 8;

   public PhaseStrikeAbility() {
      super("phase_strike", 14.0F, 3);
   }

   @Override
   public AbilityScaling scalingInfo() {
      return AbilityScaling.of().damage(7.0).build();
   }

   @Override
   public boolean activate(ServerPlayer player) {
      LivingEntity target = TeleportSupport.aimedEnemy(player, 18.0);
      if (target == null) {
         return this.fail(player, "awakened.teleporter.no_target", new Object[0]);
      } else {
         ServerLevel level = player.serverLevel();
         Vec3 toTarget = TeleportSupport.horizontalDir(player.position(), target.position());
         Vec3 landing = target.position().add(toTarget.scale(0.9));
         if (!TeleportSupport.fits(player, landing)) {
            landing = target.position().subtract(toTarget.scale(0.9));
         }

         if (!TeleportSupport.fits(player, landing)) {
            landing = target.position();
         }

         Vec3 fromCenter = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
         TeleFx.ghost(level, player);
         TeleportSupport.puff(level, fromCenter);
         player.teleportTo(landing.x, landing.y, landing.z);
         player.resetFallDistance();
         player.invulnerableTime = Math.max(player.invulnerableTime, 8);
         Vec3 toCenter = player.position().add(0.0, (double)player.getBbHeight() * 0.5, 0.0);
         TeleportSupport.puff(level, toCenter);
         TeleFx.trail(level, fromCenter, toCenter);
         Dmg.hurt(target, level, player.damageSources().playerAttack(player), MasteryScaling.scaledDamage(7.0F, player, this.balanceKey()));
         Harm.knockback(player, target, 0.3, player.getX() - target.getX(), player.getZ() - target.getZ());
         target.hurtMarked = true;
         player.swing(InteractionHand.MAIN_HAND);
         PowerService.playAnimation(player, "phase_strike");
         MasterySystem.award(player, MasteryReason.HIT, target);
         return true;
      }
   }
}
