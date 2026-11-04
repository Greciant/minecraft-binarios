package com.awakenedmod.awakened.content.command.vocab.target;

import com.awakenedmod.awakened.content.command.vocab.Clash;
import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.Posture;
import com.awakenedmod.awakened.content.command.vocab.RuleContext;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import com.awakenedmod.awakened.content.command.vocab.Trigger;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class WhatIRideTarget implements Target {
   @Override
   public String id() {
      return "what_i_ride";
   }

   @Override
   public int unlockLevel() {
      return 1;
   }

   @Override
   public double defaultWeight() {
      return 1.2;
   }

   @Override
   public TargetKind kind() {
      return TargetKind.NEAR;
   }

   @Override
   public boolean usesRadius() {
      return false;
   }

   @Override
   public Collection<LivingEntity> select(ServerPlayer caster, RuleContext ctx) {
      if (caster.getVehicle() instanceof LivingEntity mount && mount.isAlive()) {
         return List.of(mount);
      }

      return List.of();
   }

   @Override
   public boolean namesPlayers() {
      return false;
   }

   @Override
   public boolean carriesCaster() {
      return true;
   }

   @Override
   public Clash clash(Target target, Effect effect, Trigger trigger) {
      if (trigger.postures().contains(Posture.SNEAKING)) {
         return Clash.SNEAK_DISMOUNTS;
      } else {
         return effect.actsThroughBlows() ? Clash.NEVER_FIGHTS : null;
      }
   }
}
