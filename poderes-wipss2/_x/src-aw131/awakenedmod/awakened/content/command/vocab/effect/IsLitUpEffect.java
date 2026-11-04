package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.core.CarriedLights;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class IsLitUpEffect implements Effect {
   @Override
   public String id() {
      return "is_lit_up";
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
      return CarriedLights.carry(target);
   }

   @Override
   public void release(LivingEntity target) {
      CarriedLights.drop(target);
   }

   @Override
   public void forget(UUID bodyId) {
      CarriedLights.drop(bodyId);
   }
}
