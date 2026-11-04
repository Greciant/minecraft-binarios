package com.awakenedmod.awakened.content.command.vocab.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

public final class GlowsEffect extends PotionEffectWord {
   public GlowsEffect() {
      super("glows", 2, 5.0, false);
   }

   @Override
   public boolean hostile() {
      return false;
   }

   @Override
   protected Holder<MobEffect> mobEffect(int level) {
      return MobEffects.GLOWING;
   }
}
