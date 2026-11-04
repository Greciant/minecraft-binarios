package com.devilfruits.fruits.kilo_kilo.ability;

import com.devilfruits.core.ability.Ability;
import com.devilfruits.core.ability.AbilityContext;
import com.devilfruits.core.ability.AbilityManager;
import com.devilfruits.core.ability.AbilityStats;
import com.devilfruits.core.ability.CastType;
import com.devilfruits.core.fx.ServerFx;
import com.devilfruits.core.physics.PhysicsProfile;
import com.devilfruits.fruits.kilo_kilo.KiloAbilities;
import com.devilfruits.fruits.kilo_kilo.KiloKilo;

public class OneKiloAbility extends Ability {
   public OneKiloAbility() {
      super(KiloKilo.id("one_kilo"), CastType.TOGGLE, AbilityStats.builder().energy(4).cooldown(10).mastery(1).damage(0.0).range(1.0).knockback(0.0).build());
   }

   @Override
   public PhysicsProfile physics() {
      return PhysicsProfile.builder(this.defaults())
         .impact(PhysicsProfile.Impact.NONE)
         .reaction(PhysicsProfile.Reaction.JUMP)
         .blockDamage(0.0)
         .manual()
         .build();
   }

   @Override
   public void onToggle(AbilityContext ctx, boolean active) {
      if (active) {
         AbilityManager.stopToggle(ctx.player(), KiloAbilities.PRESS);
         ServerFx.animate(ctx.player(), ctx.animation(KiloKilo.id("light_cast")));
      }
   }
}
