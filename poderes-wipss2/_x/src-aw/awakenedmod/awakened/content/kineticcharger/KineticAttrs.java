package com.awakenedmod.awakened.content.kineticcharger;

import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

final class KineticAttrs {
   private static final double SAFE_FALL_IMMUNE = 1024.0;
   private static final double FALL_MULTIPLIER_IMMUNE = -1.0;
   static final int FALL_IMMUNE_MASTERY = 5;
   private static final ResourceLocation PASSIVE_SAFE_ID = AwId.of("kinetic_capacitor_safe_fall");
   private static final ResourceLocation PASSIVE_MULT_ID = AwId.of("kinetic_capacitor_fall");

   private KineticAttrs() {
   }

   static void passiveFallImmunity(ServerPlayer player, int masteryLevel) {
      fallImmune(player, PASSIVE_SAFE_ID, PASSIVE_MULT_ID, masteryLevel >= 5);
   }

   static void fallImmune(ServerPlayer player, ResourceLocation safeId, ResourceLocation multiplierId, boolean immune) {
      Attrs.set(player, Attributes.SAFE_FALL_DISTANCE, safeId, immune ? 1024.0 : 0.0, Operation.ADD_VALUE);
      Attrs.set(player, Attributes.FALL_DAMAGE_MULTIPLIER, multiplierId, immune ? -1.0 : 0.0, Operation.ADD_MULTIPLIED_TOTAL);
   }
}
