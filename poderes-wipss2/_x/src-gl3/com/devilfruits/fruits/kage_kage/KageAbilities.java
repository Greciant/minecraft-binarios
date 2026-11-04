package com.devilfruits.fruits.kage_kage;

import com.devilfruits.fruits.kage_kage.ability.BrickBatAbility;
import com.devilfruits.fruits.kage_kage.ability.DoppelmanAbility;
import com.devilfruits.fruits.kage_kage.ability.ShadowCollectionAbility;
import com.devilfruits.fruits.kage_kage.ability.ShadowHandAbility;
import com.devilfruits.fruits.kage_kage.ability.ShadowSpearAbility;
import com.devilfruits.fruits.kage_kage.ability.ShadowTrapAbility;

public final class KageAbilities {
   public static final BrickBatAbility BRICK_BAT = new BrickBatAbility();
   public static final ShadowSpearAbility SPEAR = new ShadowSpearAbility();
   public static final ShadowCollectionAbility COLLECTION = new ShadowCollectionAbility();
   public static final ShadowTrapAbility TRAP = new ShadowTrapAbility();
   public static final ShadowHandAbility HAND = new ShadowHandAbility();
   public static final DoppelmanAbility DOPPELMAN = new DoppelmanAbility();

   private KageAbilities() {
   }
}
