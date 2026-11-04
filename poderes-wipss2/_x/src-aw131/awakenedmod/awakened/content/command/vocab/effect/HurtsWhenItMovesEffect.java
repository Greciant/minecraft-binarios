package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.RuleEngine;
import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.content.command.vocab.TargetKind;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class HurtsWhenItMovesEffect implements Effect {
   private static final float HEALTH_PER_BLOCK_PER_LEVEL = 0.5F;
   private static final double MAX_BLOCKS_PER_TICK = 1.0;
   private static final double MIN_BLOCKS_PER_TICK = 0.01;
   private static final float DEAL_AT = 1.0F;
   private static final HoldMemory<HurtsWhenItMovesEffect.Trail> TRAILS = new HoldMemory<>();

   @Override
   public String id() {
      return "hurts_when_it_moves";
   }

   @Override
   public int unlockLevel() {
      return 4;
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
   public String numberUnit() {
      return "awakened.command.unit.bare";
   }

   @Override
   public boolean supports(TargetKind kind) {
      return kind != TargetKind.SELF;
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      if (target != caster && target.level() instanceof ServerLevel world) {
         TRAILS.take(target, age, () -> new HurtsWhenItMovesEffect.Trail(target));
         HurtsWhenItMovesEffect.Trail var13 = TRAILS.get(target);
         long now = world.getGameTime();
         if (var13 != null && var13.countedAt != now) {
            var13.countedAt = now;
            Vec3 here = target.position();
            double moved = var13.dimension == world.dimension() ? Math.min(1.0, here.distanceTo(var13.last)) : 0.0;
            var13.last = here;
            var13.dimension = world.dimension();
            if (moved < 0.01) {
               return false;
            } else {
               var13.owed = var13.owed + (float)moved * 0.5F * (float)Math.max(1, level);
               if (age % 4 == 0) {
                  Fx.burst(world, ParticleTypes.CRIT, here.add(0.0, 0.1, 0.0), 2, (double)target.getBbWidth() * 0.3, 0.05);
               }

               if (var13.owed < 1.0F) {
                  return false;
               } else {
                  float owed = var13.owed;
                  var13.owed = 0.0F;
                  return Dmg.hurtRapid(target, world, Dmg.by(caster, DamageTypes.GENERIC), RuleEngine.ruleDamage(owed));
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public void release(LivingEntity target) {
      TRAILS.release(target);
   }

   @Override
   public void forget(UUID bodyId) {
      TRAILS.forget(bodyId);
   }

   @Override
   public void forgetAllBodies() {
      TRAILS.clear();
   }

   private static final class Trail {
      Vec3 last;
      ResourceKey<Level> dimension;
      float owed;
      long countedAt = Long.MIN_VALUE;

      Trail(LivingEntity body) {
         this.last = body.position();
         this.dimension = body.level().dimension();
      }
   }
}
