package com.awakenedmod.awakened.content.command.vocab.target;

import com.awakenedmod.awakened.content.command.vocab.RuleContext;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class MeTarget implements Target {
   @Override
   public String id() {
      return "me";
   }

   @Override
   public int unlockLevel() {
      return 0;
   }

   @Override
   public double defaultWeight() {
      return 1.0;
   }

   @Override
   public TargetKind kind() {
      return TargetKind.SELF;
   }

   @Override
   public Collection<LivingEntity> select(ServerPlayer caster, RuleContext ctx) {
      return List.of(caster);
   }
}
