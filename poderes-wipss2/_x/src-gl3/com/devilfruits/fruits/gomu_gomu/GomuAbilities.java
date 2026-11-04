package com.devilfruits.fruits.gomu_gomu;

import com.devilfruits.fruits.gomu_gomu.ability.AxeAbility;
import com.devilfruits.fruits.gomu_gomu.ability.BazookaAbility;
import com.devilfruits.fruits.gomu_gomu.ability.GatlingAbility;
import com.devilfruits.fruits.gomu_gomu.ability.PistolAbility;
import com.devilfruits.fruits.gomu_gomu.ability.RifleAbility;
import com.devilfruits.fruits.gomu_gomu.ability.RocketAbility;

public final class GomuAbilities {
   public static final PistolAbility PISTOL = new PistolAbility();
   public static final BazookaAbility BAZOOKA = new BazookaAbility();
   public static final RocketAbility ROCKET = new RocketAbility();
   public static final AxeAbility AXE = new AxeAbility();
   public static final RifleAbility RIFLE = new RifleAbility();
   public static final GatlingAbility GATLING = new GatlingAbility();

   private GomuAbilities() {
   }
}
