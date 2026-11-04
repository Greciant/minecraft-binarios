package com.devilfruits.fruits.bomu_bomu;

import com.devilfruits.fruits.bomu_bomu.ability.ExplosivePunchAbility;
import com.devilfruits.fruits.bomu_bomu.ability.ExplosiveShotAbility;
import com.devilfruits.fruits.bomu_bomu.ability.NoseFancyCannonAbility;
import com.devilfruits.fruits.bomu_bomu.ability.SelfDestructAbility;

public final class BomuAbilities {
   public static final ExplosivePunchAbility PUNCH = new ExplosivePunchAbility();
   public static final NoseFancyCannonAbility CANNON = new NoseFancyCannonAbility();
   public static final ExplosiveShotAbility SHOT = new ExplosiveShotAbility();
   public static final SelfDestructAbility SELF_DESTRUCT = new SelfDestructAbility();

   private BomuAbilities() {
   }
}
