package com.awakenedmod.awakened.content.command.vocab.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

public final class DigsFasterEffect extends PotionEffectWord {
   public DigsFasterEffect() {
      super("digs_faster", 0, 10.0, true);
   }

   @Override
   public boolean hostile() {
      return false;
   }

   @Override
   protected Holder<MobEffect> mobEffect(int level) {
      return MobEffects.DIG_SPEED;
   }

   @Override
   public boolean playersOnly() {
      return true;
   }
}
