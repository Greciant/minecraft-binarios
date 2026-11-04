package com.devilfruits.fruits.uo_uo_seiryu.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.form.FormFlags;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.uo_uo_seiryu.DragonShells;
import com.devilfruits.fruits.uo_uo_seiryu.UoUoSeiryu;

public class SerpentineFlightAbility extends Ability {
   public SerpentineFlightAbility() {
      super(
         UoUoSeiryu.id("serpentine_flight"),
         CastType.TOGGLE,
         AbilityStats.builder()
            .energy(5)
            .cooldown(20)
            .mastery(6)
            .damage(0.0)
            .range(1.0)
            .knockback(0.0)
            .extra("drainPerSecond", 0.0, 0.0, 100.0, "Extra drain while flying (the form already drains).")
            .build()
      );
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.NONE)
         .reaction(PhysicsProfile.Reaction.NONE)
         .blockDamage(0.0)
         .manual()
         .build();
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      FormFlags.set(ctx.player(), DragonShells.SLITHER, active);
      FormFlags.set(ctx.player(), 2, !active);
   }
}
