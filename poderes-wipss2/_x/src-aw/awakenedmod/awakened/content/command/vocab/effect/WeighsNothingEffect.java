package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Clash;
import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.Posture;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import com.awakenedmod.awakened.core.Attrs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;

public final class WeighsNothingEffect implements Effect {
   private static final ResourceLocation GRAVITY_ID = WeightlessDrift.GRAVITY_ID;
   private static final double HOVER = -1.0;
   private static final double SINK = -0.75;

   @Override
   public String id() {
      return "weighs_nothing";
   }

   @Override
   public boolean hostile() {
      return false;
   }

   @Override
   public int unlockLevel() {
      return 4;
   }

   @Override
   public double defaultWeight() {
      return 20.0;
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      boolean sinking = target.isShiftKeyDown();
      if (!inOpenAir(target)) {
         this.release(target);
         return false;
      } else {
         Attrs.set(target, Attributes.GRAVITY, GRAVITY_ID, sinking ? -0.75 : -1.0, Operation.ADD_MULTIPLIED_TOTAL);
         target.resetFallDistance();
         return true;
      }
   }

   private static boolean inOpenAir(LivingEntity body) {
      return !body.onGround() && !body.isInWater() && !body.isInLava() && !body.onClimbable() && !body.isFallFlying() && !body.isPassenger();
   }

   @Override
   public void release(LivingEntity target) {
      Attrs.set(target, Attributes.GRAVITY, GRAVITY_ID, 0.0, Operation.ADD_MULTIPLIED_TOTAL);
   }

   @Override
   public Clash clash(Target target, Effect effect, Trigger trigger) {
      if (!target.movesWithCaster()) {
         return null;
      } else if (trigger.postures().contains(Posture.SNEAKING)) {
         return Clash.SNEAK_SINKS;
      } else {
         return trigger.postures().contains(Posture.FALLING) ? Clash.STOPS_THE_FALL : null;
      }
   }
}
