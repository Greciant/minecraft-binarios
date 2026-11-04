package com.devilfruits.fruits.horo_horo;

import com.devilfruits.fruits.horo_horo.ability.GhostFormAbility;
import com.devilfruits.fruits.horo_horo.ability.MiniHollowAbility;
import com.devilfruits.fruits.horo_horo.ability.NegativeHollowAbility;
import com.devilfruits.fruits.horo_horo.ability.SpecialHollowAbility;

public final class HoroAbilities {
   public static final NegativeHollowAbility NEGATIVE_HOLLOW = new NegativeHollowAbility();
   public static final MiniHollowAbility MINI_HOLLOW = new MiniHollowAbility();
   public static final GhostFormAbility GHOST_FORM = new GhostFormAbility();
   public static final SpecialHollowAbility SPECIAL_HOLLOW = new SpecialHollowAbility();

   private HoroAbilities() {
   }
}
