package com.awakenedmod.awakened.content.command.vocab.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

public final class IsUnseenEffect extends PotionEffectWord {
   public IsUnseenEffect() {
      super("is_unseen", 7, 15.0, false);
   }

   @Override
   public boolean hostile() {
      return false;
   }

   @Override
   protected Holder<MobEffect> mobEffect(int level) {
      return MobEffects.INVISIBILITY;
   }
}
