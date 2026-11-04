package com.awakenedmod.awakened.core;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class GravityOverride {
   private static final double MIN_SOLVABLE = 0.004;

   private GravityOverride() {
   }

   public static void set(LivingEntity entity, ResourceLocation id, double target) {
      AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
      if (gravity != null) {
         double others = valueWithout(gravity, id);
         if (Math.abs(others) < 0.004) {
            clear(entity, id);
         } else {
            double amount = (double)Math.round((target / others - 1.0) * 10000.0) / 10000.0;
            Attrs.set(entity, Attributes.GRAVITY, id, amount, Operation.ADD_MULTIPLIED_TOTAL);
         }
      }
   }

   public static void clear(LivingEntity entity, ResourceLocation id) {
      Attrs.set(entity, Attributes.GRAVITY, id, 0.0, Operation.ADD_MULTIPLIED_TOTAL);
   }

   private static double valueWithout(AttributeInstance inst, ResourceLocation skip) {
      double add = inst.getBaseValue();

      for (AttributeModifier m : inst.getModifiers()) {
         if (!m.id().equals(skip) && m.operation() == Operation.ADD_VALUE) {
            add += m.amount();
         }
      }

      double value = add;

      for (AttributeModifier mx : inst.getModifiers()) {
         if (!mx.id().equals(skip) && mx.operation() == Operation.ADD_MULTIPLIED_BASE) {
            value += add * mx.amount();
         }
      }

      for (AttributeModifier mxx : inst.getModifiers()) {
         if (!mxx.id().equals(skip) && mxx.operation() == Operation.ADD_MULTIPLIED_TOTAL) {
            value *= 1.0 + mxx.amount();
         }
      }

      return value;
   }
}
