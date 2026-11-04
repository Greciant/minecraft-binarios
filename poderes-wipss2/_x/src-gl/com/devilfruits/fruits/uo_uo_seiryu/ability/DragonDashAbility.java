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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class DragonDashAbility extends Ability {
   public DragonDashAbility() {
      super(
         UoUoSeiryu.id("dragon_dash"),
         CastType.INSTANT,
         AbilityStats.builder()
            .energy(15)
            .cooldown(60)
            .mastery(1)
            .damage(5.0)
            .range(10.0)
            .knockback(1.6)
            .extra("dashTicks", 4.0, 2.0, 20.0, "Duration of the lunge.")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.DIRECTIONAL)
         .force(0.8)
         .reaction(PhysicsProfile.Reaction.DASH)
         .blockDamage(0.0)
         .manual()
         .build();
   }

   @Override
   public void activate(AbilityContext ctx) {
      ServerPlayer player = ctx.player();
      ServerLevel level = ctx.level();
      int dash = (int)ctx.extra("dashTicks");
      Vec3 dir = AimHelper.look(player);
      double speed = (double)(ctx.range() / (float)dash);
      Vec3 start = player.position();
      AbilityManager.setBusy(player, dash + 1);
      ServerFx.animate(player, ctx.animation(UoUoSeiryu.anim("dragon_dash")));
      ServerFx.send(player, SeiryuFx.DASH_STREAK, start, start.add(dir.scale((double)ctx.range())), 1.0F, ctx.seed(), dash + 8);
      double radius = 1.2 * (double)Math.max(1.0F, player.getScale());
      Set<Entity> struck = new HashSet<>();
      ServerScheduler.schedule(level, 0, age -> {
         if (player.isAlive() && !player.isRemoved() && player.level() == level && age < dash) {
            Targeting.moveSelf(player, dir.scale(speed));
            player.fallDistance = 0.0F;
            BlockImpactService.push(ctx, ImpactShape.sphere(player.getBoundingBox().getCenter(), radius + 0.6), dir, 0.3);

            for (Entity e : Targeting.inRadius(level, player.getBoundingBox().getCenter(), radius, x -> Targeting.canHurt(player, x) && !struck.contains(x))) {
               struck.add(e);
               ctx.hurt(e, ctx.damage());
               Targeting.launch(e, dir.scale((double)ctx.knockback()).add(0.0, 0.4, 0.0));
            }

            return false;
         } else {
            if (player.isAlive()) {
               Targeting.moveSelf(player, player.getDeltaMovement().scale(0.3));
            }

            return true;
         }
      });
   }
}
