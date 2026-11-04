package com.devilfruits.fruits.suke_suke;

import com.devilfruits.fruits.suke_suke.ability.CloakAbility;
import com.devilfruits.fruits.suke_suke.ability.HiddenWeaponAbility;
import com.devilfruits.fruits.suke_suke.ability.ShipInvisibilityAbility;

public final class SukeAbilities {
   public static final CloakAbility INVISIBILITY = new CloakAbility("invisibility", false, 1, 40);
   public static final HiddenWeaponAbility HIDDEN_WEAPON = new HiddenWeaponAbility();
   public static final CloakAbility GROUP_INVISIBILITY = new CloakAbility("group_invisibility", true, 4, 100);
   public static final ShipInvisibilityAbility SHIP_INVISIBILITY = new ShipInvisibilityAbility();

   private SukeAbilities() {
   }
}
