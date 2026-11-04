package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Clash;
import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.Posture;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

public final class MovesAtEffect extends PotionEffectWord {
   public MovesAtEffect() {
      super("moves_at", 0, 10.0, true);
   }

   @Override
   public boolean hostile() {
      return false;
   }

   @Override
   protected boolean hostileAt(int level) {
      return level <= 0;
   }

   @Override
   protected Holder<MobEffect> mobEffect(int level) {
      return level >= 1 ? MobEffects.MOVEMENT_SPEED : MobEffects.MOVEMENT_SLOWDOWN;
   }

   @Override
   protected int amplifier(int level) {
      return level >= 1 ? level - 1 : -level;
   }

   @Override
   public Clash clash(Target target, Effect effect, Trigger trigger) {
      return target.movesWithCaster() && trigger.postures().contains(Posture.STILL) ? Clash.NEEDS_MOVING : null;
   }
}
