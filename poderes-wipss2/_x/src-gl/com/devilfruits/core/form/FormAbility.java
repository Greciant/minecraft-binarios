package com.devilfruits.core.form;

import com.devilfruits.core.ability.Ability;

public record FormAbility(Ability ability, AbilityModifiers modifiers) {
   public FormAbility(Ability ability) {
      this(ability, AbilityModifiers.NONE);
   }
}
