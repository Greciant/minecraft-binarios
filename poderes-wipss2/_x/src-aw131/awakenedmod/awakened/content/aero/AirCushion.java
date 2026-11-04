package com.awakenedmod.awakened.content.aero;

import com.awakenedmod.awakened.core.AwId;
import com.awakenedmod.awakened.core.data.PowerData;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class AirCushion {
   private static final ResourceLocation FALL_MULT_ID = AwId.of("aero_air_cushion");
   private static final float BASE_MULT = 0.6F;
   private static final float FLOOR_MULT = 0.25F;
   private static final int FLOOR_LEVEL = 10;

   private AirCushion() {
   }

   public static void apply(ServerPlayer player, PowerData data) {
      int level = data.masteryLevel();
      float t = Math.min(1.0F, Math.max(0.0F, (float)level / 10.0F));
      float target = 0.6F - 0.35000002F * t;
      setModifier(player, Attributes.FALL_DAMAGE_MULTIPLIER, (double)target - 1.0);
   }

   public static void clear(ServerPlayer player) {
      AttributeInstance inst = player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);
      if (inst != null) {
         inst.removeModifier(FALL_MULT_ID);
      }
   }

   private static void setModifier(ServerPlayer player, Holder<Attribute> attr, double amount) {
      AttributeInstance inst = player.getAttribute(attr);
      if (inst != null) {
         AttributeModifier current = inst.getModifier(FALL_MULT_ID);
         if (current == null || !(Math.abs(current.amount() - amount) < 1.0E-6)) {
            inst.removeModifier(FALL_MULT_ID);
            inst.addTransientModifier(new AttributeModifier(FALL_MULT_ID, amount, Operation.ADD_MULTIPLIED_TOTAL));
         }
      }
   }
}
