package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Clash;
import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.Posture;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

public final class JumpsHigherEffect extends PotionEffectWord {
   public JumpsHigherEffect() {
      super("jumps_higher", 0, 10.0, true);
   }

   @Override
   public boolean hostile() {
      return false;
   }

   @Override
   protected Holder<MobEffect> mobEffect(int level) {
      return MobEffects.JUMP;
   }

   @Override
   public Clash clash(Target target, Effect effect, Trigger trigger) {
      return target.movesWithCaster() && trigger.postures().contains(Posture.AIRBORNE) ? Clash.JUMPS_FROM_GROUND : null;
   }
}
