package com.devilfruits.core.physics;

import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.CastType;
import net.minecraft.world.phys.Vec3;

public final class AutoStrike {
   public static void afterCast(AbilityContext ctx) {
      if (PhysicsWorld.active()) {
         PhysicsProfile p = ctx.ability().physics();
         if (p.auto() && (p.impact() != PhysicsProfile.Impact.NONE || !(p.blockDamage() <= 0.0))) {
            double range = Math.max(2.0, (double)ctx.range());
            double power = ctx.ability().castType() == CastType.CHARGED ? Math.max(0.3, (double)ctx.charge()) : 1.0;
            Vec3 look = ctx.look();

            ImpactShape shape = switch (p.impact()) {
               case RADIAL, TANGENTIAL -> ImpactShape.sphere(ctx.player().position().add(0.0, 1.0, 0.0), range);
               default -> ImpactShape.cone(ctx.eye(), look, range, Math.toRadians(22.0));
            };
            BlockImpactService.strike(ctx, shape, look, power);
         }
      }
   }

   private AutoStrike() {
   }
}
