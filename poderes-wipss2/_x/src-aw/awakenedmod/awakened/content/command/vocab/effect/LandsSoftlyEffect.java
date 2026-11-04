package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.core.Attrs;
import com.awakenedmod.awakened.core.AwId;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class LandsSoftlyEffect implements Effect {
   private static final ResourceLocation NO_FALL_ID = AwId.of("command_lands_softly");
   private static final double WORKED_FALL = 0.6;

   @Override
   public String id() {
      return "lands_softly";
   }

   @Override
   public boolean hostile() {
      return false;
   }

   @Override
   public int unlockLevel() {
      return 1;
   }

   @Override
   public double defaultWeight() {
      return 5.0;
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      Attrs.set(target, Attributes.FALL_DAMAGE_MULTIPLIER, NO_FALL_ID, -1.0, Operation.ADD_MULTIPLIED_TOTAL);
      double fallen = (double)target.fallDistance;
      target.resetFallDistance();
      return fallen > 0.6;
   }

   @Override
   public void release(LivingEntity target) {
      Attrs.set(target, Attributes.FALL_DAMAGE_MULTIPLIER, NO_FALL_ID, 0.0, Operation.ADD_MULTIPLIED_TOTAL);
   }
}
