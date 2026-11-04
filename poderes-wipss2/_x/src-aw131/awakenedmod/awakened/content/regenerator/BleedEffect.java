package com.awakenedmod.awakened.content.regenerator;

import com.awakenedmod.awakened.content.DisplayOnlyEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class BleedEffect extends DisplayOnlyEffect {
   private static final int BLOOD_COLOR = 11541294;

   public BleedEffect() {
      super(MobEffectCategory.HARMFUL, 11541294);
   }
}
