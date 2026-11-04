package com.awakenedmod.awakened.content.teleporter;

import com.awakenedmod.awakened.core.Harm;
import com.awakenedmod.awakened.core.PowerService;
import com.awakenedmod.awakened.core.power.Ability;
import com.awakenedmod.awakened.core.progression.MasteryReason;
import com.awakenedmod.awakened.core.progression.MasterySystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class VanishingDropAbility extends Ability {
   private static final float COST = 20.0F;
   private static final int UNLOCK = 6;
   private static final double AIM_RANGE = 16.0;
   private static final double FALLBACK_RANGE = 6.0;
   private static final double MAX_HEIGHT = 14.0;
   private static final double MIN_HEIGHT = 4.0;

   public VanishingDropAbility() {
      super("vanishing_drop", 20.0F, 6);
   }

   @Override
   public boolean activate(ServerPlayer player) {
      ServerLevel level = player.serverLevel();
      LivingEntity target = TeleportSupport.aimedEnemy(player, 16.0);
      if (target == null || !Harm.allowed(player, target)) {
         target = nearest(player, level);
      }

      if (target == null) {
         return this.fail(player, "awakened.teleporter.no_enemy_near", new Object[0]);
      } else {
         double height = clearHeightAbove(level, target);
         if (height < 4.0) {
            return this.fail(player, "awakened.teleporter.no_headroom", new Object[0]);
         } else {
            PowerService.playAnimation(player, "vanishing_drop");
            TeleportSupport.puff(level, target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0));
            Vec3 lifted = target.position().add(0.0, height, 0.0);
            target.teleportTo(lifted.x, lifted.y, lifted.z);
            target.setDeltaMovement(0.0, 0.0, 0.0);
            target.resetFallDistance();
            target.hurtMarked = true;
            TeleportSupport.puff(level, lifted.add(0.0, (double)target.getBbHeight() * 0.5, 0.0));
            MasterySystem.award(player, MasteryReason.HIT, target);
            return true;
         }
      }
   }

   private static LivingEntity nearest(ServerPlayer player, ServerLevel level) {
      LivingEntity best = null;
      double bestSqr = 36.0;

      for (LivingEntity e : level.getEntitiesOfClass(
         LivingEntity.class, player.getBoundingBox().inflate(6.0), x -> x != player && x.isAlive() && Harm.allowed(player, x)
      )) {
         double d = e.distanceToSqr(player);
         if (d < bestSqr) {
            bestSqr = d;
            best = e;
         }
      }

      return best;
   }

   private static double clearHeightAbove(ServerLevel level, LivingEntity target) {
      for (double h = 14.0; h >= 4.0; h--) {
         AABB box = target.getBoundingBox().move(0.0, h, 0.0);
         if (level.noCollision(target, box)) {
            return h;
         }
      }

      return 0.0;
   }
}
