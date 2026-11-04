package com.awakenedmod.awakened.content.command.vocab.target;

import com.awakenedmod.awakened.content.command.vocab.RuleContext;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import com.awakenedmod.awakened.core.AimUtil;
import com.awakenedmod.awakened.core.Foes;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class WhatILookAtTarget implements Target {
   private static final double RANGE = 32.0;

   @Override
   public String id() {
      return "what_i_look_at";
   }

   @Override
   public int unlockLevel() {
      return 4;
   }

   @Override
   public double defaultWeight() {
      return 1.5;
   }

   @Override
   public TargetKind kind() {
      return TargetKind.GAZE;
   }

   @Override
   public Collection<LivingEntity> select(ServerPlayer caster, RuleContext ctx) {
      double reach = caster.getEyePosition().distanceTo(AimUtil.aimedPosition(caster, 32.0));
      LivingEntity seen = AimUtil.aimedLivingEntity(
         caster, reach, body -> !body.isSpectator() && !Foes.fightsFor(caster, body) && !body.hasIndirectPassenger(caster)
      );
      return seen == null ? List.of() : List.of(seen);
   }
}
