package com.awakenedmod.awakened.content.command.vocab.effect;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

public final class IsStrongerEffect extends PotionEffectWord {
   public IsStrongerEffect() {
      super("is_stronger", 5, 20.0, true);
   }

   @Override
   public boolean hostile() {
      return false;
   }

   @Override
   protected Holder<MobEffect> mobEffect(int level) {
      return MobEffects.DAMAGE_BOOST;
   }

   @Override
   public boolean actsThroughBlows() {
      return true;
   }
}
