package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class IsFragileEffect implements Effect {
   private static final float EXTRA_PER_LEVEL = 0.2F;

   @Override
   public String id() {
      return "is_fragile";
   }

   @Override
   public int unlockLevel() {
      return 5;
   }

   @Override
   public double defaultWeight() {
      return 15.0;
   }

   @Override
   public boolean hasLevel() {
      return true;
   }

   @Override
   public boolean supports(TargetKind kind) {
      return kind != TargetKind.SELF;
   }

   @Override
   public Component numberText(int number) {
      return Component.translatable("awakened.command.unit.more_damage", new Object[]{Math.round(20.0F * (float)Math.max(1, number))});
   }

   @Override
   public float incomingDamageMultiplier(int level) {
      return 1.0F + 0.2F * (float)Math.max(1, level);
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      return false;
   }
}
