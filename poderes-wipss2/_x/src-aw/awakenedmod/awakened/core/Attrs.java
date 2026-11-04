package com.awakenedmod.awakened.core;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class Attrs {
   private Attrs() {
   }

   public static void set(LivingEntity entity, Holder<Attribute> attr, ResourceLocation id, double amount, Operation op) {
      if (entity != null) {
         AttributeInstance inst = entity.getAttribute(attr);
         if (inst != null) {
            AttributeModifier current = inst.getModifier(id);
            boolean want = Math.abs(amount) > 1.0E-4;
            if (current != null || want) {
               if (current == null || !want || !(Math.abs(current.amount() - amount) < 1.0E-6) || current.operation() != op) {
                  inst.removeModifier(id);
                  if (want) {
                     inst.addTransientModifier(new AttributeModifier(id, amount, op));
                  }
               }
            }
         }
      }
   }
}
