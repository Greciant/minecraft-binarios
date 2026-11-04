package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class RegeneratesEffect implements Effect {
   private static final float HEALTH_PER_SECOND_PER_LEVEL = 0.5F;

   @Override
   public String id() {
      return "regenerates";
   }

   @Override
   public boolean hostile() {
      return false;
   }

   @Override
   public int unlockLevel() {
      return 6;
   }

   @Override
   public double defaultWeight() {
      return 10.0;
   }

   @Override
   public boolean hasLevel() {
      return true;
   }

   @Override
   public Component numberText(int number) {
      float perSecond = 0.5F * (float)Math.max(1, number);
      String shown = (double)perSecond == Math.rint((double)perSecond) ? String.valueOf((int)perSecond) : String.valueOf(perSecond);
      return Component.translatable("awakened.command.unit.health_per_second", new Object[]{shown});
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      if (target.getHealth() >= target.getMaxHealth()) {
         return false;
      } else {
         target.heal(0.5F * (float)Math.max(1, level) / 20.0F);
         return true;
      }
   }
}
