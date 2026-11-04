package com.awakenedmod.awakened.content.command.vocab.target;

import com.awakenedmod.awakened.content.command.vocab.RuleContext;
import com.awakenedmod.awakened.content.command.vocab.Target;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import java.util.Collection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

public final class WhatTargetsMeTarget implements Target {
   private static final double RANGE = 32.0;

   @Override
   public String id() {
      return "what_targets_me";
   }

   @Override
   public int unlockLevel() {
      return 6;
   }

   @Override
   public double defaultWeight() {
      return 1.5;
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
      return caster.level().getEntitiesOfClass(LivingEntity.class, new AABB(caster.position(), caster.position()).inflate(32.0), body -> {
         if (body.isAlive() && body instanceof Mob mob && mob.getTarget() == caster && body.distanceToSqr(caster) <= 1024.0) {
            return true;
         }

         return false;
      });
   }

   @Override
   public boolean namesPlayers() {
      return false;
   }
}
