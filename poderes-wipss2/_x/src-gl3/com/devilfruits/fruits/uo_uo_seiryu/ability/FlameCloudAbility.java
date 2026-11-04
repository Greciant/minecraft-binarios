package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.AimHelper;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.BlockImpactService;
import com.devilfruits.core.physics.ImpactShape;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.core.util.ServerScheduler;
import com.devilfruits.core.util.Targeting;
import com.devilfruits.fruits.uo_uo_seiryu.SeiryuFx;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class FlameCloudAbility extends Ability {
   public FlameCloudAbility() {
      super(
         UoUoSeiryu.id("flame_cloud"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(20)
            .cooldown(100)
            .mastery(1)
            .damage(6.0)
            .range(8.0)
            .knockback(0.8)
            .extra("dashTicks", 6.0, 2.0, 20.0, "Duration of the dash.")
            .extra("lingerTicks", 40.0, 0.0, 200.0, "How long the flame clouds keep burning.")
            .extra("burnSeconds", 4.0, 0.0, 30.0, "Seconds targets keep burning.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.DIRECTIONAL)
         .force(0.5)
         .reaction(PhysicsProfile.Reaction.DASH)
         .blockDamage(0.0)
         .manual()
         .build();
   }

   @Nullable
   @Override
   public ResourceKey<DamageType> damageType() {
      return UoUoSeiryu.FLAME_DAMAGE;
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      int dash = (int)ctx.extra("dashTicks");
      double speed = (double)(ctx.range() / (float)dash);
      Vec3 dir = ctx.horizontalLook().add(0.0, Math.max(0.0, Math.min(0.35, AimHelper.look(player).y)), 0.0).normalize();
      Vec3 start = player.position();
      AbilityManager.setBusy(player, dash + 2);
      ServerFx.animate(player, ctx.animation(UoUoSeiryu.anim("flame_cloud")));
      double radius = 1.3 * (double)Math.max(1.0F, player.getScale());
      Set<Entity> struck = new HashSet<>();
      ServerFx.send(player, SeiryuFx.FLAME_CLOUD, start, dir, (float)radius, ctx.seed(), dash);
      ServerScheduler.schedule(
         level,
         0,
         age -> {
            if (player.isAlive() && !player.isRemoved() && player.level() == level) {
               if (age < dash) {
                  Targeting.moveSelf(player, dir.scale(speed).add(0.0, 0.04, 0.0));
                  player.fallDistance = 0.0F;
                  BlockImpactService.push(ctx, ImpactShape.sphere(player.getBoundingBox().getCenter(), radius + 0.5), dir, 0.15);

                  for (Entity e : Targeting.inRadius(
                     level, player.getBoundingBox().getCenter(), radius, x -> Targeting.canHurt(player, x) && !struck.contains(x)
                  )) {
                     struck.add(e);
                     if (ctx.hurt(e, ctx.damage())) {
                        e.igniteForSeconds((float)ctx.extra("burnSeconds"));
                        Targeting.launch(e, dir.scale((double)ctx.knockback()).add(0.0, 0.35, 0.0));
                     }
                  }

                  return false;
               } else {
                  Vec3 end = player.position();
                  ServerFx.send(
                     player, SeiryuFx.FLAME_TRAIL, start.add(0.0, 0.6, 0.0), end.add(0.0, 0.6, 0.0), (float)radius, ctx.seed(), (int)ctx.extra("lingerTicks")
                  );
                  this.linger(ctx, start, end, radius);
                  BlockImpactService.ignite(ctx, ImpactShape.capsule(start, end, radius), 0.12);
                  return true;
               }
            } else {
               return true;
            }
         }
      );
   }

   private void linger(AbilityContext ctx, Vec3 a, Vec3 b, double radius) {
      int ticks = (int)ctx.extra("lingerTicks");
      ServerLevel level = ctx.level();
      ServerScheduler.schedule(level, 10, age -> {
         for (Entity e : SeiryuCombat.inCapsule(level, ctx.player(), a, b.add(0.0, 0.6, 0.0), radius * 0.8)) {
            if (e.getRemainingFireTicks() <= 0) {
               e.igniteForSeconds(2.0F);
               ctx.hurt(e, ctx.damage() * 0.2F);
            }
         }

         return age >= ticks;
      });
   }
}
