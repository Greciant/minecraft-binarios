package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class FeelsMyPainEffect implements Effect {
   @Override
   public String id() {
      return "feels_my_pain";
   }

   @Override
   public int unlockLevel() {
      return 8;
   }

   @Override
   public double defaultWeight() {
      return 30.0;
   }

   @Override
   public boolean supports(TargetKind kind) {
      return kind != TargetKind.SELF;
   }

   @Override
   public boolean sharesCasterDamage() {
      return true;
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      return false;
   }
}
