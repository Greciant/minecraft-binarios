package com.awakenedmod.awakened.content;

import com.awakenedmod.awakened.content.regenerator.BleedEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AwakenedEffects {
   private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "awakened");
   public static final DeferredHolder<MobEffect, BleedEffect> BLEED = EFFECTS.register("bleed", BleedEffect::new);
   public static final DeferredHolder<MobEffect, DisplayOnlyEffect> ENERGIZED = EFFECTS.register(
      "energized", () -> new DisplayOnlyEffect(MobEffectCategory.BENEFICIAL, 13638700)
   );

   private AwakenedEffects() {
   }

   public static void register(IEventBus modEventBus) {
      EFFECTS.register(modEventBus);
   }
}
