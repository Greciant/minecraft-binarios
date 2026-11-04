package com.devilfruits.fruits.suna_suna;

import com.devilfruits.fruits.suna_suna.effect.DehydratedEffect;
import com.devilfruits.registry.DFMobEffects;
import javax.annotation.Nullable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class SunaEffects {
   public static final DeferredHolder<MobEffect, MobEffect> DEHYDRATED = DFMobEffects.EFFECTS.register("suna_suna/dehydrated", DehydratedEffect::new);
   public static final int MAX_LEVEL = 4;

   public static int dehydrate(LivingEntity target, int seconds, int levels, int maxLevel, @Nullable Entity source) {
      MobEffectInstance current = target.getEffect(DEHYDRATED);
      int amplifier = current == null ? levels - 1 : Math.min(maxLevel, current.getAmplifier() + levels);
      amplifier = Math.max(0, Math.min(maxLevel, amplifier));
      int duration = Math.max(seconds * 20, current == null ? 0 : current.getDuration());
      return target.addEffect(new MobEffectInstance(DEHYDRATED, duration, amplifier), source) ? amplifier : -1;
   }

   static void init() {
   }

   private SunaEffects() {
   }
}
