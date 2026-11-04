package com.awakenedmod.awakened.content.command.vocab.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

public final class IsWeakerEffect extends PotionEffectWord {
   public IsWeakerEffect() {
      super("is_weaker", 3, 10.0, true);
   }

   @Override
   protected Holder<MobEffect> mobEffect(int level) {
      return MobEffects.WEAKNESS;
   }

   @Override
   public boolean actsThroughBlows() {
      return true;
   }
}
