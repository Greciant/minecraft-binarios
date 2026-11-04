package com.awakenedmod.awakened.core;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public record QuietEffect(Holder<MobEffect> effect, int duration, int refreshTicks) {
   public void keep(LivingEntity body) {
      MobEffectInstance present = body.getEffect(this.effect);
      if (present == null || this.isOurs(present) && body.tickCount % this.refreshTicks == 0) {
         body.addEffect(new MobEffectInstance(this.effect, this.duration, 0, true, false, false));
      }
   }

   public void clear(LivingEntity body) {
      MobEffectInstance present = body.getEffect(this.effect);
      if (present != null && this.isOurs(present)) {
         body.removeEffect(this.effect);
      }
   }

   private boolean isOurs(MobEffectInstance instance) {
      return instance.isAmbient() && !instance.isVisible() && instance.getDuration() <= this.duration;
   }
}
