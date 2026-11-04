package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.core.Dmg;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class BurnsEffect implements Effect {
   private static final float BURN_SECONDS = 2.0F;

   @Override
   public String id() {
      return "burns";
   }

   @Override
   public int unlockLevel() {
      return 2;
   }

   @Override
   public double defaultWeight() {
      return 10.0;
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      if (target.fireImmune()) {
         return false;
      } else {
         target.igniteForSeconds(2.0F);
         if (target != caster) {
            Dmg.creditTo(target, caster);
         }

         return true;
      }
   }

   @Override
   public void release(LivingEntity target) {
      target.clearFire();
   }
}
