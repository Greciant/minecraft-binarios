package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class CannotHurtMeEffect implements Effect {
   @Override
   public String id() {
      return "cannot_hurt_me";
   }

   @Override
   public int unlockLevel() {
      return 9;
   }

   @Override
   public double defaultWeight() {
      return 60.0;
   }

   @Override
   public boolean supports(TargetKind kind) {
      return kind != TargetKind.SELF;
   }

   @Override
   public boolean nullifiesDamageToCaster() {
      return true;
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      return false;
   }

   @Override
   public boolean actsThroughBlows() {
      return true;
   }
}
