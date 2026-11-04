package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.core.AwId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;

public final class WeightlessDrift {
   public static final ResourceLocation GRAVITY_ID = AwId.of("command_weighs_nothing");
   private static final float VANILLA_FRICTION = 0.91F;
   private static final float FRICTION = 0.98F;
   private static final float INPUT_SCALE = 0.20634909F;

   private WeightlessDrift() {
   }

   public static boolean drifting(LivingEntity body) {
      if (body.onGround() || body.isInLiquid() || body.isFallFlying() || body.isPassenger() || body instanceof FlyingAnimal) {
         return false;
      } else {
         if (body instanceof Player player && player.getAbilities().flying) {
            return false;
         }

         AttributeInstance gravity = body.getAttribute(Attributes.GRAVITY);
         return gravity != null && gravity.getModifier(GRAVITY_ID) != null;
      }
   }

   public static float airFriction(LivingEntity body, float vanilla) {
      return drifting(body) ? 0.98F : vanilla;
   }

   public static float airInput(LivingEntity body, float vanilla) {
      return drifting(body) ? vanilla * 0.20634909F : vanilla;
   }
}
