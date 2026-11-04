package com.awakenedmod.awakened.content.command.vocab.effect;

import com.awakenedmod.awakened.content.command.RuleEngine;
import com.awakenedmod.awakened.content.command.vocab.Effect;
import com.awakenedmod.awakened.core.Dmg;
import com.awakenedmod.awakened.core.Fx;
import com.awakenedmod.awakened.core.Harm;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ExplodesEffect implements Effect {
   private static final double RADIUS = 3.0;
   private static final double KNOCKBACK = 0.9;
   private static final double CENTRE_LIFT = 0.35;
   private static final HoldMemory<Boolean> POPPED = new HoldMemory<>();
   private static final int REPEAT_TICKS = 40;

   @Override
   public String id() {
      return "explodes";
   }

   @Override
   public int unlockLevel() {
      return 6;
   }

   @Override
   public double defaultWeight() {
      return 6.0;
   }

   @Override
   public boolean instant() {
      return true;
   }

   @Override
   public int repeatTicks() {
      return 40;
   }

   @Override
   public boolean hasLevel() {
      return true;
   }

   @Override
   public String numberUnit() {
      return "awakened.command.unit.health";
   }

   @Override
   public boolean apply(ServerPlayer caster, LivingEntity target, int level, int age) {
      boolean fires = age == 0 ? POPPED.take(target, age, () -> Boolean.TRUE) : age % 40 == 0;
      if (!fires) {
         return false;
      } else if (!(target.level() instanceof ServerLevel world)) {
         return false;
      } else {
         Vec3 var18 = target.position().add(0.0, (double)target.getBbHeight() * 0.5, 0.0);
         float damage = RuleEngine.ruleDamage((float)Math.max(1, level));
         DamageSource blast = Dmg.by(caster, DamageTypes.PLAYER_EXPLOSION);
         AABB reach = new AABB(var18, var18).inflate(3.0);
         boolean hurtAny = false;

         for (LivingEntity body : world.getEntitiesOfClass(LivingEntity.class, reach, candidate -> candidate.isAlive() && !spared(caster, target, candidate))) {
            double distance = body.position().add(0.0, (double)body.getBbHeight() * 0.5, 0.0).distanceTo(var18);
            if (body == target || !(distance > 3.0)) {
               hurtAny |= Dmg.hurtThrough(body, world, blast, damage);
               if (body == target) {
                  Harm.push(caster, body, 0.0, 0.35, 0.0);
               } else {
                  double strength = 0.9 * (1.0 - 0.5 * Math.min(1.0, distance / 3.0));
                  Harm.knockback(caster, body, strength, var18.x - body.getX(), var18.z - body.getZ());
               }
            }
         }

         Fx.burst(world, ParticleTypes.EXPLOSION, var18, 3, 0.8999999999999999, 0.0);
         Fx.burst(world, ParticleTypes.POOF, var18, 24, 1.0499999999999998, 0.08);
         Fx.sound(world, var18, (SoundEvent)SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
         return hurtAny;
      }
   }

   private static boolean spared(ServerPlayer caster, LivingEntity target, LivingEntity body) {
      if (body == caster) {
         return true;
      } else {
         return body != target && body.hasIndirectPassenger(caster) ? true : !Harm.allowed(caster, body);
      }
   }

   @Override
   public void release(LivingEntity target) {
      POPPED.release(target);
   }

   @Override
   public void forget(UUID bodyId) {
      POPPED.forget(bodyId);
   }

   @Override
   public void forgetAllBodies() {
      POPPED.clear();
   }
}
