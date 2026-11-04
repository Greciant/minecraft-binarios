package com.awakenedmod.awakened.content.command.vocab.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

public final class BreathesWaterEffect extends PotionEffectWord {
   public BreathesWaterEffect() {
      super("breathes_water", 1, 5.0, false);
   }

   @Override
   public boolean hostile() {
      return false;
   }

   @Override
   protected Holder<MobEffect> mobEffect(int level) {
      return MobEffects.WATER_BREATHING;
   }
}
