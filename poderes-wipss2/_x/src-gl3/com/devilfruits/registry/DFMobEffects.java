package com.devilfruits.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class DFMobEffects {
   public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, "devilfruits");
   public static final DeferredHolder<MobEffect, MobEffect> ARMAMENT_COATING = EFFECTS.register(
      "armament_coating", () -> new DFMobEffects.CoreEffect(MobEffectCategory.BENEFICIAL, 2762291)
   );

   private DFMobEffects() {
   }

   public static class CoreEffect extends MobEffect {
      public CoreEffect(MobEffectCategory category, int color) {
         super(category, color);
      }
   }
}
