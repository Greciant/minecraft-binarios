package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuAbilities;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import com.devilfruits.registry.DFAttachments;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class HybridCombat {
   private static final ResourceLocation ROOTED = UoUoSeiryu.id("rooted");
   private static final Map<UUID, Long> ROOTED_UNTIL = new HashMap<>();

   public static float baseHit() {
      return (float)UoUoSeiryu.fruit().config().setting("hybridBaseHit");
   }

   public static float hit(AbilityContext ctx) {
      return ctx.damage() * baseHit();
   }

   public static float hit(AbilityContext ctx, double weight) {
      double stat = ctx.config().damage();
      return stat <= 0.0 ? (float)weight * baseHit() : (float)((double)ctx.damage() / stat * weight) * baseHit();
   }

   public static boolean strike(AbilityContext ctx, Entity target, float amount) {
      float before = 0.0F;
      if (target instanceof LivingEntity living) {
         before = living.getHealth();
         living.invulnerableTime = 0;
      }

      boolean landed = ctx.hurt(target, amount);
      if (landed) {
         float dealt = target instanceof LivingEntity living ? Math.max(0.0F, before - living.getHealth()) : amount;
         ServerFx.sendTo(ctx.player(), SeiryuFx.HIT, target, target.getBoundingBox().getCenter(), Vec3.ZERO, dealt, 0L, 0);
      }

      return landed;
   }

   @Nullable
   public static LivingEntity aimTarget(ServerPlayer player, double range, double halfAngleDeg) {
      Vec3 eye = player.getEyePosition();
      Vec3 look = AimHelper.look(player);
      AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.5);
      EntityHitResult aimed = ProjectileUtil.getEntityHitResult(
         player, eye, eye.add(look.scale(range)), box, ex -> ex instanceof LivingEntity && Targeting.canHurt(player, ex), range * range
      );
      if (aimed != null && aimed.getEntity() instanceof LivingEntity living && player.hasLineOfSight(living)) {
         return living;
      } else {
         LivingEntity best = null;
         double bestScore = Double.MAX_VALUE;

         for (Entity e : Targeting.inCone(
            player.serverLevel(), eye, look, range, Math.toRadians(halfAngleDeg), x -> x instanceof LivingEntity && Targeting.canHurt(player, x)
         )) {
            if (e instanceof LivingEntity) {
               LivingEntity living = (LivingEntity)e;
               if (player.hasLineOfSight(living)) {
                  Vec3 to = living.getBoundingBox().getCenter().subtract(eye);
                  double d = to.length();
                  double score = d * (2.0 - to.scale(1.0 / Math.max(1.0E-4, d)).dot(look));
                  if (score < bestScore) {
                     bestScore = score;
                     best = living;
                  }
               }
            }
         }

         return best;
      }
   }

   public static boolean veiled(Player player) {
      return player.hasData(DFAttachments.PLAYER_DATA) && DFAttachments.data(player).isToggleActive(SeiryuAbilities.THUNDERCLAD_VEIL.id());
   }

   public static float incomingFactor(Player victim) {
      return veiled(victim) ? (float)(1.0 - SeiryuAbilities.THUNDERCLAD_VEIL.config().extra("damageReduction")) : 1.0F;
   }

   public static float outgoingFactor(Player attacker) {
      return veiled(attacker) ? (float)(1.0 + SeiryuAbilities.THUNDERCLAD_VEIL.config().extra("damageBonus")) : 1.0F;
   }

   public static void root(ServerPlayer player, int ticks) {
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null && ticks > 0) {
         long until = player.level().getGameTime() + (long)ticks;
         ROOTED_UNTIL.merge(player.getUUID(), until, Math::max);
         speed.addOrUpdateTransientModifier(new AttributeModifier(ROOTED, -1.0, Operation.ADD_MULTIPLIED_TOTAL));
         ServerScheduler.later(player.serverLevel(), ticks, () -> {
            Long last = ROOTED_UNTIL.get(player.getUUID());
            if (last == null || player.level().getGameTime() >= last) {
               unroot(player);
            }
         });
      }
   }

   public static void unroot(ServerPlayer player) {
      ROOTED_UNTIL.remove(player.getUUID());
      AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
      if (speed != null) {
         speed.removeModifier(ROOTED);
      }
   }

   private HybridCombat() {
   }
}
