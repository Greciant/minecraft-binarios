package com.awakenedmod.awakened.content.command.vocab.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

public final class SeesInTheDarkEffect extends PotionEffectWord {
   private static final int STEADY_TICKS = 220;

   public SeesInTheDarkEffect() {
      super("sees_in_the_dark", 0, 5.0, false);
   }

   @Override
   public boolean hostile() {
      return false;
   }

   @Override
   protected Holder<MobEffect> mobEffect(int level) {
      return MobEffects.NIGHT_VISION;
   }

   @Override
   protected int instanceTicks() {
      return 220;
   }

   @Override
   public boolean playersOnly() {
      return true;
   }
}
